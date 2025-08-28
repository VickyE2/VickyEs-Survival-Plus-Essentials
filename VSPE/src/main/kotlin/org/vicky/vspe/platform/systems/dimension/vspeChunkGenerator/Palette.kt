package org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator

import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.vicky.platform.world.PlatformBlockState
import org.vicky.vspe.distance
import org.vicky.vspe.platform.systems.dimension.Exceptions.NoSuitableBiomeException
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

open class Palette<T> {
    internal val map: MutableMap<Pair<Double, Double>, T> = mutableMapOf()

    open fun getPaletteMap(): Map<Pair<Double, Double>, T> {
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
 * More modular BiomeBlockDistributionPalette.
 *
 * Use PaletteBuilder to add layers:
 *  - addAbsolute(y0,y1,block)
 *  - addDepthLayer(depth0, depth1, block)  // depth = columnTopY - y
 *  - addShoreLayer(belowSea = 2, aboveSea = 1, block)
 *  - addUnderwater(depth0,depth1,block)    // underwater depth relative to seaLevel
 *  - addPredicate { y, top, depth, sea -> ... } // fully custom
 *
 * Call getFor(y, columnTopY, seaLevel) to evaluate (preferred).
 */
class BiomeBlockDistributionPalette<T : PlatformBlockState<*>> {
    private val entries = mutableListOf<Entry<T>>()
    private var rng: Random = Random.Default

    // 1) priority enum
    enum class Priority(val rank: Int) {
        PREDICATE(1),
        NOISE(2),
        DEPTH(3),      // depth-based layers (relative to column top)
        ABSOLUTE(4),   // absolute y-range
        UNDERWATER(5),
        SHORE(6)       // highest priority
    }

    // 2) Entry base (note added `priority`)
    sealed class Entry<T>(
        val value: T?,
        val weight: Double,
        val priority: Priority = Priority.ABSOLUTE
    ) {
        abstract fun matches(y: Int, columnTopY: Int, seaLevel: Int): Boolean
    }

    // Example: update your existing subclasses to pass priority explicitly
    data class AbsoluteEntry<T>(val y0: Int, val y1: Int, val v: T, val w: Double) :
        Entry<T>(v, w, Priority.ABSOLUTE) {
        override fun matches(y: Int, columnTopY: Int, seaLevel: Int): Boolean {
            val lo = min(y0, y1)
            val hi = max(y0, y1)
            return y in lo..hi
        }
    }

    data class DepthEntry<T>(val depth0: Int, val depth1: Int, val v: T, val w: Double) :
        Entry<T>(v, w, Priority.DEPTH) {
        override fun matches(y: Int, columnTopY: Int, seaLevel: Int): Boolean {
            val depth = columnTopY - y
            val lo = min(depth0, depth1)
            val hi = max(depth0, depth1)
            return depth in lo..hi
        }
    }

    data class ShoreEntry<T>(
        val belowSea: Int, val aboveSea: Int, val v: T, val w: Double
    ) : Entry<T>(v, w, Priority.SHORE) {
        override fun matches(y: Int, columnTopY: Int, seaLevel: Int): Boolean {
            val lo = seaLevel - belowSea
            val hi = seaLevel + aboveSea
            return columnTopY in lo..hi
        }
    }

    data class UnderwaterEntry<T>(
        val depth0: Int, val depth1: Int, val v: T, val w: Double
    ) : Entry<T>(v, w, Priority.UNDERWATER) {
        override fun matches(y: Int, columnTopY: Int, seaLevel: Int): Boolean {
            val underwaterDepth = seaLevel - y
            val lo = min(depth0, depth1)
            val hi = max(depth0, depth1)
            return underwaterDepth in lo..hi
        }
    }

    data class PredicateEntry<T>(
        val pred: (Int, Int, Int) -> Boolean,
        val v: T,
        val w: Double
    ) : Entry<T>(v, w, Priority.PREDICATE) {
        override fun matches(y: Int, columnTopY: Int, seaLevel: Int): Boolean =
            pred(y, columnTopY, seaLevel)
    }

    // Noise entry: priority = NOISE, but it uses noise to pick block
    data class NoiseEntry<T>(
        val y0: Int,
        val y1: Int,
        val noise: (x: Int, z: Int, y: Int) -> Double,
        val thresholds: List<Pair<T, Double>>
    ) : Entry<T>(thresholds.first().first, thresholds.sumOf { it.second }, Priority.NOISE) {

        override fun matches(y: Int, columnTopY: Int, seaLevel: Int): Boolean {
            val lo = min(y0, y1)
            val hi = max(y0, y1)
            return y in lo..hi
        }

        // map noise -> pick block by thresholds (weights)
        fun pick(x: Int, y: Int, z: Int): T {
            val n = noise(x, z, y).coerceIn(0.0, 1.0)
            val total = thresholds.sumOf { it.second }
            var acc = 0.0
            for ((block, w) in thresholds) {
                acc += (w / total)
                if (n <= acc) return block
            }
            return thresholds.last().first
        }
    }


    // builder helpers ---------------------------------------------------

    fun addAbsolute(y0: Int, y1: Int, value: T, weight: Double = 1.0) {
        require(weight >= 0.0)
        entries.add(AbsoluteEntry(y0, y1, value, weight))
    }

    /**
     * Add a layer by *depth* relative to the column top:
     * depth 0 = column top, depth 1 = one block below surface, etc.
     */
    fun addDepthLayer(depth0: Int, depth1: Int, value: T, weight: Double = 1.0) {
        require(weight >= 0.0)
        entries.add(DepthEntry(depth0, depth1, value, weight))
    }

    /**
     * Add a shore layer that applies when the *column top* is within [seaLevel - belowSea .. seaLevel + aboveSea].
     * Example: addShoreLayer(belowSea = 2, aboveSea = 1, shoreBlock) matches columns whose top is near sea level.
     */
    fun addShoreLayer(belowSea: Int = 2, aboveSea: Int = 1, value: T, weight: Double = 1.0) {
        require(belowSea >= 0 && aboveSea >= 0 && weight >= 0.0)
        entries.add(ShoreEntry(belowSea, aboveSea, value, weight))
    }

    /**
     * Add a layer for underwater blocks by how many blocks below sea level (seaLevel - y).
     * E.g. addUnderwater(1, 5, someBlock) applies to blocks 1 to 5 blocks below sea.
     */
    fun addUnderwaterLayer(depth0: Int, depth1: Int, value: T, weight: Double = 1.0) {
        require(depth0 >= 0 && depth1 >= 0 && weight >= 0.0)
        entries.add(UnderwaterEntry(depth0, depth1, value, weight))
    }

    /**
     * Add a custom predicate. Predicate receives (y, columnTopY, seaLevel).
     */
    fun addPredicate(pred: (Int, Int, Int) -> Boolean, value: T, weight: Double = 1.0) {
        require(weight >= 0.0)
        entries.add(PredicateEntry(pred, value, weight))
    }

    /**
     * If you want the palette to pick a default block when nothing matches:
     * addAbsolute(Int.MIN_VALUE, Int.MAX_VALUE, defaultBlock)
     * or use this convenience:
     */
    fun addDefault(value: T, weight: Double = 1.0) {
        addAbsolute(Int.MIN_VALUE / 2, Int.MAX_VALUE / 2, value, weight) // very wide catch-all
    }

    // API to query -----------------------------------------------------

    /**
     * Primary API: provide the world column top Y so depth/shore/etc can be computed.
     * seaLevel default kept at 63-like defaults but let caller pass exact sea level.
     */
    @Deprecated("Use getFor(x, y, z, columnTopY, seaLevel) instead")
    fun getFor(y: Int, columnTopY: Int, seaLevel: Int = 63): T =
        getFor(0, y, 0, columnTopY, seaLevel)

    /**
     * Backwards-compatible convenience: minimal call if caller only supplies y
     * (assumes columnTopY == y so depth-based matchers behave like absolute ranges).
     *
     * PLEASE prefer getFor(y, columnTopY, seaLevel).
     */
    @Deprecated("Use getFor(y, columnTopY, seaLevel) instead")
    fun getFor(y: Int): T {
        return getFor(y, y, 63)
    }

    fun getFor(x: Int, y: Int, z: Int, columnTopY: Int, seaLevel: Int = 63): T {
        val matches = entries.filter { it.matches(y, columnTopY, seaLevel) }
        if (matches.isEmpty()) error("No matching palette entry at $y (top=$columnTopY, sea=$seaLevel)")

        // pick the highest priority group present
        val bestRank = matches.maxOf { it.priority.rank }
        val candidates = matches.filter { it.priority.rank == bestRank }

        // If single candidate and it's noise, let it pick
        if (candidates.size == 1 && candidates[0] is NoiseEntry<*>) {
            @Suppress("UNCHECKED_CAST")
            return (candidates[0] as NoiseEntry<T>).pick(x, y, z)
        }

        // weighted selection inside candidate group
        val totalWeight = candidates.sumOf { it.weight }
        if (totalWeight <= 0.0) return candidates.random(rng).value!!

        var pick = rng.nextDouble() * totalWeight
        for (e in candidates) {
            pick -= e.weight
            if (pick <= 0.0) {
                return if (e is NoiseEntry<*>) {
                    @Suppress("UNCHECKED_CAST")
                    (e as NoiseEntry<T>).pick(x, y, z)
                } else {
                    e.value!!
                }
            }
        }
        return candidates.last().value!!
    }


    fun clear() = entries.clear()
    fun getEntries(): List<Entry<T>> = entries.toList()

    fun setRandom(r: Random) {
        rng = r
    }

    companion object {
        private var aintSaid: Boolean = true
        fun empty(): BiomeBlockDistributionPalette<*> =
            BiomeBlockDistributionPalette<PlatformBlockState<*>>()
    }
}

class BiomeBlockDistributionPaletteBuilder<T : PlatformBlockState<*>> {
    private val palette = BiomeBlockDistributionPalette<T>()

    @JvmOverloads
    fun addLayer(yStart: Int, yEnd: Int, blockState: T, weight: Double = 1.0): BiomeBlockDistributionPaletteBuilder<T> {
        palette.addAbsolute(yStart, yEnd, blockState, weight)
        return this
    }
    @JvmOverloads
    fun addNormalizedLayer(norm0: Double, norm1: Double, value: T, weight: Double = 1.0,
                           minY: Int = -64, maxY: Int = 319
    ): BiomeBlockDistributionPaletteBuilder<T> {
        val y0 = (norm0.coerceIn(0.0, 1.0) * (maxY - minY) + minY).toInt()
        val y1 = (norm1.coerceIn(0.0, 1.0) * (maxY - minY) + minY).toInt()
        palette.addAbsolute(y0, y1, value, weight)
        return this
    }

    @JvmOverloads
    fun addNoiseLayer(
        y0: Int,
        y1: Int,
        noise: (x: Int, z: Int, y: Int) -> Double,
        thresholds: List<Pair<T, Double>>
    ): BiomeBlockDistributionPaletteBuilder<T> {
        val total = thresholds.sumOf { it.second }
        val normalized = thresholds.map { it.first to (it.second / total) }
        palette.getEntries().plus(
            BiomeBlockDistributionPalette.NoiseEntry(y0, y1, noise, normalized)
        )
        return this
    }

    @JvmOverloads
    fun addDepthLayer(
        depth0: Int,
        depth1: Int,
        blockState: T,
        weight: Double = 1.0
    ): BiomeBlockDistributionPaletteBuilder<T> {
        palette.addDepthLayer(depth0, depth1, blockState, weight)
        return this
    }

    @JvmOverloads
    fun addShoreLayer(
        belowSea: Int = 2,
        aboveSea: Int = 1,
        blockState: T,
        weight: Double = 1.0
    ): BiomeBlockDistributionPaletteBuilder<T> {
        palette.addShoreLayer(belowSea, aboveSea, blockState, weight)
        return this
    }

    @JvmOverloads
    fun addUnderwaterLayer(
        depth0: Int,
        depth1: Int,
        blockState: T,
        weight: Double = 1.0
    ): BiomeBlockDistributionPaletteBuilder<T> {
        palette.addUnderwaterLayer(depth0, depth1, blockState, weight)
        return this
    }

    @JvmOverloads
    fun addPredicate(
        pred: (Int, Int, Int) -> Boolean,
        blockState: T,
        weight: Double = 1.0
    ): BiomeBlockDistributionPaletteBuilder<T> {
        palette.addPredicate(pred, blockState, weight)
        return this
    }

    @JvmOverloads
    fun addDefault(blockState: T, weight: Double = 1.0): BiomeBlockDistributionPaletteBuilder<T> {
        palette.addDefault(blockState, weight)
        return this
    }

    fun build(): BiomeBlockDistributionPalette<T> = palette
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

open class InvertedPalette<T> : Palette<T>() {
    internal val invertedMap: MutableMap<T, Pair<Double, Double>> = mutableMapOf()

    override fun get(x: Double, z: Double?): T {
        error("Inverted palette has no use of Palette methods")
    }

    override fun getPaletteMap(): Map<Pair<Double, Double>, T> {
        error("Inverted palette has no use of Palette methods")
    }

    fun getInvertedPaletteMap(): Map<T, Pair<Double, Double>> {
        return invertedMap
    }

    open fun getInv(x: Double, @Nullable z: Double?): T {
        for ((result, range) in invertedMap) {
            if (x in range.first..range.second) {
                return result
            }
        }
        throw IllegalArgumentException("No matching palette entry for value $x")
    }
}

open class InvertedPaletteBuilder<T> {
    companion object {
        fun <B> EMPTY(): InvertedPaletteBuilder<B> = object : InvertedPaletteBuilder<B>() {
            override fun add(key: Pair<Double, Double>, value: B): InvertedPaletteBuilder<B> {
                throw UnsupportedOperationException("Cannot add to EMPTY PaletteBuilder")
            }

            override fun add(min: Double, max: Double, value: B): InvertedPaletteBuilder<B> {
                throw UnsupportedOperationException("Cannot add to EMPTY PaletteBuilder")
            }

            override fun build(): InvertedPalette<B> {
                return InvertedPalette<B>() // Assuming Palette can be empty
            }
        }
    }

    val map: MutableMap<T, Pair<Double, Double>> = mutableMapOf()

    open fun add(key: Pair<Double, Double>, value: T): InvertedPaletteBuilder<T> {
        map[value] = key
        return this
    }

    open fun add(min: Double, max: Double, value: T): InvertedPaletteBuilder<T> {
        map[value] = min to max
        return this
    }

    open fun build(): InvertedPalette<T> {
        return InvertedPalette<T>().apply {
            invertedMap.putAll(this@InvertedPaletteBuilder.map)
        }
    }
}