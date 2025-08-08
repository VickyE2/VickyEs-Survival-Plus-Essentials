package org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator

import org.vicky.platform.world.PlatformBlockState
import org.vicky.platform.world.PlatformWorld
import org.vicky.vspe.BiomeCategory
import org.vicky.platform.utils.ResourceLocation
import org.vicky.vspe.weightedRandomOrNull
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.absoluteValue
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





class MultiParameterBiomeResolver<T: PlatformBiome>(
    val tempNoise: FBMGenerator,
    val humidNoise: FBMGenerator,
    val elevNoise: FBMGenerator,
    val rainNoise: FBMGenerator?,
    val palette: Palette<T>,
    val seaLevel: Double = 0.2
) : BiomeResolver<T> {
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

    fun Palette<T>.lookup(
        t: Double, h: Double, e: Double, r: Double,
        random: Random = Random(9019090930830)  // or your own RandomSource
    ): T {
        // 1) Pick the *category* or “biome class” you want
        val desiredCategories = buildList {
            val oceanBlend = 0.6
            val oceanTransition = ((seaLevel + oceanBlend) - e).coerceIn(0.0, oceanBlend) / oceanBlend
            val baseOceanChance = 0.6

            // 🌊 Ocean formation
            if (e < seaLevel && random.nextDouble() < (baseOceanChance + 0.4 * oceanTransition)) {
                add(BiomeCategory.OCEAN)
                return@buildList
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
        val candidates = this.map.values.filter { it.category in desiredCategories }

        val weighted = candidates.map { b ->
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


        if (candidates.isEmpty()) {
            println("No biome matches [$desiredCategories]")
            return getBiomePalette().map.values.random()
        }

        val totalWeight = weighted.sumOf { it.second }
        if (totalWeight == 0.0) {
            // fallback to uniform pick if all weights are zero (i.e., all biomes are too far)
            return candidates.random(random)
        }

        var pick = random.nextDouble() * totalWeight
        for ((biome, weight) in weighted) {
            pick -= weight
            if (pick <= 0.0) return biome
        }

        return weighted.last().first
    }
}

