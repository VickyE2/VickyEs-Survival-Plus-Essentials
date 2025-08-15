package org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator

import de.articdive.jnoise.interpolation.Interpolation
import org.vicky.platform.world.PlatformBlockState
import org.vicky.platform.world.PlatformWorld
import org.vicky.vspe.BiomeCategory
import org.vicky.platform.utils.ResourceLocation
import org.vicky.vspe.platform.systems.dimension.imagetester.seedBase
import org.vicky.vspe.weightedRandomOrNull
import java.lang.Math.clamp
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.absoluteValue
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sqrt
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
    val structureRegistry: StructureRegistry<T>
    val structurePlacer: StructurePlacer<T>
    val random: RandomSource
}

data class StructureRegistry<T>(
    val structures: Map<ResourceLocation, Pair<PlatformStructure<T>, StructureRule>>
)

open class ChunkGenerateContext<T, B: PlatformBiome>(
    val chunkX: Int,
    val chunkZ: Int,
    val dimension: PlatformDimension<T, B>
)

interface BiomeResolver<B: PlatformBiome>{
    fun getBiomePalette(): Palette<B>
    fun resolveBiome(x: Int, y: Int, z: Int, seed: Long): B
}

interface ChunkData<T, B: PlatformBiome> {
    fun setBlock(x: Int, y: Int, z: Int, block: PlatformBlockState<T>)
    fun setBiome(x: Int, y: Int, z: Int, biome: B)
}





