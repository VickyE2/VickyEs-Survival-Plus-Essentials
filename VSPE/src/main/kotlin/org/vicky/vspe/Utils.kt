package org.vicky.vspe

import org.vicky.platform.utils.Mirror
import org.vicky.platform.utils.Rotation
import org.vicky.platform.utils.Vec3
import org.vicky.platform.world.PlatformBlockState
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.*
import java.lang.AutoCloseable
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.stream.Collectors
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random


fun createTerrainSampler(seed: Long): CompositeNoiseLayer {
    // low-frequency "mountain" ridges (FBM) — big scale, fewer features
    val mountainBase = FBMGenerator(
        seed xor 0x9E3779B97F4AL, octaves = 5,
        amplitude = 1.0f, frequency = 0.0006f, lacunarity = 2.0f, gain = 0.5f
    )

    // mask that determines where mountains appear (very low frequency)
    val mountainMask = FBMGenerator(
        seed + 0x9E3779B97C15L, octaves = 3,
        amplitude = 1.0f, frequency = 0.00035f, lacunarity = 2.0f, gain = 0.45f
    )

    // mid-frequency hills
    val hills = FBMGenerator(
        seed + 12345, octaves = 4,
        amplitude = 0.8f, frequency = 0.0045f, lacunarity = 2.0f, gain = 0.5f
    )

    // higher-frequency small bumps / noise for surface detail
    val bumps = FBMGenerator(
        seed + 54321, octaves = 3,
        amplitude = 0.5f, frequency = 0.02f, lacunarity = 2.0f, gain = 0.5f
    )

    // a gentle base waviness to keep the overall elevation near baseline (keeps things "mostly 73")
    val gentle = FBMGenerator(
        seed + 777, octaves = 2,
        amplitude = 0.25f, frequency = 0.0015f, lacunarity = 2.0f, gain = 0.5f
    )

    // Mountain with mask: we want mountainBase to contribute only where mountainMask is high.
    val maskedMountains = MaskedNoiseSampler(mountainBase, mountainMask, maskExponent = 4.0)

    // weights chosen so bumps/hills dominate small variation, maskedMountains produce rare large peaks.
    // CompositeNoiseLayer expects weights (we'll treat them as relative; you may want to normalize depending on behavior)
    val layers = listOf(
        pairs(bumps, 0.20),         // small surface bumps
        pairs(hills, 0.45),         // rolling hills
        pairs(gentle, 0.10),        // keeps baseline from drifting too much
        pairs(maskedMountains, 0.25)// occasional big mountains
    )

    // note: CompositeNoiseLayer.sample currently sums ((layer.sample + 1)/2) * weight
    // so weights should preferably sum to around 1.0 (not strictly required)
    return CompositeNoiseLayer(layers, seed)
}

// tiny helper to avoid Kotlin's Pair constructor noise
private fun pairs(s: NoiseSampler, w: Double) = Pair(s, w)

/**
 * Convert sampler output to a world Y coordinate.
 * - baseline: ~73
 * - max: ~167
 * You can tweak baseY and amplitudeY to taste.
 */
fun sampleHeight(sampler: NoiseSampler, x: Double, z: Double, baseY: Int = 73, maxY: Int = 167): Int {
    // many samplers return values roughly in [-1,1] or 0..1 depending on composition. CompositeNoiseLayer
    // in the user's code returns weighted ((layer.sample+1)/2)*weight; so expect ~[0, totalWeight].
    val raw = sampler.sample(x, z)
    val v = normalize01(raw) // clamp to [0,1]
    val height = baseY + (v * (maxY - baseY)).roundToInt()
    return height
}

/** Normalize and clamp a potentially unbounded noise value to [0,1]. */
fun normalize01(value: Double): Double {
    // If your composite weights sum to 1.0 this is already ideally in [0,1].
    // We still clamp and apply a gentle easing so extremes feel less harsh.
    val clamped = value.coerceIn(0.0, 1.0)
    // optional curve: bias towards lower elevations a bit (comment out if you don't want bias)
    // return clamped.pow(0.95)
    return clamped
}

