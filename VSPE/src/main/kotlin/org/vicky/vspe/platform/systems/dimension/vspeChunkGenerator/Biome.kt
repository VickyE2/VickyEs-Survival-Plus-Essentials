package org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator

import org.vicky.platform.PlatformPlugin
import org.vicky.platform.utils.ResourceLocation
import org.vicky.platform.utils.Vec3
import org.vicky.platform.world.PlatformBlockState
import org.vicky.utilities.Identifiable
import org.vicky.vspe.BiomeCategory
import org.vicky.vspe.PrecipitationType
import java.util.concurrent.*
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

// New layer type so we can mix HeightMappers and normalized noise samplers
data class NoiseLayer @JvmOverloads constructor(
    val sampler: NoiseSampler,
    val weight: Double,
    val mode: Mode = Mode.NOISE
) {
    enum class Mode { NOISE, HEIGHT }
}

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
class ChunkHeightProvider @JvmOverloads constructor(
    private val noiseLayers: List<NoiseLayer>,      // (sampler, weight, mode)
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

    // pooling for output int arrays (avoid allocations)
    private val maxPool = cacheSize.coerceAtLeast(64)
    private val samplePool = ConcurrentLinkedDeque<IntArray>()
    private fun borrowIntArray(): IntArray = samplePool.poll() ?: IntArray(chunkSize * chunkSize)
    private fun offerToPool(arr: IntArray) {
        if (samplePool.size < maxPool) {
            arr.fill(0)
            samplePool.offer(arr)
        }
    }

    // pool for coarse-grid double arrays (1D)
    private val gridPool = ConcurrentLinkedDeque<DoubleArray>()
    private fun borrowGrid(gridSize: Int): DoubleArray {
        val needed = gridSize * gridSize
        val existing = gridPool.poll()
        if (existing != null && existing.size >= needed) return existing
        return DoubleArray(needed)
    }

    private fun offerGrid(d: DoubleArray) {
        if (gridPool.size < maxPool) gridPool.offer(d)
    }

    // Simple LRU with explicit locking (keeps semantics similar to your previous version).
    private val cacheLock = Any()
    private val cache = object : LinkedHashMap<Long, IntArray>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, IntArray>): Boolean {
            val shouldRemove = size > cacheSize
            if (shouldRemove) {
                val arr = eldest.value
                arr.fill(0)
                offerToPool(arr)
            }
            return shouldRemove
        }
    }

    private val executor: ExecutorService = threadPool ?: Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
    )

    // Compose a stable key for (chunkX, chunkZ)
    private fun keyFor(chunkX: Int, chunkZ: Int): Long {
        return (chunkX.toLong() shl 32) or (chunkZ.toLong() and 0xffffffffL)
    }

    // Convert normalized noise [-1,1] -> height in same scale as HeightMapper
    private inline fun normalizedNoiseToHeight(noiseValue: Double): Double {
        val clamped = when {
            noiseValue > 1.0 -> 1.0
            noiseValue < -1.0 -> -1.0
            else -> noiseValue
        }
        return ((clamped + 1.0) * 0.5 * maxHeight)
    }

    // combine layers: convert all to height-space and weighted average
    private inline fun sampleCombinedAsHeight(x: Double, z: Double): Double {
        var sum = 0.0
        var totalWeight = 0.0
        // local copy to avoid repeated field lookups
        val layers = noiseLayers
        for (i in layers.indices) {
            val layer = layers[i]
            val s = layer.sampler.sample(x, z)
            val heightVal = if (layer.mode == NoiseLayer.Mode.HEIGHT) {
                // sampler already returns an absolute height
                s
            } else {
                // normalized noise: map into height range
                normalizedNoiseToHeight(s)
            }
            val w = layer.weight
            sum += heightVal * w
            totalWeight += w
        }
        return if (totalWeight == 0.0) 0.0 else sum / totalWeight
    }

    // Public synchronous API — returns cached array (do NOT mutate)
    fun getChunkHeights(chunkX: Int, chunkZ: Int): IntArray {
        val key = keyFor(chunkX, chunkZ)
        synchronized(cacheLock) {
            cache[key]?.let { return it } // cache hit
        }

        // generate outside lock (avoid blocking other readers)
        val arr = generateChunkHeights(chunkX, chunkZ)

        synchronized(cacheLock) {
            // another thread might have created it while we were generating
            val existing = cache[key]
            if (existing != null) {
                // discard our generated array and reuse existing
                offerToPool(arr)
                return existing
            }
            cache[key] = arr
        }
        return arr
    }

    fun getChunkHeightsCopy(chunkX: Int, chunkZ: Int): IntArray {
        val src = getChunkHeights(chunkX, chunkZ)
        return src.copyOf()
    }

    fun getChunkHeightsAsync(chunkX: Int, chunkZ: Int): Future<IntArray> {
        val key = keyFor(chunkX, chunkZ)
        synchronized(cacheLock) {
            cache[key]?.let { return CompletableFuture.completedFuture(it) }
        }
        return executor.submit<IntArray> {
            val arr = generateChunkHeights(chunkX, chunkZ)
            synchronized(cacheLock) {
                cache[key] = arr
            }
            arr
        }
    }

    private fun generateChunkHeights(chunkX: Int, chunkZ: Int): IntArray {
        val out = borrowIntArray()
        val baseX = chunkX * chunkSize
        val baseZ = chunkZ * chunkSize

        val step = chunkSize.toDouble() / lowRes.toDouble() // e.g. 16 / 4 = 4.0
        val gridSize = lowRes + 1

        // 1D grid array: index = gx + gz * gridSize
        val grid = borrowGrid(gridSize)
        val worldScaleLocal = worldScale
        val sampleCombinedLocal = ::sampleCombinedAsHeight

        // sample coarse grid (world coords * worldScale)
        var gz = 0
        while (gz < gridSize) {
            val sampleZ = (baseZ + gz * step) * worldScaleLocal
            var gx = 0
            val baseIndexRow = gz * gridSize
            while (gx < gridSize) {
                val sampleX = (baseX + gx * step) * worldScaleLocal
                grid[baseIndexRow + gx] = sampleCombinedLocal(sampleX, sampleZ)
                gx++
            }
            gz++
        }

        // interpolate to chunkSize x chunkSize
        var outIdx = 0
        for (lz in 0 until chunkSize) {
            val fy = (lz.toDouble() / chunkSize.toDouble()) * lowRes.toDouble()
            val gy = floor(fy).toInt().coerceIn(0, lowRes - 1)
            val ty = fy - gy
            val gyRow = gy * gridSize
            for (lx in 0 until chunkSize) {
                val fx = (lx.toDouble() / chunkSize.toDouble()) * lowRes.toDouble()
                val gxIndex = floor(fx).toInt().coerceIn(0, lowRes - 1)
                val tx = fx - gxIndex

                val a = grid[gyRow + gxIndex]
                val b = grid[gyRow + gxIndex + 1]
                val c = grid[(gy + 1) * gridSize + gxIndex]
                val d = grid[(gy + 1) * gridSize + gxIndex + 1]

                // bilerp on absolute heights
                val i1 = a * (1.0 - tx) + b * tx
                val i2 = c * (1.0 - tx) + d * tx
                val interpolated = i1 * (1.0 - ty) + i2 * ty

                // round and clamp to int height
                out[outIdx++] = interpolated.roundToInt().coerceIn(0, maxHeight)
            }
        }

        // return grid to pool
        offerGrid(grid)
        return out
    }

    fun clearCache() {
        synchronized(cacheLock) {
            cache.values.forEach { it.fill(0); offerToPool(it) }
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
    val foliageColor: Int = biomeColor,
    val skyColor: Int,
    val isOcean: Boolean,
    val temperature: Double,
    val humidity: Double,
    val elevation: Double,
    val rainfall: Double,
    val category: BiomeCategory,
    val isMountainous: Boolean,
    val isCold: Boolean,
    val isHumid: Boolean,
    val precipitation: PrecipitationType = PrecipitationType.RAIN,
    val distributionPalette: BiomeBlockDistributionPalette<*>,
    val heightSampler: List<NoiseLayer> = listOf(),
    val features: List<BiomeFeature<*>> = emptyList(),
    val spawnSettings: BiomeSpawnSettings = BiomeSpawnSettings(),
)

interface PlatformBiome : Identifiable {
    val name: String
    val biomeColor: Int
    val fogColor: Int
    val waterColor: Int
    val waterFogColor: Int get() = waterColor
    val foliageColor: Int get() = biomeColor
    val skyColor: Int
    val isOcean: Boolean
    val temperature: Double   // 0.0 - 1.0
    val humidity: Double      // 0.0 - 1.0
    val elevation: Double     // 0.0 - 1.0
    val rainfall: Double      // 0.0 - 1.0
    val category: BiomeCategory
    val heightSampler: List<NoiseLayer>
    val precipitation: PrecipitationType
        get() = PrecipitationType.RAIN
    val distributionPalette: BiomeBlockDistributionPalette<*>

    fun toNativeBiome(): Any
    fun isCold(): Boolean = temperature < 0.3
    fun isHumid(): Boolean = humidity > 0.7
    fun isMountainous(): Boolean = elevation > 0.6
    val features: List<BiomeFeature<*>>
    val spawnSettings: BiomeSpawnSettings
}

class SimpleConstructorBasedBiome(
    override val name: String,
    val namespace: String,
    override val biomeColor: Int,
    override val fogColor: Int,
    override val waterColor: Int,
    override val waterFogColor: Int,
    override val foliageColor: Int,
    override val skyColor: Int,
    override val isOcean: Boolean,
    override val temperature: Double,
    override val humidity: Double,
    override val elevation: Double,
    override val rainfall: Double,
    override val category: BiomeCategory,
    override val heightSampler: List<NoiseLayer>,
    override val precipitation: PrecipitationType,
    override val features: List<BiomeFeature<*>> = emptyList(),
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
interface BiomeFeature<T> {
    val id: ResourceLocation
    val placement: FeaturePlacement
    fun shouldPlace(x: Int, y: Int, z: Int, ctx: FeatureContext<T>): Boolean
    fun place(x: Int, y: Int, z: Int, ctx: FeatureContext<T>)
}

// Small context passed during generation
data class FeatureContext<T>(
    val worldSeed: Long,
    val chunkX: Int,
    val chunkZ: Int,
    val random: RandomSource,
    val blockPlacer: BlockPlacer<T>, // PlatformWorld<T,N> or null if not yet created
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

class CaveFeature<T>(
    override val id: ResourceLocation,
    val cfg: CaveFeatureConfig,
    override val placement: FeaturePlacement = FeaturePlacement.UNDERGROUND
) : BiomeFeature<T> {

    override fun shouldPlace(x: Int, y: Int, z: Int, ctx: FeatureContext<T>): Boolean {
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

    override fun place(x: Int, y: Int, z: Int, ctx: FeatureContext<T>) {
        val chunkX = x shr 4
        val chunkZ = z shr 4
        val mask = generateCaveMaskSliceWithPadding(chunkX, chunkZ, y, ctx.noiseProvider.getSampler(cfg.noiseId))
        var idx = 0
        val baseX = chunkX * 16
        val baseZ = chunkZ * 16
        for (cz in 0 until 16) {
            for (cx in 0 until 16) {
                if (mask[idx++]) {
                    val worldX = baseX + cx
                    val worldZ = baseZ + cz
                    ctx.blockPlacer.placeBlock(
                        worldX.toInt(), y.toInt(), worldZ.toInt(),
                        PlatformPlugin.stateFactory().getBlockState("minecraft:air") as PlatformBlockState<T>
                    )
                }
            }
        }
    }

}

class FloraFeature<T>(
    override val id: ResourceLocation,
    val cfg: VegetationConfig,
    override val placement: FeaturePlacement = FeaturePlacement.PER_CHUNK_RANDOM
) : BiomeFeature<T> {

    override fun shouldPlace(x: Int, y: Int, z: Int, ctx: FeatureContext<T>): Boolean {
        return y in cfg.minHeight..cfg.maxHeight
    }

    override fun place(x: Int, y: Int, z: Int, ctx: FeatureContext<T>) {

    }
}

data class MobSpawnEntry(
    val mobKey: ResourceLocation,
    val weight: Int,
    val minGroup: Int = 1,
    val maxGroup: Int = 4,
    val spawnPredicate: ((FeatureContext<*>, Int, Int, Int) -> Boolean)? = null // optional custom logic
)

data class BiomeSpawnSettings(
    val ambient: List<MobSpawnEntry> = emptyList(),
    val monsters: List<MobSpawnEntry> = emptyList(),
    val creatures: List<MobSpawnEntry> = emptyList()
)

data class RiverConfig<T : PlatformBlockState<T>>(
    val noiseId: ResourceLocation,
    val waterBlock: T,
    val depositBlock: T,
    val threshold: Double = 0.05,     // how close to noise center counts as river
    val bankFalloff: Double = 0.1,   // wideness of banks
    val riverDepth: Int = 7,         // target Y for water
    val riverSpread: Int = 12
)

class RiverFeature<T : PlatformBlockState<T>>(
    override val id: ResourceLocation,
    val cfg: RiverConfig<T>,
    override val placement: FeaturePlacement = FeaturePlacement.PER_COLUMN
) : BiomeFeature<T> {

    override fun shouldPlace(x: Int, y: Int, z: Int, ctx: FeatureContext<T>): Boolean {
        val n = ctx.noiseProvider.getSampler(cfg.noiseId).sample(x.toDouble(), z.toDouble())
        return abs(n) < cfg.bankFalloff
    }

    override fun place(x: Int, y: Int, z: Int, ctx: FeatureContext<T>) {
        val n = ctx.noiseProvider.getSampler(cfg.noiseId).sample(x.toDouble(), z.toDouble())
        val absN = abs(n)
        val closeness = 1.0 - (absN / cfg.bankFalloff).coerceIn(0.0, 1.0) // 1.0 at center
        val depth = (closeness * cfg.riverDepth).toInt().coerceAtLeast(1)

        val surfaceY = ctx.blockPlacer.getHighestBlockAt(x.toInt(), z.toInt())
        // carve valley a bit using riverSpread
        val loweredY = surfaceY - (closeness * cfg.riverSpread).toInt()

        // carve column down to bottom then fill water above, deposit at bottom
        val bottomY = loweredY - depth + 1
        for (ty in bottomY..loweredY) {
            val pos = Vec3(x.toDouble(), ty.toDouble(), z.toDouble())
            if (ty == bottomY) {
                ctx.blockPlacer.placeBlock(pos, cfg.depositBlock) // bottom/sediment
            } else {
                ctx.blockPlacer.placeBlock(pos, cfg.waterBlock)
            }
        }
    }
}

class FunctionBasedFeature<T>(
    override val id: ResourceLocation,
    override val placement: FeaturePlacement,
    private val placeCondition: (x: Int, y: Int, z: Int, ctx: FeatureContext<T>) -> Boolean,
    private val proceed: (x: Int, y: Int, z: Int, ctx: FeatureContext<T>) -> Unit
) : BiomeFeature<T> {
    override fun shouldPlace(x: Int, y: Int, z: Int, ctx: FeatureContext<T>): Boolean = placeCondition(x, y, z, ctx)
    override fun place(x: Int, y: Int, z: Int, ctx: FeatureContext<T>) = proceed(x, y, z, ctx)
}

// result of region check: either this region doesn't own a feature, or it owns with an origin point
sealed class RegionPlacement {
    object Skip : RegionPlacement()
    data class Place(val originX: Int, val originZ: Int) : RegionPlacement()
}

fun interface RegionPlacer<T> {
    fun shouldPlaceRegion(ctx: FeatureContext<T>, regionX: Int, regionZ: Int): RegionPlacement
}

/**
 * Example generic function-based feature that delegates ownership to a region placer.
 * regionSize is the edge length in chunks of each region (e.g., 4 = 4x4 chunks region).
 */
class RegionDelegatedFeature<T>(
    override val id: ResourceLocation,
    override val placement: FeaturePlacement,
    private val regionSizeChunks: Int = 4,
    private val placer: RegionPlacer<T>,
    private val proceed: (originX: Int, originZ: Int, ctx: FeatureContext<T>) -> Unit
) : BiomeFeature<T> {

    override fun shouldPlace(x: Int, y: Int, z: Int, ctx: FeatureContext<T>): Boolean {
        // region ownership is checked at chunk granularity: only the chunk that matches the region owner runs place
        val regionX = ctx.chunkX / regionSizeChunks
        val regionZ = ctx.chunkZ / regionSizeChunks
        return when (placer.shouldPlaceRegion(ctx, regionX, regionZ)) {
            is RegionPlacement.Place -> {
                val origin = (placer.shouldPlaceRegion(ctx, regionX, regionZ) as RegionPlacement.Place)
                // do not duplicate: only owner chunk (where origin lies) should run place
                val ownerChunkX = origin.originX shr 4
                val ownerChunkZ = origin.originZ shr 4
                ownerChunkX == ctx.chunkX && ownerChunkZ == ctx.chunkZ
            }

            RegionPlacement.Skip -> false
        }
    }

    override fun place(x: Int, y: Int, z: Int, ctx: FeatureContext<T>) {
        val regionX = ctx.chunkX / regionSizeChunks
        val regionZ = ctx.chunkZ / regionSizeChunks
        when (val res = placer.shouldPlaceRegion(ctx, regionX, regionZ)) {
            is RegionPlacement.Place -> proceed(res.originX, res.originZ, ctx)
            RegionPlacement.Skip -> {}
        }
    }
}