class EvenSpreadBiomeResolver<B: PlatformBiome>(
    private val entries: List<Entry<B>>,
    private val noise: NoiseSampler,
    private val biomeSize: Double
) : BiomeResolver<B>
{

    override fun resolveBiome(x: Int, y: Int, z: Int, seed: Long): B {
        // Scale domain for biome size
        val nx = x / biomeSize
        val nz = z / biomeSize

        // Get noise in range [0,1]
        val value = (noise.sample(nx, nz) + 1) / 2.0

        // Map to biome by cumulative weight
        var sum = 0.0
        for (e in entries) {
            sum += e.weight
            if (value <= sum) return e.biome
        }
        return entries[entries.size - 1].biome
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

class MultiParameterBiomeResolver<T: PlatformBiome> @JvmOverloads constructor(
    val tempNoise: NoiseSampler,
    val humidNoise: NoiseSampler,
    val elevNoise: NoiseSampler,
    val rainNoise: NoiseSampler?,
    internal var palette: Palette<T>,
    val seaLevel: Double = 0.2,
    val deepSeaLevel: Double = 0.07,
    internal var isMerged: Boolean = false
) : BiomeResolver<T>
{
    val replacementMap: Map<BiomeCategory, BiomeCategory> = mapOf(
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

    fun setPalette(palette: Palette<T>) {
        this.palette = palette
    }

    override fun getBiomePalette(): Palette<T> {
        return palette
    }

    override fun resolveBiome(x: Int, y: Int, z: Int, seed: Long): T {
        // sample each map (scale tuned per‐parameter)
        val tRaw = tempNoise.sample(x*0.005, z*0.005)
        val hRaw = humidNoise.sample(x*0.01,  z*0.01)
        val eRaw = elevNoise.sample(x*0.02,  z*0.02)
        val rRaw = rainNoise?.sample(x*0.01, z*0.01) ?: 0.0

        // normalize to [0,1]
        val t = ((tRaw + 1) / 2).coerceIn(0.0,1.0)
        val h = ((hRaw + 1) / 2).coerceIn(0.0,1.0)
        val e = ((eRaw + 1) / 2).coerceIn(0.0,1.0)
        val r = ((rRaw + 1) / 2).coerceIn(0.0,1.0)

        // lookup a biome by the 4-tuple (t,h,e,r)
        return palette.lookup(t, h, e, r)
    }

    fun <B> findCandidatesWithFallback(
        initialDesired: Set<BiomeCategory>,
        getCandidatesFor: (BiomeCategory) -> List<B>
    ): List<B> {
        var currentLevel: Set<BiomeCategory> = initialDesired.toSet()
        val visited = mutableSetOf<BiomeCategory>()

        while (currentLevel.isNotEmpty()) {
            // collect candidates for this level
            val candidates = currentLevel.flatMap { cat -> getCandidatesFor(cat) }
            if (candidates.isNotEmpty()) return candidates

            // compute next level of fallback categories
            val nextLevel = currentLevel.mapNotNull { replacementMap[it] }.toSet()
            // stop if nothing to try or we've already tried these categories (prevents cycles)
            if (nextLevel.isEmpty() || visited.containsAll(nextLevel)) break

            visited.addAll(currentLevel)
            currentLevel = nextLevel
        }

        // nothing found through fallbacks
        return emptyList()
    }

    fun Palette<T>.lookup(
        t: Double, h: Double, e: Double, r: Double,
        random: Random = Random(9019090930830)  // or your own RandomSource
    ): T {
        // 1) Pick the *category* or “biome class” you want
        val desiredCategories = buildList {
            if (!isMerged) {
                val oceanBlend = 0.6
                val oceanTransition = ((seaLevel + oceanBlend) - e).coerceIn(0.0, oceanBlend) / oceanBlend
                val baseOceanChance = 0.6

                // 🌊 Ocean formation
                if (e < deepSeaLevel && random.nextDouble() < (baseOceanChance + 0.4 * oceanTransition)) {
                    if (t > 0.6) add(BiomeCategory.WARM_DEEP_OCEAN)
                    else if (t > 0.4) add(BiomeCategory.DEEP_OCEAN)
                    else add(BiomeCategory.COLD_DEEP_OCEAN)
                    return@buildList
                }
                else if (e < seaLevel && random.nextDouble() < (baseOceanChance + 0.4 * oceanTransition)) {
                    if (t > 0.6) add(BiomeCategory.WARM_OCEAN)
                    else if (t > 0.4) add(BiomeCategory.OCEAN)
                    else add(BiomeCategory.COLD_OCEAN)
                    return@buildList
                }
            }

            // ❄️ Cold areas
            if (t < 0.2) {
                if (h < 0.3) add(BiomeCategory.ICY)
                else add(BiomeCategory.TUNDRA)
            }
            // 🏜️ Cool areas
            else if (t < 0.4) {
                when {
                    h < 0.3 -> add(BiomeCategory.DESERT)
                    h < 0.6 -> add(BiomeCategory.SAVANNA)
                    else -> add(BiomeCategory.TAIGA)
                }
            }
            // 🌾 Temperate areas
            else if (t < 0.7) {
                when {
                    h < 0.3 -> add(BiomeCategory.DESERT)
                    h < 0.6 -> add(BiomeCategory.PLAINS)
                    else -> add(BiomeCategory.SWAMP)
                }
            }
            // 🌴 Warm/hot areas
            else {
                when {
                    h < 0.3 -> add(BiomeCategory.SAVANNA)
                    h < 0.7 -> add(BiomeCategory.JUNGLE)
                    else -> add(BiomeCategory.RAINFOREST)
                }
            }

            // 🏔️ High elevation
            if (e > 0.82) {
                add(BiomeCategory.MOUNTAIN)
            }

            // 💦 Very rainy
            if (r > 0.8) {
                add(BiomeCategory.WETLAND)
            }
        }


        // 2) Filter your palette down to only those biomes
        val candidatesWithFallback = findCandidatesWithFallback(desiredCategories.toSet()) { cat ->
            // EXAMPLE: if your biome palette exposes a collection of Biome objects with `.category`:
            getBiomePalette().map.values.filter { biome -> biome.category == cat }
        }

        if (candidatesWithFallback.isEmpty()) {
            println("No biome matches [$desiredCategories] even after fallbacks — picking fully random.")
            return getBiomePalette().map.values.random()
        }

        val weighted = candidatesWithFallback.map { b ->
            val tempNorm = (b.temperature - t) / 1.0
            val humidNorm = (b.humidity - h) / 1.0
            val elevNorm = (b.elevation - e) / 1.0
            val rainNorm = (b.rainfall - r) / 1.0

            val dist = sqrt(
                tempNorm.pow(2) +
                        humidNorm.pow(2) +
                        elevNorm.pow(2) +
                        rainNorm.pow(2)
            )

            val weight = (1.0 - dist).coerceAtLeast(0.0)
            b to weight
        }

        // Now compute weights on candidatesWithFallback (replace `weighted` usage accordingly)
        val totalWeight = weighted.sumOf { it.second } // if you still build 'weighted' from candidatesWithFallback

        if (totalWeight == 0.0) {
            // fallback to uniform pick if all weights are zero (i.e., all biomes are too far)
            return candidatesWithFallback.random(random)
        }

        var pick = random.nextDouble() * totalWeight
        for ((biome, weight) in weighted) {
            pick -= weight
            if (pick <= 0.0) return biome
        }

        return weighted.last().first
    }
}

class ContinentBiomeResolver<T : PlatformBiome>(
    val continentNoise: NoiseSampler,   // Large scale land mask
    val elevationNoise: NoiseSampler,   // Medium scale elevation detail
    val biomeNoise: MultiParameterBiomeResolver<T>, // Biome resolver for all
    val landThreshold: Double = 0.3,    // Higher = more ocean
    val deepOceanThreshold: Double = -0.6,
    val coastThreshold: Double = -0.1,
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
        if (baseN < deepOceanThreshold) {
            return deepOceanPalette.map.values.random()
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
        println("ContinentGenerator -> created ${it.size} centers")
        it.forEachIndexed { idx, c ->
            println("  center[$idx] = (x=${"%.1f".format(c.x)}, z=${"%.1f".format(c.z)}, r=${"%.1f".format(c.radius)})")
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
        val noise = baseShapeNoise.sample(x * noiseScale, z * noiseScale) * 0.4 // ±40% variation
        val effectiveRadius = radius * (1.0 + noise)
       return 1.0 - clamp(distance / effectiveRadius, 0.0, 1.0).pow(falloffPower)
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