fun distance(
    temp1: Double,
    humid1: Double,
    elev1: Double,
    temp2: Double,
    humid2: Double,
    elev2: Double,
    tempWeight: Double = 1.0,
    humidWeight: Double = 1.0,
    elevWeight: Double = 1.0
): Double {
    val dt = (temp1 - temp2) * tempWeight
    val dh = (humid1 - humid2) * humidWeight
    val de = (elev1 - elev2) * elevWeight

    return sqrt(dt * dt + dh * dh + de * de)
}

enum class StructureTag {
    RUINS, DUNGEON, VILLAGE, HOUSE, OCEAN, SKY, NETHER, FROZEN, ANCIENT, TREELIKE, EMPTY
}

typealias PaletteFunction = (
    pos: Vec3,
    height: Int,
    distanceFromCenter: Int,
    random: RandomSource
) -> PlatformBlockState<*>

enum class BiomeCategory {
    COAST, COLD_COAST, WARM_COAST, WARM_OCEAN, WARM_DEEP_OCEAN, LUKEWARM_OCEAN, LUKEWARM_DEEP_OCEAN, COLD_OCEAN, COLD_DEEP_OCEAN, FROZEN_OCEAN, FROZEN_DEEP_OCEAN, SNOWY_BEACH, LUSH_CAVES, PLAINS, FOREST, DESERT, COLD_DESERT, MOUNTAIN, COLD_MOUNTAIN, OCEAN, DEEP_OCEAN, RIVER, SWAMP, COLD_SWAMP, TAIGA, SAVANNA, TUNDRA, NETHER, END, MESA, ICY, JUNGLE, RAINFOREST, WETLAND;
}

fun <T> List<T>.weightedRandomOrNullI(weightProvider: (T) -> Int): T? {
    if (isEmpty()) return null
    val totalWeight = sumOf(weightProvider)
    if (totalWeight <= 0) return null

    val r = Random.nextInt(totalWeight)
    var cumulative = 0
    for (item in this) {
        cumulative += weightProvider(item)
        if (r < cumulative) return item
    }
    return null // shouldn't happen unless totalWeight is messed up
}

fun <T> Iterable<T>.weightedRandomOrNullD(
    random: Random = Random.Default,
    weightSelector: (T) -> Double
): T? {
    val list = this.toList()
    if (list.isEmpty()) return null

    val totalWeight = list.sumOf(weightSelector)
    if (totalWeight <= 0.0) return null

    var r = random.nextDouble() * totalWeight
    for (item in list) {
        r -= weightSelector(item)
        if (r <= 0.0) {
            return item
        }
    }
    return list.last() // fallback if rounding quirks happen
}

fun xorNumbers(a: Number, b: Number): Number {
    return when {
        a is Long && b is Long -> a xor b
        a is Int && b is Int -> a xor b
        a is Short && b is Short -> (a.toInt() xor b.toInt()).toShort()
        a is Byte && b is Byte -> (a.toInt() xor b.toInt()).toByte()
        else -> throw IllegalArgumentException("Unsupported number types: ${a::class}, ${b::class}")
    }
}

fun subSeed(seed: Long, salt: Long): Long {
    var x = seed xor salt
    x = (x xor (x ushr 33)) * 0xff23d7ed558ccdL
    x = (x xor (x ushr 33)) * 0xc4ceba85ec53L
    x = x xor (x ushr 33)
    return x
}

fun <T> List<T>.contains(transform: (T) -> Boolean) : Boolean {
    for (e in this) {
        if (transform(e)) {
            return true
        }
    }
    return false
}

enum class PrecipitationType {
    NONE, RAIN, SNOW
}

fun Vec3.rotate(rotation: Rotation, origin: Vec3): Vec3 {
    val x = this.x - origin.x
    val z = this.z - origin.z

    return when (rotation) {
        Rotation.NONE -> this
        Rotation.CLOCKWISE_90 -> Vec3(origin.x - z, this.y, origin.z + x)
        Rotation.CLOCKWISE_180 -> Vec3(origin.x - x, this.y, origin.z - z)
        Rotation.COUNTERCLOCKWISE_90 -> Vec3(origin.x + z, this.y, origin.z - x)
    }
}



