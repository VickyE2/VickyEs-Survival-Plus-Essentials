package org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator

import com.google.errorprone.annotations.Immutable
import org.vicky.utilities.Identifiable
import org.vicky.vspe.BiomeCategory
import org.vicky.vspe.PrecipitationType
import org.vicky.platform.utils.ResourceLocation
import org.vicky.platform.utils.Vec3
import org.vicky.platform.world.PlatformWorld
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.math.floor

/**
 * ChunkHeightProvider
 *
 * - chunkSize: usually 16
 * - lowRes: number of coarse cells per axis inside chunk (4 => a 5x5 sample grid)
 * - cacheSize: how many chunk heightmaps to keep in memory
 * - worldScale: multiplier applied to world coords when sampling noise; often <=1.0
 *
 * IMPORTANT: Returned IntArray from getChunkHeights(...) is the cached array (for speed).
 * If you plan to mutate it, either copy it first or call getChunkHeightsCopy(...).
 */
class ChunkHeightProvider(
    private val noiseLayers: List<Pair<NoiseSampler, Double>>, // (sampler, weight)
    private val maxHeight: Int = 319,
    private val chunkSize: Int = 16,
    private val lowRes: Int = 4,
    private val worldScale: Double = 1.0,
    cacheSize: Int = 512,
    threadPool: ExecutorService? = null
)
{
    init {
        require(chunkSize > 0)
        require(lowRes >= 1)
    }

    private val samplePool = ConcurrentLinkedDeque<IntArray>()    // IntArray pooling
    private val cache = object : LinkedHashMap<Long, IntArray>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, IntArray>): Boolean {
            val shouldRemove = size > cacheSize
            if (shouldRemove) {
                // put removed array back into pool for reuse
                val arr = eldest.value
                arr.fill(0)
                samplePool.offer(arr)
            }
            return shouldRemove
        }
    }

    private val cacheLock = Any()
    private val executor: ExecutorService = threadPool ?: Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors().coerceAtLeast(2))

    // helper: combine noise layers with weights
    private inline fun sampleCombined(x: Double, z: Double): Double {
        var sum = 0.0
        var total = 0.0
        for ((sampler, weight) in noiseLayers) {
            val s = sampler.sample(x, z)
            sum += s * weight
            total += weight
        }
        return if (total == 0.0) 0.0 else sum / total
    }

    // default mapping from noise [-1,1] to integer height [0, maxHeight]
    private inline fun heightMapper(noiseValue: Double): Int {
        val clamped = when {
            noiseValue > 1.0 -> 1.0
            noiseValue < -1.0 -> -1.0
            else -> noiseValue
        }
        return ((clamped + 1.0) * 0.5 * maxHeight).toInt().coerceIn(0, maxHeight)
    }

    // bilinear interpolation helper
    private fun bilerp(a: Double, b: Double, c: Double, d: Double, tx: Double, ty: Double): Double {
        val i1 = a * (1.0 - tx) + b * tx
        val i2 = c * (1.0 - tx) + d * tx
        return i1 * (1.0 - ty) + i2 * ty
    }

    // Compose a stable key for (chunkX, chunkZ)
    private fun keyFor(chunkX: Int, chunkZ: Int): Long {
        return (chunkX.toLong() shl 32) or (chunkZ.toLong() and 0xffffffffL)
    }

    // borrow or allocate result array
    private fun borrowIntArray(): IntArray = samplePool.poll() ?: IntArray(chunkSize * chunkSize)

    // Public synchronous API — returns cached array (do NOT mutate)
    fun getChunkHeights(chunkX: Int, chunkZ: Int): IntArray {
        val key = keyFor(chunkX, chunkZ)
        synchronized(cacheLock) {
            cache[key]?.let { return it } // cache hit
        }

        // generate and insert
        val arr = generateChunkHeights(chunkX, chunkZ)
        synchronized(cacheLock) {
            cache[key] = arr
        }
        return arr
    }

    // If you need to mutate result, call this to get a copy
    fun getChunkHeightsCopy(chunkX: Int, chunkZ: Int): IntArray {
        val src = getChunkHeights(chunkX, chunkZ)
        return src.copyOf()
    }

    // Async API using executor returning Future<IntArray>
    fun getChunkHeightsAsync(chunkX: Int, chunkZ: Int): Future<IntArray> {
        val key = keyFor(chunkX, chunkZ)
        synchronized(cacheLock) {
            cache[key]?.let { return CompletableFutureDone(it) }
        }
        return executor.submit(Callable {
            val arr = generateChunkHeights(chunkX, chunkZ)
            synchronized(cacheLock) {
                cache[key] = arr
            }
            arr
        })
    }

    // Simple finished future impl for immediate cache hits (avoids creating new thread)
    private class CompletableFutureDone<T>(private val value: T) : Future<T> {
        override fun cancel(mayInterruptIfRunning: Boolean) = false
        override fun isCancelled() = false
        override fun isDone() = true
        override fun get(): T = value
        override fun get(timeout: Long, unit: java.util.concurrent.TimeUnit?): T = value
    }

    // Core generator (highly optimized): coarse-grid sampling + bilerp
    private fun generateChunkHeights(chunkX: Int, chunkZ: Int): IntArray {
        val out = borrowIntArray()
        val baseX = chunkX * chunkSize
        val baseZ = chunkZ * chunkSize

        val step = chunkSize.toDouble() / lowRes.toDouble() // e.g. 16 / 4 = 4.0
        val gridSize = lowRes + 1
        // allocate grid double arrays once per call (small; reuse by stack)
        val grid = Array(gridSize) { DoubleArray(gridSize) }

        // sample coarse grid (world coords * worldScale)
        for (gz in 0 until gridSize) {
            val sampleZ = (baseZ + gz * step) * worldScale
            for (gx in 0 until gridSize) {
                val sampleX = (baseX + gx * step) * worldScale
                grid[gx][gz] = sampleCombined(sampleX, sampleZ)
            }
        }

        // interpolate to chunkSize x chunkSize
        var outIdx = 0
        for (lz in 0 until chunkSize) {
            val fy = (lz.toDouble() / chunkSize.toDouble()) * lowRes.toDouble() // position in coarse coords
            val gy = floor(fy).toInt().coerceIn(0, lowRes - 1)
            val ty = fy - gy
            for (lx in 0 until chunkSize) {
                val fx = (lx.toDouble() / chunkSize.toDouble()) * lowRes.toDouble()
                val gx = floor(fx).toInt().coerceIn(0, lowRes - 1)
                val tx = fx - gx

                val a = grid[gx][gy]
                val b = grid[gx + 1][gy]
                val c = grid[gx][gy + 1]
                val d = grid[gx + 1][gy + 1]

                val interpolated = bilerp(a, b, c, d, tx, ty)
                out[outIdx++] = heightMapper(interpolated)
            }
        }
        return out
    }

    // optional maintenance helpers
    fun clearCache() {
        synchronized(cacheLock) {
            cache.values.forEach { it.fill(0); samplePool.offer(it) }
            cache.clear()
        }
    }

    fun shutdownExecutor() {
        executor.shutdown()
    }
}

