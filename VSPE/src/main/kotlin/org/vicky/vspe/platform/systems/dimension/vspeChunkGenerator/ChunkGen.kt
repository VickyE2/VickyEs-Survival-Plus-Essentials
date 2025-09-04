package org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator

import de.articdive.jnoise.interpolation.Interpolation
import de.pauleff.api.ICompoundTag
import org.vicky.platform.utils.Vec3
import org.vicky.platform.world.PlatformBlockState
import org.vicky.platform.world.PlatformWorld
import org.vicky.vspe.BiomeCategory
import org.vicky.vspe.platform.VSPEPlatformPlugin
import org.vicky.vspe.platform.systems.dimension.imagetester.seedBase
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.random.Random

interface PlatformChunkGenerator<T, B: PlatformBiome> {
    fun generateChunk(context: ChunkGenerateContext<T, B>): ChunkData<T, B>
    fun getBiome(x: Int, y: Int, z: Int): B
}

interface PlatformDimension<T, B: PlatformBiome> {
    val id: String
    val world: PlatformWorld<T, *>
    val chunkGenerator: PlatformChunkGenerator<T, B>
    val biomeResolver: BiomeResolver<B>
    val structurePlacer: StructurePlacer<T>
    val random: RandomSource
}

open class ChunkGenerateContext<T, B: PlatformBiome>(
    val chunkX: Int,
    val chunkZ: Int,
    val biomeResolver: BiomeResolver<B>,
    val random: RandomSource,
    val blockPlacer: BlockPlacer<T>,
    val locationProvider: (x: Int, y: Int, z: Int) -> Vec3
)

interface BiomeResolver<B: PlatformBiome>{
    fun getBiomePalette(): Palette<B>
    fun resolveBiome(x: Int, y: Int, z: Int, seed: Long): B
    fun foggyAt(x: Int, y: Int): Boolean = false
}

interface ChunkData<T, B: PlatformBiome> {
    fun setBlock(x: Int, y: Int, z: Int, block: PlatformBlockState<T>)
    fun setBiome(x: Int, y: Int, z: Int, biome: B)
}

interface BlockPlacer<T> {
    fun placeBlock(x: Int, y: Int, z: Int, data: PlatformBlockState<T>?)
    fun placeBlock(x: Int, y: Int, z: Int, data: PlatformBlockState<T>?, nbt: ICompoundTag)
    fun placeBlock(vec: Vec3, data: PlatformBlockState<T>?)
    fun placeBlock(vec: Vec3, data: PlatformBlockState<T>?, nbt: ICompoundTag)
    fun getHighestBlockAt(x: Int, z: Int): Int
}

class EvenSpreadBiomeResolver<B: PlatformBiome>(
    private val entries: List<Entry<B>>,
    private val noise: NoiseSampler,
    private val biomeSize: Double
) : BiomeResolver<B>
{

    override fun resolveBiome(x: Int, y: Int, z: Int, seed: Long): B {
        val nx = x / biomeSize
        val nz = z / biomeSize
        val value = (noise.sample(nx, nz) + 1.0) / 2.0

        // normalize weights
        val total = entries.sumOf { it.weight }
        if (total <= 0.0) return entries.last().biome

        var cum = 0.0
        for (e in entries) {
            cum += e.weight / total
            if (value <= cum) return e.biome
        }
        return entries.last().biome
    }

    override fun getBiomePalette(): Palette<B> = PaletteBuilder.EMPTY<B>().build()

    class Entry<B>(val biome: B, val weight: Double)
}

class NoiseBiomeResolver<B: PlatformBiome>(
    private val palette: Palette<B>,
    private val noise: NoiseSampler
) : BiomeResolver<B>
{
    override fun getBiomePalette(): Palette<B> {
        return palette
    }

    override fun resolveBiome(x: Int, y: Int, z: Int, seed: Long): B {
        val nx = x / 512.0
        val nz = z / 512.0
        val value = noise.sample(nx, nz)
        return getBiomePalette().get(value, null)
    }
}

/**
 * Optimized MultiParameterBiomeResolver:
 * - caches palette entries grouped by BiomeCategory
 * - avoids per-call allocation and heavy math
 */
