package org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator

import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.vicky.platform.world.PlatformBlockState
import org.vicky.vspe.distance
import org.vicky.vspe.platform.VSPEPlatformPlugin
import org.vicky.vspe.platform.systems.dimension.Exceptions.NoSuitableBiomeException
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

open class Palette<T> {
    internal val map: MutableMap<Pair<Double, Double>, T> = mutableMapOf()

    fun getPaletteMap(): Map<Pair<Double, Double>, T>  {
        return map
    }

    open fun get(x: Double, @Nullable z: Double?): T {
        for ((range, result) in map) {
            if (x in range.first..range.second) {
                return result
            }
        }
        throw IllegalArgumentException("No matching palette entry for value $x")
    }
}

fun <T> Palette<T>.split(toTake: (T) -> Boolean): Palette<T> {
    val derivative: PaletteBuilder<T> = PaletteBuilder()

    for (e in this.map.values) {
        if (toTake(e)) {
            derivative.add(this.map.getKey(e), e)
        }
    }

    return derivative.build()
}

fun <K, T> MutableMap<K, T>.getKey(t: T): K {
    for (e in this.keys) {
        if (t == this[e]) return e
    }
    return error("Map dosent contain value $t")
}

/**
 * BiomeBlockDistributionPalette
 *
 * A 1D palette where entries are axis-aligned columns in (y) parameter space.
 * When multiple regions match a query, a weighted-random choice is made.
 */
class BiomeBlockDistributionPalette<T : PlatformBlockState<*>> internal constructor() : Palette<T>() {
    data class Entry<T>(
        val y0: Int, val y1: Int,           // absolute Y range (inclusive)
        val value: T,
        val weight: Double = 1.0
    ) {
        fun matches(y: Int): Boolean {
            val minY = min(y0, y1)
            val maxY = max(y0, y1)
            return (y in minY..maxY)
        }
    }

    private val entries = mutableListOf<Entry<T>>()
    private val rng = Random.Default

    /**
     * Add palette entry that applies for y in {y0..y1} and depth in {depth0..depth1}
     * Depth 0 = the surface block (columnTopY).
     */
    fun addAbsolute(y0: Int, y1: Int, value: T, weight: Double = 1.0) {
        require(weight >= 0.0)
        entries += Entry(y0, y1, value, weight)
    }

    /**
     * Convenience: add by normalized height (0.0..1.0) and depth range
     */
    fun addNormalized(norm0: Double, norm1: Double, value: T, weight: Double = 1.0,
                      minY: Int = -64, maxY: Int = 319) {
        val y0 = (norm0.coerceIn(0.0,1.0) * (maxY - minY) + minY).toInt()
        val y1 = (norm1.coerceIn(0.0,1.0) * (maxY - minY) + minY).toInt()
        addAbsolute(y0, y1, value, weight)
    }

    /**
     * Query by column top Y and world y. Internally computes depth = columnTopY - y.
     * Selects a matching entry, using weights if multiple match.
     */
    fun getFor(y: Int): T {
        val matches = entries.filter { it.matches(y) }
        if (matches.isEmpty()) error("No matching palette entry for y=$y")

        if (matches.size == 1) return matches[0].value

        val totalWeight = matches.sumOf { it.weight }
        if (totalWeight <= 0.0) return matches.random(rng).value

        var pick = rng.nextDouble() * totalWeight
        for (e in matches) {
            pick -= e.weight
            if (pick <= 0.0) return e.value
        }
        return matches.last().value
    }

    @Deprecated("It is advised to use #getFor(y) instead")
    override fun get(x: Double, z: Double?): T {
        if (aintSaid) {
            VSPEPlatformPlugin.platformLogger()
                .warn("It isn't advised to use #get(x, z) use the actual intended #getFor(y) in a BiomeBlockDistributionPalette please refrain and contact the developers...")
            aintSaid = false
        }
        return getFor(x.toInt())
    }

    fun clear() = entries.clear()
    fun getEntries(): MutableList<Entry<T>> = entries

    companion object {
        private var aintSaid: Boolean = true
        fun empty(): BiomeBlockDistributionPalette<*> =
            BiomeBlockDistributionPalette<PlatformBlockState<*>>()
    }
}

class BiomeBlockDistributionPaletteBuilder<T : PlatformBlockState<*>>() {
    val palette : BiomeBlockDistributionPalette<T> = BiomeBlockDistributionPalette()

    @JvmOverloads
    fun addLayer(yStart: Int, yEnd: Int, blockState: T, weight: Double = 1.0) : BiomeBlockDistributionPaletteBuilder<T> {
        palette.addAbsolute(yStart, yEnd, blockState, weight)
        return this
    }