data class BiomeParameters @JvmOverloads constructor(
    val id: String,
    val name: String,
    val biomeColor: Int,
    val fogColor: Int,
    val waterColor: Int,
    val waterFogColor: Int = waterColor,
    val isOcean: Boolean,
    val temperature: Double,
    val humidity: Double,
    val elevation: Double,
    val rainfall: Double,
    val category: BiomeCategory,
    val precipitation: PrecipitationType = PrecipitationType.RAIN,
    val distributionPalette: BiomeBlockDistributionPalette<*>,
    val heightSampler: CompositeNoiseLayer = CompositeNoiseLayer.EMPTY,
    val features: List<BiomeFeature<*, *>> = emptyList(),
    val spawnSettings: BiomeSpawnSettings = BiomeSpawnSettings(),
    val biomeStructureData: BiomeStructureData = BiomeStructureData.EMPTY
)

interface PlatformBiome : Identifiable {
    val name: String
    val biomeColor: Int
    val fogColor: Int
    val waterColor: Int
    val waterFogColor: Int get() = waterColor
    val isOcean: Boolean
    val temperature: Double   // 0.0 - 1.0
    val humidity: Double      // 0.0 - 1.0
    val elevation: Double     // 0.0 - 1.0
    val rainfall: Double      // 0.0 - 1.0
    val category: BiomeCategory
    val heightSampler: CompositeNoiseLayer
    val precipitation: PrecipitationType
        get() = PrecipitationType.RAIN
    val biomeStructureData: BiomeStructureData
    val distributionPalette: BiomeBlockDistributionPalette<*>