class MultiParameterBiomeResolver<T : PlatformBiome> @JvmOverloads constructor(
    val tempNoise: NoiseSampler,
    val humidNoise: NoiseSampler,
    val elevNoise: NoiseSampler,
    private val rainNoise: NoiseSampler? = null,
    internal var palette: Palette<T>,
    private val continentNoise: NoiseSampler? = null,
    val seaLevel: Double = 0.30,
    val deepSeaLevel: Double = 0.2,
    internal var isMerged: Boolean = false
) : BiomeResolver<T> {

    private val replacementMap: Map<BiomeCategory, BiomeCategory> = mapOf(
        BiomeCategory.WARM_DEEP_OCEAN to BiomeCategory.DEEP_OCEAN,
        BiomeCategory.COLD_DEEP_OCEAN to BiomeCategory.DEEP_OCEAN,
        BiomeCategory.DEEP_OCEAN to BiomeCategory.OCEAN,
        BiomeCategory.WARM_OCEAN to BiomeCategory.OCEAN,
        BiomeCategory.COLD_OCEAN to BiomeCategory.OCEAN,
        BiomeCategory.OCEAN to BiomeCategory.COAST,
        BiomeCategory.COAST to BiomeCategory.PLAINS,
        BiomeCategory.SNOWY_BEACH to BiomeCategory.ICY,
        BiomeCategory.ICY to BiomeCategory.TUNDRA,
        BiomeCategory.TUNDRA to BiomeCategory.TAIGA,
        BiomeCategory.TAIGA to BiomeCategory.FOREST,
        BiomeCategory.FOREST to BiomeCategory.PLAINS,
        BiomeCategory.JUNGLE to BiomeCategory.RAINFOREST,
        BiomeCategory.RAINFOREST to BiomeCategory.FOREST,
        BiomeCategory.WETLAND to BiomeCategory.SWAMP,
        BiomeCategory.SWAMP to BiomeCategory.RIVER,
        BiomeCategory.RIVER to BiomeCategory.PLAINS,
        BiomeCategory.LUSH_CAVES to BiomeCategory.FOREST,
        BiomeCategory.MOUNTAIN to BiomeCategory.PLAINS,
        BiomeCategory.SAVANNA to BiomeCategory.PLAINS,
        BiomeCategory.MESA to BiomeCategory.DESERT,
        BiomeCategory.DESERT to BiomeCategory.DESERT,
        BiomeCategory.NETHER to BiomeCategory.DESERT,
        BiomeCategory.END to BiomeCategory.PLAINS,
        BiomeCategory.PLAINS to BiomeCategory.PLAINS
    )
    private data class BiomeEntry<T : PlatformBiome>(
        val biome: T,
        val temp: Double,
        val humid: Double,
        val elev: Double,
        val rain: Double,
        val category: BiomeCategory
    )

    private var entriesByCategory: Array<Array<BiomeEntry<T>>> = Array(BiomeCategory.entries.size) { emptyArray() }
    private var allEntriesCached: Array<BiomeEntry<T>> = emptyArray()

    private val continentScale = 0.0015
    private val continentThreshold = 0.45
    private val coastBand = 18
    private val coastChanceInOpenOcean = 0.02
    private val deepSeaDistance = 80
    private val landElevScale = 0.001
    private val oceanElevScale = 0.006
    private val mountainElevCut = 0.82
    private val distanceSampleMax = 128
    private val coarseCellSize = 16
    private val maxSearchRadiusChunks = (distanceSampleMax / coarseCellSize).coerceAtLeast(8)
    private val MASK_BUCKETS = 256
    private var maskBuckets: Array<Array<BiomeEntry<T>>> = Array(MASK_BUCKETS) { emptyArray() }
    private val chunkRngCache = ConcurrentHashMap<Long, java.util.SplittableRandom>()
    init {
        rebuildCaches()
    }

    fun setPalette(newPalette: Palette<T>) {
        palette = newPalette
        rebuildCaches()
    }

    override fun getBiomePalette(): Palette<T> = palette
    private fun rebuildCaches() {
        val byCategory = Array(BiomeCategory.entries.size) { mutableListOf<BiomeEntry<T>>() }
        val values = mutableListOf<BiomeEntry<T>>()
        val biomeToEntry = HashMap<Any, BiomeEntry<T>>() // key: Palette key/biome id depending on palette

        // Build BiomeEntry for each biome value and store mapping from biome object -> entry
        for (biome in palette.map.values) {
            val entry = BiomeEntry(
                biome,
                biome.temperature.toDouble(),
                biome.humidity.toDouble(),
                biome.elevation.toDouble(),
                biome.rainfall.toDouble(),
                biome.category
            )
            byCategory[biome.category.ordinal].add(entry)
            values.add(entry)
            biomeToEntry[biome] = entry
        }
        entriesByCategory = byCategory.map { it.toTypedArray() }.toTypedArray()
        allEntriesCached = values.toTypedArray()

        // --- Build mask buckets from palette keys (interpret key as Pair<Double,Double>) ---
        val tmpBuckets = Array(MASK_BUCKETS) { mutableListOf<BiomeEntry<T>>() }

        // iterate palette entries so we can read each key range
        for ((key, biomeVal) in palette.map) {
            // attempt to extract interval (lo, hi) from key
            val range = when (key) {
                else -> {
                    val lo = (key.first as? Number)?.toDouble() ?: continue
                    val hi = (key.second as? Number)?.toDouble() ?: continue
                    lo.coerceAtMost(hi) to hi.coerceAtLeast(lo) // Pair(lo,hi)
                }
            }
            val (lo, hi) = range
            // find bucket indices that the range covers
            val startIdx = ((lo.coerceIn(0.0, 1.0) * (MASK_BUCKETS - 1))).toInt()
            val endIdx = ((hi.coerceIn(0.0, 1.0) * (MASK_BUCKETS - 1))).toInt()
            val entry = biomeToEntry[biomeVal] ?: continue
            for (bi in startIdx..endIdx) {
                tmpBuckets[bi].add(entry)
            }
        }

        // remove duplicates and store as arrays for fast read
        for (i in tmpBuckets.indices) {
            // dedupe by identity of BiomeEntry (small lists)
            val dedup = tmpBuckets[i].distinctBy { it.biome } // or use Set if you prefer
            maskBuckets[i] = dedup.toTypedArray()
        }
    }

    private fun rngForChunk(cx: Int, cz: Int, seed: Long): java.util.SplittableRandom {
        val key = chunkKey(cx, cz)
        return chunkRngCache.computeIfAbsent(key) {
            java.util.SplittableRandom(seed xor (cx.toLong() * 0x9E3779B97F4AL xor cz.toLong()))
        }
    }

    fun continentMask(x: Int, z: Int): Double {
        val noise = continentNoise ?: elevNoise
        return ((noise.sample(x * continentScale, z * continentScale) + 1.0) * 0.5).coerceIn(0.0, 1.0)
    }

    data class ChunkMeta(
        val mask: Double,
        val isLand: Boolean,
        val elevCenter: Double,
        val tempCenter: Double,
        val humidCenter: Double,
        val rainCenter: Double
    )

    private val chunkMetaCache = ConcurrentHashMap<Long, ChunkMeta>()
    private val chunkDistChunksCache = ConcurrentHashMap<Long, Int>()
    private fun chunkKey(cx: Int, cz: Int): Long = (cx.toLong() shl 32) or (cz.toLong() and 0xffffffffL)
    private fun computeChunkDistanceForChunk(cxOrigin: Int, czOrigin: Int): Int {
        val originKey = chunkKey(cxOrigin, czOrigin)

        // quick check: if origin chunk is land
        if (getChunkMeta(cxOrigin, czOrigin).isLand) return 0

        // BFS on chunk grid
        val maxRadius = maxSearchRadiusChunks
        val visited = HashSet<Long>()
        val q = ArrayDeque<Pair<Int, Int>>()
        q.add(cxOrigin to czOrigin)
        visited.add(originKey)
        var radius = 0

        while (q.isNotEmpty() && radius <= maxRadius) {
            val levelSize = q.size
            for (i in 0 until levelSize) {
                val (cx, cz) = q.removeFirst()
                // if this chunk is land, compute center-distance (in chunks) to origin
                if (getChunkMeta(cx, cz).isLand) {
                    val dx = cx - cxOrigin
                    val dz = cz - czOrigin
                    val distChunks = kotlin.math.sqrt((dx * dx + dz * dz).toDouble()).toInt()
                    return distChunks.coerceAtMost(maxRadius)
                }
                // push neighbors
                val neighbors = arrayOf(cx + 1 to cz, cx - 1 to cz, cx to cz + 1, cx to cz - 1)
                for ((ncx, ncz) in neighbors) {
                    val nKey = chunkKey(ncx, ncz)
                    if (visited.add(nKey)) q.add(ncx to ncz)
                }
            }
            radius++
        }

        // nothing found within radius -> treat as "very far"
        return maxRadius + 1
    }

    private fun computeChunkMetaPure(cx: Int, cz: Int): ChunkMeta {
        val centerX = cx * coarseCellSize + coarseCellSize / 2
        val centerZ = cz * coarseCellSize + coarseCellSize / 2

        val noiseForContinent = continentNoise ?: elevNoise
        val mask =
            ((noiseForContinent.sample(centerX * continentScale, centerZ * continentScale) + 1.0) * 0.5).coerceIn(
                0.0,
                1.0
            )
        val isLand = mask >= continentThreshold

        // sample elevation with appropriate scale for land/ocean
        val elevScale = if (isLand) landElevScale else oceanElevScale
        val elev = ((elevNoise.sample(centerX * elevScale, centerZ * elevScale) + 1.0) * 0.5).coerceIn(0.0, 1.0)

        val temp = ((tempNoise.sample(centerX * 0.005, centerZ * 0.005) + 1.0) * 0.5).coerceIn(0.0, 1.0)
        val humid = ((humidNoise.sample(centerX * 0.01, centerZ * 0.01) + 1.0) * 0.5).coerceIn(0.0, 1.0)
        val rain = ((rainNoise?.sample(centerX * 0.01, centerZ * 0.01) ?: 0.0) + 1.0) * 0.5

        return ChunkMeta(mask, isLand, elev, temp, humid, rain)
    }

    /**
     * Get chunk meta. This uses computeIfAbsent with a *pure* mapping function, so
     * no nested computeIfAbsent or map mutation occurs inside the mapping.
     */
    private fun getChunkMeta(cx: Int, cz: Int): ChunkMeta {
        val key = chunkKey(cx, cz)
        // computeIfAbsent is safe because computeChunkMetaPure does not touch other
        // concurrent maps nor have side-effects besides returning the meta.
        return chunkMetaCache.computeIfAbsent(key) { computeChunkMetaPure(cx, cz) }
    }

    // Optional manual warm/caching helper for an area (call before heavy generation)
    fun precomputeChunkRegion(cx0: Int, cz0: Int, cx1: Int, cz1: Int) {
        for (cz in cz0..cz1) {
            for (cx in cx0..cx1) {
                val key = chunkKey(cx, cz)
                if (!chunkMetaCache.containsKey(key)) {
                    // compute & put (computeIfAbsent avoided to keep explicit)
                    val meta = computeChunkMetaPure(cx, cz)
                    chunkMetaCache.putIfAbsent(key, meta)
                }
            }
        }
    }
    override fun resolveBiome(x: Int, y: Int, z: Int, seed: Long): T {
        val cx = x shr 4
        val cz = z shr 4
        val key = chunkKey(cx, cz)
        val localRand = rngForChunk(cx, cz, seed).nextDouble()
        val meta = chunkMetaCache.computeIfAbsent(key) { computeChunkMetaPure(cx, cz) }

        // read precomputed stuff
        val isLand = meta.isLand
        val distChunks = chunkDistChunksCache.computeIfAbsent(key) { computeChunkDistanceForChunk(cx, cz) }
        val distBlocksApprox = distChunks * coarseCellSize
        val openOcean = !isLand && distBlocksApprox > deepSeaDistance

        // fast bilinear interpolation of climate/elev using chunk centres
        val elev = bilinearSampleElevation(x, z)
        val t = bilinearSampleTemp(x, z)
        val h = bilinearSampleHumid(x, z)
        val r = bilinearSampleRain(x, z)

        // choose category using cheap checks (same as before)
        val chosenCategoryOrdinal = when {
            openOcean -> {
                when {
                    t > 0.6 -> BiomeCategory.WARM_DEEP_OCEAN.ordinal
                    t > 0.4 -> BiomeCategory.DEEP_OCEAN.ordinal
                    else -> BiomeCategory.COLD_DEEP_OCEAN.ordinal
                }
            }

            !isLand -> {
                val isCoastal = distBlocksApprox <= coastBand
                when {
                    isCoastal && localRand < coastChanceInOpenOcean && distBlocksApprox > deepSeaDistance / 2 ->
                        BiomeCategory.COAST.ordinal

                    isCoastal && t < 0.3 && h < 0.4 ->
                        BiomeCategory.SWAMP.ordinal

                    isCoastal && t < 0.2 ->
                        BiomeCategory.SNOWY_BEACH.ordinal

                    else -> {
                        when {
                            t > 0.6 -> BiomeCategory.WARM_OCEAN.ordinal
                            t > 0.4 -> BiomeCategory.OCEAN.ordinal
                            else -> BiomeCategory.COLD_OCEAN.ordinal
                        }
                    }
                }
            }

            else -> { // Land
                val isCoastal = distBlocksApprox <= coastBand
                if (isCoastal) {
                    when {
                        elev < 0.25 -> BiomeCategory.COAST.ordinal
                        elev > 0.78 -> BiomeCategory.MOUNTAIN.ordinal
                        else -> BiomeCategory.PLAINS.ordinal
                    }
                } else {
                    when {
                        elev > mountainElevCut -> BiomeCategory.MOUNTAIN.ordinal
                        t < 0.2 -> if (h < 0.35) BiomeCategory.ICY.ordinal else BiomeCategory.TUNDRA.ordinal
                        t < 0.35 -> if (h < 0.3) BiomeCategory.DESERT.ordinal else BiomeCategory.TAIGA.ordinal
                        t < 0.55 -> if (h < 0.3) BiomeCategory.MESA.ordinal else BiomeCategory.FOREST.ordinal
                        t < 0.7 -> if (h < 0.25) BiomeCategory.SAVANNA.ordinal else BiomeCategory.JUNGLE.ordinal
                        else -> if (h < 0.45) BiomeCategory.WETLAND.ordinal else BiomeCategory.RAINFOREST.ordinal
                    }
                }
            }


            /* TODO: Y-level:
                Example: underground lush caves
                y < 40 && noise.isCaveLike -> BiomeCategory.LUSH_CAVES.ordinal
                if (dimension == NETHER) BiomeCategory.NETHER.ordinal
                if (dimension == END) BiomeCategory.END.ordinal */
        }

        // candidate selection: use squared-distance mapping (no sqrt)
        val maskValue = meta.mask // or continentMask(x,z)
        val bucketIdx = ((maskValue.coerceIn(0.0, 1.0) * (MASK_BUCKETS - 1))).toInt()
        val bucketCandidates = maskBuckets[bucketIdx]
        val candidates = if (bucketCandidates.isNotEmpty()) {
            bucketCandidates.filter { it.category.ordinal == chosenCategoryOrdinal }.toTypedArray()
        } else {
            findCandidatesWithFallbackFast(intArrayOf(chosenCategoryOrdinal))
        }
        if (candidates.isEmpty()) return allEntriesCached.firstOrNull()?.biome ?: palette.map.values.first()

        // compute weights without sqrt:
        // weight = max(0, 1 - (sqDist / S)), pick S = 4.0 (tune)
        val S = 4.0  // max allowed squared distance; tune to compress/expand attraction radius
        var totalWeight = 0.0
        // use local array to avoid allocations
        val localWeights = DoubleArray(candidates.size)
        for (i in candidates.indices) {
            val ent = candidates[i]
            val dt = ent.temp - t
            val dh = ent.humid - h
            val de = ent.elev - elev
            val dr = ent.rain - r
            val sq = dt * dt + dh * dh + de * de + dr * dr
            val w = ((1.0 - (sq / S))).coerceAtLeast(0.0)
            localWeights[i] = w
            totalWeight += w
        }

        val rng = rngForChunk(cx, cz, seed)
        val pickIdx = if (totalWeight <= 0.0) {
            (rng.nextDouble() * candidates.size).toInt().coerceAtMost(candidates.size - 1)
        } else {
            var pw = rng.nextDouble() * totalWeight
            var picked = -1
            for (i in candidates.indices) {
                pw -= localWeights[i]
                if (pw <= 0.0) {
                    picked = i; break
                }
            }
            if (picked < 0) candidates.lastIndex else picked
        }

        return candidates[pickIdx].biome
    }
    private fun findCandidatesWithFallbackFast(initial: IntArray): Array<BiomeEntry<T>> {
        if (initial.isEmpty()) return emptyArray()
        val seen = BooleanArray(entriesByCategory.size)
        val work = IntArray(entriesByCategory.size)
        var workLen = 0

        for (o in initial) {
            if (o in entriesByCategory.indices) work[workLen++] = o
        }

        while (workLen > 0) {
            for (i in 0 until workLen) {
                val arr = entriesByCategory.getOrNull(work[i])
                if (arr != null && arr.isNotEmpty()) return arr
            }

            var nextLen = 0
            for (i in 0 until workLen) {
                val ord = work[i]
                if (ord in entriesByCategory.indices && !seen[ord]) {
                    seen[ord] = true
                    val nextOrd = replacementMap[BiomeCategory.entries[ord]]?.ordinal ?: continue
                    if (!seen[nextOrd]) {
                        work[nextLen++] = nextOrd
                    }
                }
            }
            workLen = nextLen
        }
        return emptyArray()
    }

    private fun lerp(a: Double, b: Double, t: Double): Double = a + (b - a) * t
    private fun bilinear(q00: Double, q10: Double, q01: Double, q11: Double, tx: Double, tz: Double): Double {
        val x0 = lerp(q00, q10, tx)
        val x1 = lerp(q01, q11, tx)
        return lerp(x0, x1, tz)
    }

    /**
     * Sample elevation at arbitrary block coords using bilinear interpolation of chunk centers.
     * Falls back to a direct noise sample if a neighbor chunk meta is missing (shouldn't happen if you precompute).
     */
    private fun bilinearSampleElevation(x: Int, z: Int): Double {
        val cx = x shr 4
        val cz = z shr 4
        val tx = ((x - (cx shl 4)).toDouble()) / 16.0
        val tz = ((z - (cz shl 4)).toDouble()) / 16.0

        // neighbor coords
        val m00 = chunkMetaCache[chunkKey(cx, cz)] ?: getChunkMeta(cx, cz)
        val m10 = chunkMetaCache[chunkKey(cx + 1, cz)] ?: getChunkMeta(cx + 1, cz)
        val m01 = chunkMetaCache[chunkKey(cx, cz + 1)] ?: getChunkMeta(cx, cz + 1)
        val m11 = chunkMetaCache[chunkKey(cx + 1, cz + 1)] ?: getChunkMeta(cx + 1, cz + 1)

        return bilinear(m00.elevCenter, m10.elevCenter, m01.elevCenter, m11.elevCenter, tx, tz)
    }

    private fun bilinearSampleTemp(x: Int, z: Int): Double {
        val cx = x shr 4
        val cz = z shr 4
        val tx = ((x - (cx shl 4)).toDouble()) / 16.0
        val tz = ((z - (cz shl 4)).toDouble()) / 16.0

        val m00 = chunkMetaCache[chunkKey(cx, cz)] ?: getChunkMeta(cx, cz)
        val m10 = chunkMetaCache[chunkKey(cx + 1, cz)] ?: getChunkMeta(cx + 1, cz)
        val m01 = chunkMetaCache[chunkKey(cx, cz + 1)] ?: getChunkMeta(cx, cz + 1)
        val m11 = chunkMetaCache[chunkKey(cx + 1, cz + 1)] ?: getChunkMeta(cx + 1, cz + 1)

        return bilinear(m00.tempCenter, m10.tempCenter, m01.tempCenter, m11.tempCenter, tx, tz)
    }

    private fun bilinearSampleHumid(x: Int, z: Int): Double {
        val cx = x shr 4
        val cz = z shr 4
        val tx = ((x - (cx shl 4)).toDouble()) / 16.0
        val tz = ((z - (cz shl 4)).toDouble()) / 16.0

        val m00 = chunkMetaCache[chunkKey(cx, cz)] ?: getChunkMeta(cx, cz)
        val m10 = chunkMetaCache[chunkKey(cx + 1, cz)] ?: getChunkMeta(cx + 1, cz)
        val m01 = chunkMetaCache[chunkKey(cx, cz + 1)] ?: getChunkMeta(cx, cz + 1)
        val m11 = chunkMetaCache[chunkKey(cx + 1, cz + 1)] ?: getChunkMeta(cx + 1, cz + 1)

        return bilinear(m00.humidCenter, m10.humidCenter, m01.humidCenter, m11.humidCenter, tx, tz)
    }

    private fun bilinearSampleRain(x: Int, z: Int): Double {
        val cx = x shr 4
        val cz = z shr 4
        val tx = ((x - (cx shl 4)).toDouble()) / 16.0
        val tz = ((z - (cz shl 4)).toDouble()) / 16.0

        val m00 = chunkMetaCache[chunkKey(cx, cz)] ?: getChunkMeta(cx, cz)
        val m10 = chunkMetaCache[chunkKey(cx + 1, cz)] ?: getChunkMeta(cx + 1, cz)
        val m01 = chunkMetaCache[chunkKey(cx, cz + 1)] ?: getChunkMeta(cx, cz + 1)
        val m11 = chunkMetaCache[chunkKey(cx + 1, cz + 1)] ?: getChunkMeta(cx + 1, cz + 1)

        return bilinear(m00.rainCenter, m10.rainCenter, m01.rainCenter, m11.rainCenter, tx, tz)
    }
}