fun getAllZipFiles(directoryPath: String): List<Path> {
    val dirPath:Path = Paths.get(directoryPath)
    if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
        throw IllegalArgumentException("Invalid directory path: $directoryPath")
    }
    return Files.walk(dirPath).filter { path -> path.toString().lowercase().endsWith(".zip") }.collect(Collectors.toList())
}

fun Vec3.mirror(mirror: Mirror, origin: Vec3): Vec3 {
    return when (mirror) {
        Mirror.NONE -> this
        Mirror.FRONT_BACK -> Vec3(this.x, this.y, origin.z * 2 - this.z)
        Mirror.LEFT_RIGHT -> Vec3(origin.x * 2 - this.x, this.y, this.z)
    }
}

fun BlockVec3i.toVec3(): Vec3 {
    return Vec3(this.x.toDouble(), this.y.toDouble(), this.z.toDouble())
}

internal fun <T: Vec3> T.offset(
    x: Integer,
    y: Integer,
    z: Integer
): Vec3 {
    return Vec3(this.x + x.toDouble(), this.y + y.toDouble(), this.z + z.toDouble())
}

internal fun <T: Vec3> T.offset(
    x: Int,
    y: Int,
    z: Int
): Vec3 {
    return Vec3(this.x + x.toDouble(), this.y + y.toDouble(), this.z + z.toDouble())
}

data class BlockVec3i(val x: Int, val y: Int, val z: Int) {

    constructor(x: Integer, y: Integer, z: Integer) : this(x.toInt(), y.toInt(), z.toInt())

    operator fun plus(other: BlockVec3i): BlockVec3i =
        BlockVec3i(x + other.x, y + other.y, z + other.z)

    operator fun minus(other: BlockVec3i): BlockVec3i =
        BlockVec3i(x - other.x, y - other.y, z - other.z)

    operator fun times(scalar: Int): BlockVec3i =
        BlockVec3i(x * scalar, y * scalar, z * scalar)

    operator fun div(scalar: Int): BlockVec3i =
        BlockVec3i(
            (x.toDouble() / scalar).roundToInt(),
            (y.toDouble() / scalar).roundToInt(),
            (z.toDouble() / scalar).roundToInt()
        )

    fun toChunkPos(): Pair<Int, Int> =
        x.shr(4) to z.shr(4)

    fun rotate(rotation: Rotation): BlockVec3i =
        when (rotation) {
            Rotation.NONE -> this
            Rotation.CLOCKWISE_90 -> BlockVec3i(-z, y, x)
            Rotation.CLOCKWISE_180 -> BlockVec3i(-x, y, -z)
            Rotation.COUNTERCLOCKWISE_90 -> BlockVec3i(z, y, -x)
        }

    fun mirror(mirror: Mirror): BlockVec3i =
        when (mirror) {
            Mirror.NONE -> this
            Mirror.LEFT_RIGHT -> BlockVec3i(-x, y, z)
            Mirror.FRONT_BACK -> BlockVec3i(x, y, -z)
        }

    override fun toString(): String = "($x, $y, $z)"
}

enum class Direction(val dx: Int, val dz: Int) {
    NORTH(0, -1),
    SOUTH(0, 1),
    EAST(1, 0),
    WEST(-1, 0),
    NORTHEAST(1, -1),
    NORTHWEST(-1, -1),
    SOUTHEAST(1, 1),
    SOUTHWEST(-1, 1)
}

class KeyedLock<K> @JvmOverloads constructor(private val fair: Boolean = false) {
    class LockEntry(fair: Boolean) {
        val lock: ReentrantLock = ReentrantLock(fair)
        val refs: AtomicInteger = AtomicInteger(1)
    }

    internal val map = ConcurrentHashMap<K, LockEntry>()

    /**
     * Acquire the lock for the given key. Blocks until the lock is obtained.
     * Must call close() on the returned LockToken (try-with-resources).
     */
    @Synchronized
    fun acquire(key: K): LockToken<K> {
        // create or bump ref count atomically
        val entry: LockEntry = map.compute(key) { k: K, existing: LockEntry? ->
            if (existing == null) {
                return@compute LockEntry(fair)
            } else {
                existing.refs.incrementAndGet()
                return@compute existing
            }
        }!!

        // now obtain the lock (this will block if another thread holds it)
        entry.lock.lock()
        return LockToken<K>(key, entry, map)
    }