    @JvmOverloads
    fun addNormalizedLayer(norm0: Double, norm1: Double, value: T, weight: Double = 1.0,
                      minY: Int = -64, maxY: Int = 319) : BiomeBlockDistributionPaletteBuilder<T> {
        val y0 = (norm0.coerceIn(0.0,1.0) * (maxY - minY) + minY).toInt()
        val y1 = (norm1.coerceIn(0.0,1.0) * (maxY - minY) + minY).toInt()
        addLayer(y0, y1, value, weight)
        return this
    }

    fun build(): BiomeBlockDistributionPalette<T> {
        return palette
    }
}

class NoiseBiomeDistributionPalette<B: PlatformBiome>(
    val temperatureNoiseSampler: NoiseSampler,
    val humidityNoiseSampler: NoiseSampler,
    val elevationNoiseSampler: NoiseSampler
): Palette<B>()
{

    override fun get(x: Double, @NotNull z: Double?): B {
        if (z == null) throw IllegalArgumentException("The value of z cannot be null in NoiseBiomeDistributionPalette#get")
        val temp = temperatureNoiseSampler.sample(x, z)
        val humid = humidityNoiseSampler.sample(x, z)
        val elev = elevationNoiseSampler.sample(x, z)

        return findBestMatch(temp, humid, elev)
    }

    private fun findBestMatch(temp: Double, humid: Double, elev: Double): B {
        return map.minByOrNull { entry ->
            val biome = entry.value
            val dt = biome.temperature - temp
            val dh = biome.humidity - humid
            val de = biome.elevation - elev
            dt * dt + dh * dh + de * de
        }?.value ?: throw NoSuitableBiomeException("No biome matched the temperature/humidity/elevation values.")
    }

    fun getWeightedInfluences(x: Double, z: Double): Map<PlatformBiome, Double> {
        val temp = temperatureNoiseSampler.sample(x, z)
        val humid = humidityNoiseSampler.sample(x, z)
        val elev = elevationNoiseSampler.sample(x, z)

        val weighted = mutableMapOf<PlatformBiome, Double>()
        var totalWeight = 0.0

        for ((_, biome) in map) {
            val d = distance(temp, humid, elev, biome.temperature, biome.humidity, biome.elevation)
            val w = 1.0 / (d * d + 0.001) // Avoid div-by-zero
            weighted[biome] = w
            totalWeight += w
        }

        return weighted.mapValues { it.value / totalWeight }
    }
}

class NoiseBiomeDistributionPaletteBuilder<T: PlatformBiome>(
    val temperatureNoiseSampler: NoiseSampler,
    val humidityNoiseSampler: NoiseSampler,
    val elevationNoiseSampler: NoiseSampler
) {
    val map: MutableMap<Pair<Double, Double>, T> = mutableMapOf()

    fun add(key: Pair<Double, Double>, value: T) : NoiseBiomeDistributionPaletteBuilder<T> {
        map[key] = value
        return this
    }

    fun add(min: Double, max: Double, value: T) : NoiseBiomeDistributionPaletteBuilder<T> {
        map[min to max] = value
        return this
    }

    fun build(): NoiseBiomeDistributionPalette<T> {
        return NoiseBiomeDistributionPalette<T>(
            temperatureNoiseSampler,
            humidityNoiseSampler,
            elevationNoiseSampler
        ).apply {
            map.putAll(this@NoiseBiomeDistributionPaletteBuilder.map)
        }
    }
}

open class PaletteBuilder<T> {
    companion object {
        fun <B> EMPTY() : PaletteBuilder<B> = object : PaletteBuilder<B>() {
            override fun add(key: Pair<Double, Double>, value: B): PaletteBuilder<B> {
                throw UnsupportedOperationException("Cannot add to EMPTY PaletteBuilder")
            }

            override fun add(min: Double, max: Double, value: B): PaletteBuilder<B> {
                throw UnsupportedOperationException("Cannot add to EMPTY PaletteBuilder")
            }

            override fun build(): Palette<B> {
                return Palette<B>() // Assuming Palette can be empty
            }
        }
    }
    val map: MutableMap<Pair<Double, Double>, T> = mutableMapOf()

    open fun add(key: Pair<Double, Double>, value: T) : PaletteBuilder<T> {
        map[key] = value
        return this
    }

    open fun add(min: Double, max: Double, value: T) : PaletteBuilder<T> {
        map[min to max] = value
        return this
    }

    open fun build(): Palette<T> {
        return Palette<T>().apply {
            map.putAll(this@PaletteBuilder.map)
        }
    }
}