    fun toNativeBiome(): Any
    fun isCold(): Boolean = temperature < 0.3
    fun isHumid(): Boolean = humidity > 0.7
    fun isMountainous(): Boolean = elevation > 0.6
    val features: List<BiomeFeature<*, *>>
    val spawnSettings: BiomeSpawnSettings
    // val decorators: List<Decorator>                       // e.g., snow, leaves, post-process
    // val ambientSettings: BiomeAmbientSettings?
}

data class StructurePlacement(
    val structureKey: ResourceLocation,
    val frequencyPerChunk: Double = 0.01, // probability to attempt per chunk
    val separation: Int = 32, // spacing/separation rules
    val allowedBiomes: Set<ResourceLocation> = emptySet()
)

open class BiomeStructureData(
    val biomeId: ResourceLocation,
    val structureKeys: List<StructurePlacement>
) {
    object EMPTY : BiomeStructureData(ResourceLocation.getEMPTY(), listOf());
}

class SimpleConstructorBasedBiome(
    override val name: String,
    val namespace: String,
    override val biomeColor: Int,
    override val fogColor: Int,
    override val waterColor: Int,
    override val waterFogColor: Int,
    override val isOcean: Boolean,
    override val temperature: Double,
    override val humidity: Double,
    override val elevation: Double,
    override val rainfall: Double,
    override val category: BiomeCategory,
    override val heightSampler: CompositeNoiseLayer,
    override val precipitation: PrecipitationType,
    override val biomeStructureData: BiomeStructureData,
    override val features: List<BiomeFeature<*, *>> = emptyList(),
    override val spawnSettings: BiomeSpawnSettings = BiomeSpawnSettings(),
    override val distributionPalette: BiomeBlockDistributionPalette<*> = BiomeBlockDistributionPalette.empty()
):
    PlatformBiome {
    override fun toNativeBiome(): String {
        return "$namespace:${name.trim().lowercase()}"
    }
    override fun getIdentifier(): String? {
        return "$namespace:${name.trim().lowercase()}"
    }
}

// Marker for anything that can be placed during generation
interface BiomeFeature<T, N> {
    val id: ResourceLocation
    val placement: FeaturePlacement
    fun shouldPlace(x: Int, y: Int, z: Int, ctx: FeatureContext<T, N>): Boolean
    fun place(x: Int, y: Int, z: Int, ctx: FeatureContext<T, N>)
}

// Small context passed during generation
data class FeatureContext<T, N>(
    val worldSeed: Long,
    val chunkX: Int,
    val chunkZ: Int,
    val random: RandomSource,
    val platformWorld: PlatformWorld<T, N>, // PlatformWorld<T,N> or null if not yet created
    val noiseProvider: NoiseSamplerProvider // helper to get noise samplers
)

interface NoiseSamplerProvider {
    fun getSampler(id: ResourceLocation): NoiseSampler
}

// How/when the generator attempts placements
enum class FeaturePlacement {
    PER_COLUMN,       // evaluate once per column
    PER_CHUNK_RANDOM, // attempt N attempts per chunk at random positions
    GRID,             // regular grid (good for patches)
    UNDERGROUND       // underground specific placement
}

data class CaveFeatureConfig(
    val noiseId: ResourceLocation, // id of noise sampler to use
    val threshold: Double,         // values below = air / carve
    val frequencyScale: Double = 1.0,
    val verticalFreqScale: Double = 1.0,
    val verticalRange: IntRange = 8..120,
    val carveRadius: Double = 1.0, // optional widening factor
    val tunnelConnectedness: Double = 0.5, // domain warp strength or connectivity
    val domainWarpPadding: Int = 0
)

data class VegetationConfig(
    val paletteId: ResourceLocation,   // which tree/pattern template to use
    val densityNoiseId: ResourceLocation?, // optional noise for density
    val density: Double = 0.5,         // fallback probability per attempt
    val attemptsPerChunk: Int = 8,
    val minHeight: Int = 62,
    val maxHeight: Int = 120,
    val allowedSurfaceBlocks: Set<ResourceLocation> = setOf(ResourceLocation.from("minecraft", "grass_block"))
)