    /**
     * Token that represents ownership. AutoCloseable so it works with try-with-resources.
     */
    class LockToken<K> internal constructor(key: K, entry: LockEntry, val map: ConcurrentHashMap<K, LockEntry>) :
        AutoCloseable {
        private val key: K = Objects.requireNonNull(key, "key")
        private val entry: LockEntry = Objects.requireNonNull(entry, "entry")!!
        private var closed = false

        /**
         * Release the lock and decrement ref count. If ref count drops to zero the entry
         * is removed from the internal map (using remove(key, entry) to avoid races).
         */
        override fun close() {
            if (closed) return
            try {
                entry.lock.unlock()
            } finally {
                val remaining = entry.refs.decrementAndGet()
                if (remaining <= 0) {
                    // remove only if the same entry is still present (avoid removing newer entry)
                    map.remove(key, entry)
                }
                closed = true
            }
        }
    }
}

class KeyedRWLock<K> {
    private val map = ConcurrentHashMap<K, ReentrantReadWriteLock>()

    @Synchronized
    fun <T> withRead(
        key: K,
        block: () -> T,
        ifLocked: (() -> T)? = null
    ): T {
        val lock = map.computeIfAbsent(key) { ReentrantReadWriteLock(true) }
        val readLock = lock.readLock()

        // Try non-blocking acquire
        return if (readLock.tryLock()) {
            try {
                block()
            } finally {
                readLock.unlock()
                cleanupIfUnused(key, lock)
            }
        } else {
            // Fallback: either call alternate or throw
            ifLocked?.invoke()
                ?: throw IllegalStateException("Lock for key $key is currently held")
        }
    }

    @Synchronized
    fun <T> withWrite(
        key: K,
        block: () -> T
    ): T {
        val lock = map.computeIfAbsent(key) { ReentrantReadWriteLock(true) }
        val writeLock = lock.writeLock()

        return try {
            writeLock.lock()
            block()
        } finally {
            writeLock.unlock()
            cleanupIfUnused(key, lock)
        }
    }

    // Optional cleanup if no threads are holding this lock
    private fun cleanupIfUnused(key: K, lock: ReentrantReadWriteLock) {
        if (!lock.isWriteLocked && lock.readLockCount == 0) {
            map.remove(key, lock)
        }
    }
}

class SingleFlightCache<K, V>(
    private val cache: (K) -> V?
) {
    private val inflight = ConcurrentHashMap<K, CompletableFuture<V>>()
    private val threadGenerating = ThreadLocal.withInitial { mutableSetOf<K>() }

    /**
     * Returns a cached value or runs [generator] exactly once for a given key.
     * This implementation avoids holding external locks while waiting and handles
     * re-entrant calls from the same thread (warn: re-entrancy usually indicates a logic issue).
     */
    fun getOrGenerate(key: K, generator: (K) -> V): V {
        // fast path: already cached
        cache(key)?.let { return it }

        // If this thread is already generating the same key -> reentrant call.
        // Avoid joining our own future (deadlock). We delegate directly to generator.
        val generatingSet = threadGenerating.get()
        if (generatingSet.contains(key)) {
            // Re-entrant: don't try to wait for inflight future (we created it).
            // Directly call generator. This may still recurse or be a logic bug in your generator.
            return generator(key)
        }

        // Try to create a promise; if someone else already created one, we'll wait on it.
        val promise = CompletableFuture<V>()
        val existing = inflight.putIfAbsent(key, promise)

        if (existing == null) {
            // we "own" generation for this key
            generatingSet.add(key)
            try {
                // double-check cache (race)
                cache(key)?.let {
                    promise.complete(it)
                    return it
                }

                val generated = generator(key)
                promise.complete(generated)
                return generated
            } catch (t: Throwable) {
                promise.completeExceptionally(t)
                throw t
            } finally {
                generatingSet.remove(key)
                inflight.remove(key)
            }
        } else {
            // someone else is generating -> wait for their result (no locks held)
            return existing.join()
        }
    }
}


fun <K, V> Map<K, V>.toConcurrentMap(): ConcurrentHashMap<K, V> {
    val map = ConcurrentHashMap<K, V>()
    this.forEach { (k, v) -> map[k] = v }
    return map
}