class ContinentBiomeResolver<T : PlatformBiome>(
    val continentNoise: NoiseSampler,   // Large scale land mask
    val elevationNoise: NoiseSampler,   // Medium scale elevation detail
    val biomeNoise: MultiParameterBiomeResolver<T>,
    val landThreshold: Double = 0.3,          // (0..1)
    val deepSeaLevel: Double = 0.07,          // (0..1)
    val coastThreshold: Double = 0.15,        // (0..1)
    val mountainThreshold: Double = 0.7
) : BiomeResolver<T>
{
    val deepOceanPalette: Palette<T> = biomeNoise.palette.split {
        it.category in
                setOf(BiomeCategory.DEEP_OCEAN,
                    BiomeCategory.COLD_DEEP_OCEAN,
                    BiomeCategory.WARM_DEEP_OCEAN
                )
    }
    val oceanPalette: Palette<T> = biomeNoise.palette.split {
        it.category in
                setOf(BiomeCategory.OCEAN,
                    BiomeCategory.COLD_OCEAN,
                    BiomeCategory.WARM_OCEAN
                )
    }

    init {
        biomeNoise.setPalette(biomeNoise.palette.split {
            it.category !in
                    setOf(BiomeCategory.DEEP_OCEAN,
                        BiomeCategory.COLD_DEEP_OCEAN,
                        BiomeCategory.WARM_DEEP_OCEAN,
                        BiomeCategory.OCEAN,
                        BiomeCategory.WARM_OCEAN,
                        BiomeCategory.COLD_OCEAN
                    )
        })
        biomeNoise.isMerged = true
    }

    override fun getBiomePalette(): Palette<T> {
        // We have multiple palettes, so we just return land's main palette for compatibility
        return biomeNoise.palette
    }

    override fun resolveBiome(x: Int, y: Int, z: Int, seed: Long): T {
        val continentFeatureSize = 500.0        // ~4k blocks for each big continent (tweak)
        val continentScale = 1.0 / continentFeatureSize

        // Sample base continent (use raw coords scaled; do NOT double-scale)
        val base = continentNoise.sample(x * continentScale, z * continentScale) // -1..1
        val baseN = ((base + 1.0) / 2.0).coerceIn(0.0, 1.0) // 0..1 mask value

        // Smoothstep helper
        fun smoothStep(edge0: Double, edge1: Double, t: Double): Double {
            val tt = ((t - edge0) / (edge1 - edge0)).coerceIn(0.0, 1.0)
            return tt * tt * (3.0 - 2.0 * tt) // classic smoothstep
        }

        // falloff region around threshold (coast size)
        val falloff = 0.06 // 6% falloff width — tweak to make coast thicker/thinner
        val landT = landThreshold.coerceIn(0.0, 1.0)

        // continent mask: 0 = ocean, 1 = solid land, with smooth edges
        val landMask = smoothStep(landT - falloff, landT + falloff, baseN)

        // Deep ocean / shallow ocean decision
        if (baseN < deepSeaLevel) {
            return deepOceanPalette.map.values.random(Random(seed)) // deterministic choice below
        }

        // Now sample elevation (use different scale)
        val elevScale = 1.0 / 600.0 // finer than continent, coarser than local noise
        val rawElev = elevationNoise.sample(x * elevScale, z * elevScale) // -1..1
        val rawElevN = ((rawElev + 1.0) / 2.0).coerceIn(0.0, 1.0)

        // Make elevation *follow* continent mask:
        // center of continent should be higher: raise elevation by mask^power
        val centerBoost = landMask.pow(1.5) // concentrates height to island centers
        val elev = (rawElevN * 0.9 + 0.1) * centerBoost // 0..1 scaled

        // shore detail: small noise layered on edges only
        val shoreNoiseScale = 1.0 / 30.0
        val shoreDetail = if (landMask in 0.01..0.99) {
            // small amplitude tweak based on a short-frequency sampler (you can create one)
            val s = elevationNoise.sample(x * shoreNoiseScale, z * shoreNoiseScale) * 0.05
            s
        } else 0.0

        val finalElev = (elev + shoreDetail).coerceIn(0.0, 1.0)

        // classification: deep ocean, ocean, coast, mountain, land
        if (landMask < 0.01) {
            // far ocean
            return deepOceanPalette.map.values.random()
        } else if (landMask < 0.15) {
            // nearshore shallow ocean
            return oceanPalette.map.values.random()
        }

        // now land: choose special categories by elevation
        when {
            finalElev > mountainThreshold -> return biomeNoise.palette.map.values.first { it.category == BiomeCategory.MOUNTAIN }
            finalElev < coastThreshold -> return biomeNoise.palette.map.values.first { it.category == BiomeCategory.COAST }
            else -> return biomeNoise.resolveBiome(x, y, z, seed)
        }
    }

}

