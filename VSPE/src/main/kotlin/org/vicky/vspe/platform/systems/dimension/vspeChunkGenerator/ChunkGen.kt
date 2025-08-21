package org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator

import de.articdive.jnoise.interpolation.Interpolation
import de.pauleff.api.ICompoundTag
import org.vicky.platform.utils.Vec3
import org.vicky.platform.world.PlatformBlockState
import org.vicky.platform.world.PlatformLocation
import org.vicky.platform.world.PlatformWorld
import org.vicky.vspe.BiomeCategory
import org.vicky.vspe.platform.VSPEPlatformPlugin
import org.vicky.vspe.platform.systems.dimension.imagetester.seedBase
import java.util.concurrent.ThreadLocalRandom
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
    val locationProvider: (x: Int, y: Int, z: Int) -> PlatformLocation
)

interface BiomeResolver<B: PlatformBiome>{
    fun getBiomePalette(): Palette<B>
    fun resolveBiome(x: Int, y: Int, z: Int, seed: Long): B
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
    private val tempNoise: NoiseSampler,
    private val humidNoise: NoiseSampler,
    private val elevNoise: NoiseSampler,
    private val rainNoise: NoiseSampler?,
    internal var palette: Palette<T>,
    val seaLevel: Double = 0.2,
    val deepSeaLevel: Double = 0.07,
    internal var isMerged: Boolean = false
) : BiomeResolver<T> {

    // fallback mapping (unchanged)
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

    // ----- internal cached structures derived from palette -----
    private val lock = Any()

    private data class BiomeEntry<T : PlatformBiome>(
        val biome: T,
        val temp: Double,
        val humid: Double,
        val elev: Double,
        val rain: Double,
        val category: BiomeCategory
    )

    // EnumMap-like: index by ordinal (fast)
    private var entriesByCategory: Array<Array<BiomeEntry<T>>> =
        Array(BiomeCategory.values().size) { emptyArray() }

    // flattened list of all entries (also cached)
    private var allEntriesCached: Array<BiomeEntry<T>> = emptyArray()

    init {
        rebuildCaches()
    }

    fun setPalette(palette: Palette<T>) {
        synchronized(lock) {
            this.palette = palette
            rebuildCaches()
        }
    }

    override fun getBiomePalette(): Palette<T> {
        return palette
    }

    // rebuild caches from the palette (call when palette changes)
    private fun rebuildCaches() {
        synchronized(lock) {
            val byCat = Array(BiomeCategory.values().size) { ArrayList<BiomeEntry<T>>() }
            val vals = ArrayList<BiomeEntry<T>>(palette.map.size)

            // assume palette.map.values is iterable of PlatformBiome (T)
            for (b in palette.map.values) {
                // read numeric properties once
                val te = b.temperature.toDouble()
                val hu = b.humidity.toDouble()
                val el = b.elevation.toDouble()
                val ra = b.rainfall.toDouble()
                val cat = b.category
                val entry = BiomeEntry(b, te, hu, el, ra, cat)
                byCat[cat.ordinal].add(entry)
                vals.add(entry)
            }

            entriesByCategory = Array(byCat.size) { idx ->
                byCat[idx].toTypedArray()
            }
            allEntriesCached = vals.toTypedArray()
        }
    }

    override fun resolveBiome(x: Int, y: Int, z: Int, seed: Long): T {
        // sample each map (scale tuned per-parameter)
        val tRaw = tempNoise.sample(x * 0.005, z * 0.005)
        val hRaw = humidNoise.sample(x * 0.01, z * 0.01)
        val eRaw = elevNoise.sample(x * 0.02, z * 0.02)
        val rRaw = rainNoise?.sample(x * 0.01, z * 0.01) ?: 0.0

        // normalize to [0,1]
        val t = ((tRaw + 1.0) * 0.5).coerceIn(0.0, 1.0)
        val h = ((hRaw + 1.0) * 0.5).coerceIn(0.0, 1.0)
        val e = ((eRaw + 1.0) * 0.5).coerceIn(0.0, 1.0)
        val r = ((rRaw + 1.0) * 0.5).coerceIn(0.0, 1.0)

        // delegate to optimized lookup (fast, no allocation)
        return lookupFast(t, h, e, r, seed)
    }

    /**
     * Fast lookup using precomputed arrays. Thread-friendly (no allocation).
     * Uses squared-distance metric (avoid sqrt).
     */
    private fun lookupFast(t: Double, h: Double, e: Double, r: Double, seed: Long): T {
        // 1) pick desired categories quickly (no alloc)
        val desiredCategories = pickDesiredCategories(t, h, e, r)

        // 2) find candidate entries using fallback (walk replacementMap; no list building)
        val candidates = findCandidatesWithFallbackFast(desiredCategories)
        if (candidates.isEmpty()) {
            // fallback to first available biome
            val all = allEntriesCached
            return if (all.isNotEmpty()) all[0].biome else palette.map.values.first()
        }

        // 3) compute squared distance and pick using weights
        // weight = max(0, 1 - dist) where dist is sqrt(sqDist) originally; but we can map squaredDist -> weight
        // a cheap approach: use (1.0 - sqrt(sqDist)) approximated; to avoid sqrt we can scale accordingly:
        // For small candidate sets the cost is small; we will compute sqrt once per candidate. This is OK.
        var totalWeight = 0.0
        val weights = DoubleArray(candidates.size)
        // Use ThreadLocalRandom with seed-based mix so behavior can be somewhat repeatable per-call if desired.
        val rnd = ThreadLocalRandom.current()

        var i = 0
        while (i < candidates.size) {
            val ent = candidates[i]
            // normalized difference
            val dt = ent.temp - t
            val dh = ent.humid - h
            val de = ent.elev - e
            val dr = ent.rain - r

            val sq = dt * dt + dh * dh + de * de + dr * dr
            val dist = kotlin.math.sqrt(sq) // computing sqrt here; cheaper than many allocations
            var weight = 1.0 - dist
            if (weight < 0.0) weight = 0.0
            weights[i] = weight
            totalWeight += weight
            i++
        }

        if (totalWeight <= 0.0) {
            // uniform random among candidates
            val idx = (rnd.nextDouble() * candidates.size).toInt().coerceIn(0, candidates.size - 1)
            return candidates[idx].biome
        }

        var pick = rnd.nextDouble() * totalWeight
        i = 0
        while (i < candidates.size) {
            pick -= weights[i]
            if (pick <= 0.0) return candidates[i].biome
            i++
        }
        return candidates.last().biome
    }

    /**
     * Build the "desired categories" small array (no list alloc).
     * Returns an IntArray of category ordinals; length 0 means none.
     */
    private fun pickDesiredCategories(t: Double, h: Double, e: Double, r: Double): IntArray {
        // typical result is 1-3 categories; use small buffer
        val buf = IntArray(6)
        var count = 0

        if (!isMerged) {
            val oceanBlend = 0.6
            val oceanTransition = ((seaLevel + oceanBlend) - e).coerceIn(0.0, oceanBlend) / oceanBlend
            val baseOceanChance = 0.6
            // Note: deterministic randomness removed here to keep it cheap.
            if (e < deepSeaLevel && baseOceanChance + 0.4 * oceanTransition > 0.5) {
                val cat =
                    if (t > 0.6) BiomeCategory.WARM_DEEP_OCEAN else if (t > 0.4) BiomeCategory.DEEP_OCEAN else BiomeCategory.COLD_DEEP_OCEAN
                buf[count++] = cat.ordinal
                return buf.copyOf(count)
            } else if (e < seaLevel && baseOceanChance + 0.4 * oceanTransition > 0.5) {
                val cat =
                    if (t > 0.6) BiomeCategory.WARM_OCEAN else if (t > 0.4) BiomeCategory.OCEAN else BiomeCategory.COLD_OCEAN
                buf[count++] = cat.ordinal
                return buf.copyOf(count)
            }
        }

        if (t < 0.2) {
            if (h < 0.3) buf[count++] = BiomeCategory.ICY.ordinal else buf[count++] = BiomeCategory.TUNDRA.ordinal
        } else if (t < 0.4) {
            if (h < 0.3) buf[count++] = BiomeCategory.DESERT.ordinal
            else if (h < 0.6) buf[count++] = BiomeCategory.SAVANNA.ordinal
            else buf[count++] = BiomeCategory.TAIGA.ordinal
        } else if (t < 0.7) {
            if (h < 0.3) buf[count++] = BiomeCategory.DESERT.ordinal
            else if (h < 0.6) buf[count++] = BiomeCategory.PLAINS.ordinal
            else buf[count++] = BiomeCategory.SWAMP.ordinal
        } else {
            if (h < 0.3) buf[count++] = BiomeCategory.SAVANNA.ordinal
            else if (h < 0.7) buf[count++] = BiomeCategory.JUNGLE.ordinal
            else buf[count++] = BiomeCategory.RAINFOREST.ordinal
        }

        if (e > 0.82) buf[count++] = BiomeCategory.MOUNTAIN.ordinal
        if (r > 0.8) buf[count++] = BiomeCategory.WETLAND.ordinal

        return if (count == buf.size) buf else buf.copyOf(count)
    }

    /**
     * Very fast fallback search: tries categories in "currentLevel", if none found maps them
     * to next-level via replacementMap using ordinal indices and returns the first non-empty
     * array of BiomeEntry. Avoids allocating temporary lists whenever possible.
     */
    private fun findCandidatesWithFallbackFast(initial: IntArray): Array<BiomeEntry<T>> {
        if (initial.isEmpty()) return emptyArray()
        val seen = BooleanArray(entriesByCategory.size)
        val work = IntArray(entriesByCategory.size)
        var workLen = 0
        for (o in initial) {
            if (o in entriesByCategory.indices) work[workLen++] = o
        }

        while (workLen > 0) {
            // check each ordinal in work for entries
            for (i in 0 until workLen) {
                val ord = work[i]
                val arr = entriesByCategory.getOrNull(ord)
                if (arr != null && arr.isNotEmpty()) return arr
            }

            // compute next level into the same work array
            var nextLen = 0
            for (i in 0 until workLen) {
                val ord = work[i]
                if (ord < 0 || ord >= entriesByCategory.size) continue
                if (seen[ord]) continue
                seen[ord] = true
                val cat = BiomeCategory.values()[ord]
                val next = replacementMap[cat] ?: continue
                val nextOrd = next.ordinal
                if (!seen[nextOrd]) {
                    work[nextLen++] = nextOrd
                }
            }
            if (nextLen == 0) break
            workLen = nextLen
        }
        return emptyArray()
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

