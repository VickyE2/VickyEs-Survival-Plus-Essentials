package org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator

import org.vicky.vspe.BiomeCategory
import org.vicky.vspe.weightedRandomOrNullD

data class Rule(
    val t: ClosedFloatingPointRange<Double>,
    val e: ElevBand,
    val r: ClosedFloatingPointRange<Double>,
    val category: BiomeCategory
)

enum class ElevBand { DEEP, OCEAN, COAST, LOWLAND, HIGHLAND, PEAK }

fun elevBand(e: Double): ElevBand = when {
    e < 0.09 -> ElevBand.DEEP
    e < 0.36 -> ElevBand.OCEAN
    e < 0.42 -> ElevBand.COAST
    e < 0.68 -> ElevBand.LOWLAND
    e < 0.8 -> ElevBand.HIGHLAND
    else -> ElevBand.PEAK
}

fun elevBand(e: ElevBand): ClosedFloatingPointRange<Double> = when (e) {
    ElevBand.DEEP -> 0.0..0.09
    ElevBand.OCEAN -> 0.09..0.36
    ElevBand.COAST -> 0.36..0.42
    ElevBand.LOWLAND -> 0.42..0.68
    ElevBand.HIGHLAND -> 0.68..0.8
    else -> 0.8..1.0
}

val fallbackByBand = mapOf(
    ElevBand.DEEP to BiomeCategory.DEEP_OCEAN,
    ElevBand.OCEAN to BiomeCategory.OCEAN,
    ElevBand.COAST to BiomeCategory.COAST,
    ElevBand.LOWLAND to BiomeCategory.PLAINS,
    ElevBand.HIGHLAND to BiomeCategory.MOUNTAIN,
    ElevBand.PEAK to BiomeCategory.MOUNTAIN
)

private fun inRangeWithTol(value: Double, range: ClosedFloatingPointRange<Double>, tol: Double): Boolean {
    return value >= range.start - tol && value <= range.endInclusive + tol
}