fun computeAdaptiveThreshold(noise: NoiseSampler, targetOceanRatio: Double): Double {
    val samples = mutableListOf<Double>()
    for (x in 0 until 100) {
        for (z in 0 until 100) {
            samples += (noise.sample(x * 100.0, z * 100.0) + 1.0) / 2.0
        }
    }
    return samples.sorted()[(samples.size * targetOceanRatio).toInt()]
}

data class ContinentCenter(val x: Double, val z: Double, val radius: Double, val falloffPower: Double = 1.0)

class ContinentGenerator(
    seed: Long,
    val worldWidth: Int,
    val worldHeight: Int,
    val targetCount: Int,
    // sensible default: keep continents separated by at least a fraction of typical radius
    val minDist: Double = 300.0,
    val radiusRange: Pair<Double, Double> = 300.0 to 900.0,
    val maxAttemptsPerCenter: Int = 500
) {
    private val rng = Random(seed)
    val centers: List<ContinentCenter> = generateCenters().also {
        VSPEPlatformPlugin.platformLogger().debug("ContinentGenerator -> created ${it.size} centers")
        it.forEachIndexed { idx, c ->
            VSPEPlatformPlugin.platformLogger()
                .debug("  center[$idx] = (x=${"%.1f".format(c.x)}, z=${"%.1f".format(c.z)}, r=${"%.1f".format(c.radius)})")
        }
    }

    private fun generateCenters(): MutableList<ContinentCenter> {
        val list = mutableListOf<ContinentCenter>()
        var attempts = 0
        while (list.size < targetCount && attempts < targetCount * maxAttemptsPerCenter) {
            attempts++
            // place in positive coordinate space [0, worldWidth)
            val x = rng.nextDouble() * worldWidth
            val z = rng.nextDouble() * worldHeight
            val radius = radiusRange.first + rng.nextDouble() * (radiusRange.second - radiusRange.first)

            // if minDist is large make sure it's not impossibly big w.r.t world size/radius
            val tooClose = list.any { c ->
                val d = hypot(c.x - x, c.z - z)
                d < (c.radius * 0.5 + radius * 0.5 + minDist)
            }
            if (!tooClose) {
                // Slight variation in falloff to avoid identical falloff everywhere
                val falloff = 1.0 + rng.nextDouble() * 1.2
                list += ContinentCenter(x, z, radius, falloff)
            }
        }
        return list
    }

    val baseShapeNoise = JNoiseNoiseSampler(NoiseSamplerFactory.create(
        NoiseSamplerFactory.Type.PERLIN,
        seed = seedBase,
        frequency = 0.000008, // ~1-2 big features per 1000 blocks
        octaves = 3,
        lacunarity = 2.0,
        gain = 2.0,
        fbm = true,
        interpolation = Interpolation.COSINE
    ))

    // Smoothstep: nicer coast than linear
    private fun smoothFalloff(distance: Double, radius: Double, falloffPower: Double, x: Double, z: Double): Double {
        val noiseScale = 0.003
        val noise = baseShapeNoise.sample(x * noiseScale, z * noiseScale) * 0.4
        val effectiveRadius = radius * (1.0 + noise)
        return 1.0 - (distance / effectiveRadius).coerceIn(0.0, 1.0).pow(falloffPower)
    }

    // Compute mask at world location (0..1). Uses max of centers => distinct continents.
    fun maskAt(x: Double, z: Double): Double {
        var best = 0.0
        for (c in centers) {
            // A cheap AABB check: if outside bounding box skip exact hypot call
            if (x < c.x - c.radius || x > c.x + c.radius) continue
            if (z < c.z - c.radius || z > c.z + c.radius) continue
            val d = hypot(x - c.x, z - c.z)
            if (d > c.radius) continue
            val f = smoothFalloff(d, c.radius, c.falloffPower, x, z)
            if (f > best) best = f
            if (best >= 0.999) return 1.0
        }
        return best
    }
}

