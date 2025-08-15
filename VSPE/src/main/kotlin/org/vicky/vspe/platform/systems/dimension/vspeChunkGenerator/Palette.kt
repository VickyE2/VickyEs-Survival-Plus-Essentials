package org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator

import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.vicky.vspe.distance
import org.vicky.vspe.platform.systems.dimension.Exceptions.NoSuitableBiomeException

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

class NoiseBiomeDistributionPalette<B: PlatformBiome>(
    val temperatureNoiseSampler: NoiseSampler,
    val humidityNoiseSampler: NoiseSampler,
    val elevationNoiseSampler: NoiseSampler
): Palette<B>() {

    override fun get(x: Double, @NotNull z: Double?): B {
        if (z == null) throw IllegalArgumentException("The value of z cannot be null in NoiseBiomeDistributionPalette#get");
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
        return this;
    }

    fun add(min: Double, max: Double, value: T) : NoiseBiomeDistributionPaletteBuilder<T> {
        map[min to max] = value
        return this;
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
        return this;
    }

    open fun add(min: Double, max: Double, value: T) : PaletteBuilder<T> {
        map[min to max] = value
        return this;
    }

    open fun build(): Palette<T> {
        return Palette<T>().apply {
            map.putAll(this@PaletteBuilder.map)
        }
    }
}