class VickyMapGen<T : PlatformBiome>(
    val tempNoise: NoiseSampler,
    val rainNoise: NoiseSampler,
    val elevNoise: NoiseSampler,
    var palette: InvertedPalette<T>,
) : BiomeResolver<T> {
    val rules = listOf(
        // Deep oceans (cold → warm spectrum)
        Rule(0.0..0.3, ElevBand.DEEP, 0.0..1.0, BiomeCategory.FROZEN_DEEP_OCEAN),
        Rule(0.0..0.4, ElevBand.DEEP, 0.0..1.0, BiomeCategory.COLD_DEEP_OCEAN),
        Rule(0.28..0.78, ElevBand.DEEP, 0.0..1.0, BiomeCategory.DEEP_OCEAN),
        Rule(0.4..0.7, ElevBand.DEEP, 0.0..1.0, BiomeCategory.LUKEWARM_DEEP_OCEAN),
        Rule(0.6..1.0, ElevBand.DEEP, 0.0..1.0, BiomeCategory.WARM_DEEP_OCEAN),

        // Oceans (surface level)
        Rule(0.0..0.3, ElevBand.OCEAN, 0.0..1.0, BiomeCategory.FROZEN_OCEAN),
        Rule(0.0..0.4, ElevBand.OCEAN, 0.0..1.0, BiomeCategory.COLD_OCEAN),
        Rule(0.3..0.7, ElevBand.OCEAN, 0.0..1.0, BiomeCategory.OCEAN),
        Rule(0.4..0.8, ElevBand.OCEAN, 0.0..1.0, BiomeCategory.LUKEWARM_OCEAN),
        Rule(0.6..1.0, ElevBand.OCEAN, 0.0..1.0, BiomeCategory.WARM_OCEAN),

        // Coasts & beaches
        Rule(0.0..0.3, ElevBand.COAST, 0.0..0.3, BiomeCategory.ICY),
        Rule(0.0..0.3, ElevBand.COAST, 0.3..0.6, BiomeCategory.SNOWY_BEACH),
        Rule(0.3..0.6, ElevBand.COAST, 0.2..0.5, BiomeCategory.COLD_COAST),
        Rule(0.5..0.8, ElevBand.COAST, 0.5..0.8, BiomeCategory.COAST),
        Rule(0.7..1.0, ElevBand.COAST, 0.6..1.0, BiomeCategory.WARM_COAST),

        // Lowlands (forests, plains, swamps, tundra, deserts)
        Rule(0.0..0.3, ElevBand.LOWLAND, 0.12..0.4, BiomeCategory.TUNDRA),
        Rule(0.6..1.0, ElevBand.LOWLAND, 0.0..0.14, BiomeCategory.COLD_DESERT),
        Rule(0.6..1.0, ElevBand.LOWLAND, 0.0..0.12, BiomeCategory.DESERT),
        Rule(0.5..0.8, ElevBand.LOWLAND, 0.2..0.6, BiomeCategory.PLAINS),
        Rule(0.2..0.6, ElevBand.LOWLAND, 0.3..0.7, BiomeCategory.FOREST),
        Rule(0.4..0.6, ElevBand.LOWLAND, 0.4..0.7, BiomeCategory.COLD_SWAMP),
        Rule(0.4..0.7, ElevBand.LOWLAND, 0.6..1.0, BiomeCategory.SWAMP),

        // Highlands (transition to mountains/jungles)
        Rule(0.2..0.6, ElevBand.HIGHLAND, 0.3..0.6, BiomeCategory.TAIGA),
        Rule(0.6..0.9, ElevBand.HIGHLAND, 0.2..0.5, BiomeCategory.SAVANNA),
        Rule(0.7..1.0, ElevBand.HIGHLAND, 0.5..0.8, BiomeCategory.JUNGLE),
        Rule(0.8..1.0, ElevBand.HIGHLAND, 0.7..1.0, BiomeCategory.RAINFOREST),

        // Peaks (dry → wet spectrum)
        Rule(0.0..0.4, ElevBand.PEAK, 0.0..0.5, BiomeCategory.MESA),
        Rule(0.4..0.7, ElevBand.PEAK, 0.3..0.7, BiomeCategory.DESERT),
        Rule(0.5..1.0, ElevBand.PEAK, 0.4..0.8, BiomeCategory.MOUNTAIN),

        // Underground/other
        Rule(0.7..1.0, ElevBand.LOWLAND, 0.6..1.0, BiomeCategory.LUSH_CAVES),
        Rule(0.6..0.9, ElevBand.LOWLAND, 0.4..0.7, BiomeCategory.WETLAND)
    )
    private val replacementMap: Map<BiomeCategory, BiomeCategory> = mapOf(
        BiomeCategory.FROZEN_DEEP_OCEAN to BiomeCategory.COLD_DEEP_OCEAN,
        BiomeCategory.FROZEN_OCEAN to BiomeCategory.COLD_OCEAN,
        BiomeCategory.WARM_DEEP_OCEAN to BiomeCategory.LUKEWARM_DEEP_OCEAN,
        BiomeCategory.WARM_OCEAN to BiomeCategory.LUKEWARM_OCEAN,
        BiomeCategory.LUKEWARM_OCEAN to BiomeCategory.OCEAN,
        BiomeCategory.LUKEWARM_DEEP_OCEAN to BiomeCategory.DEEP_OCEAN,
        BiomeCategory.COLD_DEEP_OCEAN to BiomeCategory.WARM_DEEP_OCEAN,
        BiomeCategory.DEEP_OCEAN to BiomeCategory.OCEAN,
        BiomeCategory.COLD_OCEAN to BiomeCategory.WARM_OCEAN,
        BiomeCategory.OCEAN to BiomeCategory.OCEAN,
        BiomeCategory.COAST to BiomeCategory.PLAINS,
        BiomeCategory.SNOWY_BEACH to BiomeCategory.ICY,
        BiomeCategory.ICY to BiomeCategory.TUNDRA,
        BiomeCategory.TUNDRA to BiomeCategory.TAIGA,
        BiomeCategory.COLD_DESERT to BiomeCategory.TUNDRA,
        BiomeCategory.COLD_COAST to BiomeCategory.COAST,
        BiomeCategory.WARM_COAST to BiomeCategory.COAST,
        BiomeCategory.TAIGA to BiomeCategory.FOREST,
        BiomeCategory.FOREST to BiomeCategory.PLAINS,
        BiomeCategory.JUNGLE to BiomeCategory.RAINFOREST,
        BiomeCategory.RAINFOREST to BiomeCategory.FOREST,
        BiomeCategory.WETLAND to BiomeCategory.SWAMP,
        BiomeCategory.SWAMP to BiomeCategory.COAST,
        BiomeCategory.COLD_SWAMP to BiomeCategory.COAST,
        BiomeCategory.LUSH_CAVES to BiomeCategory.FOREST,
        BiomeCategory.MOUNTAIN to BiomeCategory.PLAINS,
        BiomeCategory.SAVANNA to BiomeCategory.PLAINS,
        BiomeCategory.MESA to BiomeCategory.DESERT,
        BiomeCategory.DESERT to BiomeCategory.PLAINS,
        BiomeCategory.NETHER to BiomeCategory.DESERT,
        BiomeCategory.END to BiomeCategory.PLAINS,
        BiomeCategory.PLAINS to BiomeCategory.PLAINS
    )

    val biomeMap: MutableMap<BiomeCategory, MutableList<Pair<Double, T>>> = mutableMapOf()

    init {
        val entries = palette.invertedMap.toList()

        for ((biome, rarity) in entries) {
            val avg = rarity.second
            val list = biomeMap.computeIfAbsent(biome.category) { mutableListOf() } // safe because we prepopulated
            list.add(avg to biome)
            // println("Biome ${biome.name} -> weight=$avg")
        }

        // post-process: ensure every band/category has *something* (fill from replacements or any available)
        // fillMissingCategories()
    }

    fun getForParams(r: Double, e: Double, t: Double, tolTemp: Double = 0.13, tolRain: Double = 0.15): BiomeCategory {
        val match = rules.firstOrNull { rule ->
            inRangeWithTol(t, rule.t, tolTemp) && e in elevBand(rule.e) && inRangeWithTol(r, rule.r, tolRain)
        }?.category

        return if (match != null) {
            if (biomeMap.keys.contains(match)) {
                match
            } else {
                var current = replacementMap[match]
                repeat(7) {
                    if (current == null) return@repeat
                    if (biomeMap.containsKey(current)) {
                        return current
                    }
                    current = replacementMap[current]
                }
                // fallback if no valid replacement found
                current ?: fallbackByBand[elevBand(e)]!!
            }
        } else {
            // println("Failed to find a rule that matched r: $r, e: $e, t: $t")
            fallbackByBand[elevBand(e)]!!
        }
    }

    override fun getBiomePalette(): InvertedPalette<T> = palette
    override fun resolveBiome(x: Int, y: Int, z: Int, seed: Long): T {
        val r = rainNoise.sample(x.toDouble(), z.toDouble())
        val e = elevNoise.sample(x.toDouble(), z.toDouble())
        val t = tempNoise.sample(x.toDouble(), z.toDouble())
        val category = getForParams(r, e, t)
        val list = biomeMap[category]
        return if (list != null) {
            val weighted = list.weightedRandomOrNullD { it.first }
            weighted?.second ?: // println("Failed to find a weighted biome from list: $list, Category: $category")
            biomeMap[category]!!.first().second
        } else {
            // println("Failed to find a biome that matched category: $category")
            biomeMap.values.first().first().second
        }
    }
}