class CaveFeature<T, N>(
    override val id: ResourceLocation,
    val cfg: CaveFeatureConfig,
    override val placement: FeaturePlacement = FeaturePlacement.UNDERGROUND
) : BiomeFeature<T, N> {

    override fun shouldPlace(x: Int, y: Int, z: Int, ctx: FeatureContext<T, N>): Boolean {
        if (y !in cfg.verticalRange) return false
        val sample = ctx.noiseProvider.getSampler(cfg.noiseId).sample(x * cfg.frequencyScale, z * cfg.frequencyScale)
        return sample < cfg.threshold
    }

    /**
     * Generates a boolean carve mask for the chunk's columns at a specific Y slice.
     * Returns a BooleanArray length = chunkSize*chunkSize where index = z*chunkSize + x
     */
    fun generateCaveMaskSliceWithPadding(
        chunkX: Int,
        chunkZ: Int,
        y: Int,
        sampler: NoiseSampler,
        chunkSize: Int = 16
    ): BooleanArray {
        val maxRadius = cfg.carveRadius.ceilToInt() + cfg.domainWarpPadding // define domain warp padding
        val pad = maxRadius.coerceAtLeast(1)
        val baseX = chunkX * chunkSize
        val baseZ = chunkZ * chunkSize

        val minX = baseX - pad
        val maxX = baseX + chunkSize - 1 + pad
        val minZ = baseZ - pad
        val maxZ = baseZ + chunkSize - 1 + pad

        val width = maxX - minX + 1
        val depth = maxZ - minZ + 1

        // Optionally use low-res sampling and bilerp here for speed. This example uses direct sampling.
        val maskExtended = Array(depth) { BooleanArray(width) }

        val freq = cfg.frequencyScale
        val threshold = cfg.threshold

        for (dz in 0 until depth) {
            val worldZ = minZ + dz
            for (dx in 0 until width) {
                val worldX = minX + dx
                val value = sampler.sample3D(worldX.toDouble() * freq, y.toDouble() * cfg.verticalFreqScale, worldZ.toDouble() * freq)
                maskExtended[dz][dx] = value < threshold
            }
        }

        // extract center area (the actual chunk)
        val result = BooleanArray(chunkSize * chunkSize)
        var idx = 0
        val startX = pad
        val startZ = pad
        for (z in 0 until chunkSize) {
            for (x in 0 until chunkSize) {
                result[idx++] = maskExtended[startZ + z][startX + x]
            }
        }

        return result
    }


    // small helper
    private fun Double.ceilToInt(): Int = kotlin.math.ceil(this).toInt()

    override fun place(x: Int, y: Int, z: Int, ctx: FeatureContext<T, N>) {
        val booleanArray = generateCaveMaskSliceWithPadding(x shr 4, z shr 4, y, ctx.noiseProvider.getSampler(id))
        for (bool in booleanArray) {
            if (bool) {
                ctx.platformWorld.setPlatformBlockState(Vec3(x * 1.0, y * 1.0, z * 1.0), ctx.platformWorld.AIR())
            }
        }
    }
}

class FloraFeature<T, N>(
    override val id: ResourceLocation,
    val cfg: VegetationConfig,
    override val placement: FeaturePlacement = FeaturePlacement.PER_CHUNK_RANDOM
) : BiomeFeature<T, N> {

    override fun shouldPlace(x: Int, y: Int, z: Int, ctx: FeatureContext<T, N>): Boolean {
        return y in cfg.minHeight..cfg.maxHeight
    }

    override fun place(x: Int, y: Int, z: Int, ctx: FeatureContext<T, N>) {
        // use palette/template to place entire tree or patch
        // be conservative with block updates
    }
}

data class MobSpawnEntry(
    val mobKey: ResourceLocation,
    val weight: Int,
    val minGroup: Int = 1,
    val maxGroup: Int = 4,
    val spawnPredicate: ((FeatureContext<*, *>, Int, Int, Int) -> Boolean)? = null // optional custom logic
)

data class BiomeSpawnSettings(
    val ambient: List<MobSpawnEntry> = emptyList(),
    val monsters: List<MobSpawnEntry> = emptyList(),
    val creatures: List<MobSpawnEntry> = emptyList()
)