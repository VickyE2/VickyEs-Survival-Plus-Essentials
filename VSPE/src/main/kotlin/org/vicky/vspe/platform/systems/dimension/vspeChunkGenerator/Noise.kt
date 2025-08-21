package org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator

import de.articdive.jnoise.JNoise
import de.articdive.jnoise.api.NoiseBuilder
import de.articdive.jnoise.api.NoiseGenerator
import de.articdive.jnoise.api.NoiseResult
import de.articdive.jnoise.api.module.NoiseModule
import de.articdive.jnoise.api.module.NoiseModuleBuilder
import de.articdive.jnoise.fractal_functions.FractalFunction
import de.articdive.jnoise.interpolation.Interpolation
import de.articdive.jnoise.modules.octavation.OctavationModule
import org.vicky.vspe.noise.FastNoiseLite
import kotlin.math.*

fun unsignedShiftRight(x: Long, n: Int): Long {
    return x.toULong().shr(n).toLong()
}

interface NoiseSampler {
    fun getSeed(): Long
    fun sample(x: Double, z: Double): Double
    fun sample3D(x: Double, y: Double, z: Double): Double
}

/**
 * Wrapper that precomputes a coarse grid of noise samples for a rectangular region (with padding),
 * and provides fast bilinear (2D) / trilinear (3D) interpolation reads.
 *
 * Usage pattern (recommended for chunk generation):
 * 1. val pg = PaddedGridNoise(baseNoise)
 * 2. pg.prepare2D(minX, minZ, width, depth, cellSpacing, padding, freqScale)
 * 3. call pg.sample(x.toDouble(), z.toDouble()) for many (x,z) within that region (fast)
 *
 * For 3D cave masks:
 * 1. pg.prepare3D(minX,minY,minZ,width,height,depth,cellSpacing, padding, freqScale, ySpacing)
 * 2. call pg.sample3D(x,y,z)
 */
class PaddedGridNoise(private val baseNoise: NoiseSampler, /* your underlying accurate noise */) : NoiseSampler {

    // region parameters (2D)
    private var prepared2D = false
    private var nodeOriginX = 0.0        // world X of node [0]
    private var nodeOriginZ = 0.0        // world Z of node [0]
    private var nodeCountX = 0
    private var nodeCountZ = 0
    private var cellSpacing = 1.0        // distance between nodes
    private var freqScale = 1.0

    // 2D grid: grid[nodeX][nodeZ]
    private var grid2D: Array<DoubleArray> = arrayOf()

    // 3D grid support (optional): grid3D[nodeX][nodeY][nodeZ]
    private var prepared3D = false
    private var nodeOriginY = 0.0
    private var nodeCountY = 0
    private var ySpacing = 1.0
    private var grid3D: Array<Array<DoubleArray>> = arrayOf()

    // ---- NoiseSampler API ----
    override fun getSeed(): Long = baseNoise.getSeed()

    // If a sample is requested but the region wasn't prepared, we fallback to the base noise.
    override fun sample(x: Double, z: Double): Double {
        if (!prepared2D) return baseNoise.sample(x, z)

        // compute fractional coordinates relative to node grid
        val fx = (x - nodeOriginX) / cellSpacing
        val fz = (z - nodeOriginZ) / cellSpacing

        val gx = floor(fx).toInt()
        val gz = floor(fz).toInt()
        val tx = fx - gx
        val tz = fz - gz

        // if outside prepared node index range -> fallback
        if (gx < 0 || gz < 0 || gx + 1 >= nodeCountX || gz + 1 >= nodeCountZ) {
            return baseNoise.sample(x, z)
        }

        val a = grid2D[gx][gz]
        val b = grid2D[gx + 1][gz]
        val c = grid2D[gx][gz + 1]
        val d = grid2D[gx + 1][gz + 1]

        return bilerp(a, b, c, d, tx, tz)
    }

    override fun sample3D(x: Double, y: Double, z: Double): Double {
        if (!prepared3D) return baseNoise.sample3D(x, y, z)

        val fx = (x - nodeOriginX) / cellSpacing
        val fy = (y - nodeOriginY) / ySpacing
        val fz = (z - nodeOriginZ) / cellSpacing

        val gx = floor(fx).toInt()
        val gy = floor(fy).toInt()
        val gz = floor(fz).toInt()

        val tx = fx - gx
        val ty = fy - gy
        val tz = fz - gz

        // if outside prepared range -> fallback
        if (gx < 0 || gy < 0 || gz < 0 || gx + 1 >= nodeCountX || gy + 1 >= nodeCountY || gz + 1 >= nodeCountZ) {
            return baseNoise.sample3D(x, y, z)
        }

        // fetch 8 nodes and trilinearly interpolate
        val n000 = grid3D[gx][gy][gz]
        val n100 = grid3D[gx + 1][gy][gz]
        val n010 = grid3D[gx][gy + 1][gz]
        val n110 = grid3D[gx + 1][gy + 1][gz]
        val n001 = grid3D[gx][gy][gz + 1]
        val n101 = grid3D[gx + 1][gy][gz + 1]
        val n011 = grid3D[gx][gy + 1][gz + 1]
        val n111 = grid3D[gx + 1][gy + 1][gz + 1]

        val i1 = lerp(n000, n100, tx)
        val i2 = lerp(n010, n110, tx)
        val j1 = lerp(i1, i2, ty)

        val i3 = lerp(n001, n101, tx)
        val i4 = lerp(n011, n111, tx)
        val j2 = lerp(i3, i4, ty)

        return lerp(j1, j2, tz)
    }

    // ---- helpers for preparing a region ----

    /**
     * Prepare a 2D padded node grid covering [minX..minX+width-1] x [minZ..minZ+depth-1].
     *
     * - cellSpacing: spacing between node samples in world units (e.g. 4.0 blocks).
     * - padding: number of world-blocks to pad around the region (converted to node padding internally).
     * - freqScale: multiply world coords by this before sampling baseNoise (useful if baseNoise expects scaled coords).
     */
    fun prepare2D(minX: Int, minZ: Int, width: Int, depth: Int, cellSpacing: Double, padding: Int = 0, freqScale: Double = 1.0) {
        this.cellSpacing = cellSpacing
        this.freqScale = freqScale

        // compute padded bounds in world coords
        val minWX = minX - padding
        val minWZ = minZ - padding
        val maxWX = minX + width - 1 + padding
        val maxWZ = minZ + depth - 1 + padding

        // compute node grid counts so nodes cover entire padded area (include +1 to have nodes on edges)
        val nodesX = floor((maxWX - minWX) / cellSpacing).toInt() + 1
        val nodesZ = floor((maxWZ - minWZ) / cellSpacing).toInt() + 1

        // set origin to exact node world coordinate
        nodeOriginX = minWX.toDouble()
        nodeOriginZ = minWZ.toDouble()
        nodeCountX = nodesX + 1 // ensure we have gx+1 valid (we compute +1 for safe interpolation)
        nodeCountZ = nodesZ + 1

        // allocate grid
        grid2D = Array(nodeCountX) { DoubleArray(nodeCountZ) }

        // sample baseNoise at node coordinates
        for (ix in 0 until nodeCountX) {
            val sx = nodeOriginX + ix * cellSpacing
            for (iz in 0 until nodeCountZ) {
                val sz = nodeOriginZ + iz * cellSpacing
                grid2D[ix][iz] = baseNoise.sample(sx * freqScale, sz * freqScale)
            }
        }

        prepared2D = true
    }

    /**
     * Prepare a 3D padded node grid (for caves). Y spacing can be different from X/Z spacing.
     *
     * - ySpacing: spacing between Y nodes (e.g. 4.0 blocks).
     * - height = number of world Y values (maxY-minY+1)
     */
    fun prepare3D(minX: Int, minY: Int, minZ: Int, width: Int, height: Int, depth: Int,
                  cellSpacing: Double, padding: Int = 0, freqScale: Double = 1.0, ySpacing: Double = 1.0) {
        this.cellSpacing = cellSpacing
        this.freqScale = freqScale
        this.ySpacing = ySpacing

        val minWX = minX - padding
        val minWY = minY - padding
        val minWZ = minZ - padding

        val maxWX = minX + width - 1 + padding
        val maxWY = minY + height - 1 + padding
        val maxWZ = minZ + depth - 1 + padding

        val nodesX = floor((maxWX - minWX) / cellSpacing).toInt() + 1
        val nodesY = floor((maxWY - minWY) / ySpacing).toInt() + 1
        val nodesZ = floor((maxWZ - minWZ) / cellSpacing).toInt() + 1

        nodeOriginX = minWX.toDouble()
        nodeOriginY = minWY.toDouble()
        nodeOriginZ = minWZ.toDouble()

        nodeCountX = nodesX + 1
        nodeCountY = nodesY + 1
        nodeCountZ = nodesZ + 1

        // allocate 3D grid as [nodeCountX][nodeCountY][nodeCountZ]
        grid3D = Array(nodeCountX) { ix ->
            Array(nodeCountY) { iy ->
                DoubleArray(nodeCountZ)
            }
        }

        for (ix in 0 until nodeCountX) {
            val sx = nodeOriginX + ix * cellSpacing
            for (iy in 0 until nodeCountY) {
                val sy = nodeOriginY + iy * ySpacing
                for (iz in 0 until nodeCountZ) {
                    val sz = nodeOriginZ + iz * cellSpacing
                    grid3D[ix][iy][iz] = baseNoise.sample3D(sx * freqScale, sy * freqScale, sz * freqScale)
                }
            }
        }

        prepared3D = true
    }

    // ---- math helpers ----
    private fun bilerp(a: Double, b: Double, c: Double, d: Double, tx: Double, ty: Double): Double {
        val i1 = a * (1.0 - tx) + b * tx
        val i2 = c * (1.0 - tx) + d * tx
        return i1 * (1.0 - ty) + i2 * ty
    }

    private fun lerp(a: Double, b: Double, t: Double): Double = a * (1.0 - t) + b * t
}

class CheckerboardNoise(
    private val seed: Long = 0L,
    private val tileSize: Double = 8.0,
    private val scale: Double = 1.0,
    private val invert: Boolean = false,
    private val smoothness: Double = 0.0,
    private val warpSampler: NoiseSampler? = null,    // optional domain warp (2D)
    private val warpAmp: Double = 0.0,
    private val warpFreq: Double = 1.0,               // frequency for warp sampler coords
    private val alignToOrigin: Boolean = true         // whether tiles align to global 0,0
)
    : NoiseSampler {

    init {
        require(tileSize > 0.0) { "tileSize must be > 0" }
        require(smoothness >= 0.0) { "smoothness must be >= 0" }
    }

    override fun getSeed(): Long = seed

    /** 2D checkerboard sample */
    override fun sample(xIn: Double, zIn: Double): Double {
        // apply domain warp if present
        var x = xIn
        var z = zIn
        if (warpSampler != null && warpAmp != 0.0) {
            val wx = warpSampler.sample(xIn * warpFreq, zIn * warpFreq)
            val wz = warpSampler.sample((xIn + 1234.5) * warpFreq, (zIn + 4321.1) * warpFreq) // offset for second channel
            x += wx * warpAmp
            z += wz * warpAmp
        }

        // optionally align to origin: this just centers tile grid at 0,0
        val originOffset = if (alignToOrigin) 0.0 else 0.0 // placeholder; keep consistent if needed

        val tx = (x - originOffset) / tileSize
        val tz = (z - originOffset) / tileSize

        val ix = floor(tx).toInt()
        val iz = floor(tz).toInt()

        val parity = ((ix + iz) and 1) == 0 // even = true, odd = false
        val base = if (parity xor invert) 1.0 else -1.0  // xor(invert) flips

        if (smoothness <= 0.0) {
            return base * scale
        }

        // smoothing: compute distance to nearest tile edge in x and z (in world units)
        val fx = tx - ix   // fractional position inside tile [0,1)
        val fz = tz - iz

        // distance to nearest vertical edge (in tile fractions) -> convert to world units
        val distToEdgeX = min(fx, 1.0 - fx) * tileSize
        val distToEdgeZ = min(fz, 1.0 - fz) * tileSize
        val minDist = min(distToEdgeX, distToEdgeZ)

        // smoothstep over smoothness width
        val w = smoothness.coerceAtMost(tileSize * 0.5) // cannot be larger than half tile
        val t = when {
            w <= 0.0 -> 1.0
            minDist >= w -> 1.0
            else -> clamp(minDist / w, 0.0, 1.0)
        }

        // blend from base to -base across edge: when t==1 => base, when t==0 => -base (opposite)
        // But we want center full base, edges transition to -base: use smoothstep on t
        val smoothT = smoothstep(0.0, 1.0, t)
        val value = lerp(-base, base, smoothT) // -base at t=0 (edge), base at t=1 (center)
        return value * scale
    }

    /** 3D sample: by default checkerboard pattern in X/Z plane ignores Y; you can incorporate Y by using y/tileSize parity as well. */
    override fun sample3D(x: Double, y: Double, z: Double): Double {
        // Option A: ignore Y -> same as 2D
        return sample(x, z)

        // Option B: include Y parity for 3D checker cubes:
        // val v2d = sample(x,z)
        // val ty = floor(y / tileSize).toInt()
        // if ((ty and 1) == 1) return -v2d else return v2d
    }

    // helpers
    private fun clamp(v: Double, lo: Double, hi: Double) = max(lo, min(hi, v))
    private fun lerp(a: Double, b: Double, t: Double) = a * (1.0 - t) + b * t

    private fun smoothstep(edge0: Double, edge1: Double, x: Double): Double {
        if (edge0 >= edge1) return clamp(x, 0.0, 1.0)
        val t = clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0)
        return t * t * (3.0 - 2.0 * t)
    }
}

class JNoiseWrapper(
    val noise: NoiseSampler
) : NoiseGenerator<DefaultedNoiseResult> {
    override fun evaluateNoise(x: Double, seed: Long): Double = noise.sample(x, 0.0)

    override fun evaluateNoise(x: Double, y: Double, seed: Long): Double = noise.sample(x, y)

    override fun evaluateNoise(x: Double, y: Double, z: Double, seed: Long): Double = noise.sample3D(x, y, z)

    override fun evaluateNoise(
        x: Double,
        y: Double,
        z: Double,
        w: Double,
        seed: Long
    ): Double = noise.sample3D(x, y, z)

    override fun evaluateNoise(x: Double): Double = noise.sample(x, 0.0)

    override fun evaluateNoise(x: Double, y: Double): Double = noise.sample(x, y)

    override fun evaluateNoise(x: Double, y: Double, z: Double): Double = noise.sample3D(x, y, z)

    override fun evaluateNoise(
        x: Double,
        y: Double,
        z: Double,
        w: Double
    ): Double = noise.sample3D(x, y, z)

    override fun evaluateNoiseResult(
        x: Double,
        seed: Long
    ): DefaultedNoiseResult = DefaultedNoiseResult(evaluateNoise(x))

    override fun evaluateNoiseResult(
        x: Double,
        y: Double,
        seed: Long
    ): DefaultedNoiseResult = DefaultedNoiseResult(evaluateNoise(x, y))

    override fun evaluateNoiseResult(
        x: Double,
        y: Double,
        z: Double,
        seed: Long
    ): DefaultedNoiseResult = DefaultedNoiseResult(evaluateNoise(x, y, z))

    override fun evaluateNoiseResult(
        x: Double,
        y: Double,
        z: Double,
        w: Double,
        seed: Long
    ): DefaultedNoiseResult  = DefaultedNoiseResult(evaluateNoise(x, y, z, w))

    override fun evaluateNoiseResult(
        x: Double
    ): DefaultedNoiseResult = DefaultedNoiseResult(evaluateNoise(x))

    override fun evaluateNoiseResult(
        x: Double,
        y: Double
    ): DefaultedNoiseResult = DefaultedNoiseResult(evaluateNoise(x, y))

    override fun evaluateNoiseResult(
        x: Double,
        y: Double,
        z: Double
    ): DefaultedNoiseResult = DefaultedNoiseResult(evaluateNoise(x, y, z))

    override fun evaluateNoiseResult(
        x: Double,
        y: Double,
        z: Double,
        w: Double
    ): DefaultedNoiseResult  = DefaultedNoiseResult(evaluateNoise(x, y, z, w))

    override fun getSeed(): Long = noise.getSeed()
}

class JNoiseNoiseSampler(
    val noise: NoiseGenerator<DefaultedNoiseResult>
): NoiseSampler {
    override fun getSeed(): Long = noise.seed
    override fun sample(x: Double, z: Double): Double = noise.evaluateNoise(x, z)
    override fun sample3D(x: Double, y: Double, z: Double): Double = noise.evaluateNoise(x, y, z)
}

open class CompositeNoiseLayer(
    private val layers: List<Pair<NoiseSampler, Double>>,
    private val seed: Long = 0L
): NoiseSampler
{
    object EMPTY : CompositeNoiseLayer(listOf())

    fun getLayers(): List<Pair<NoiseSampler, Double>> = layers
    override fun getSeed(): Long = seed
    override fun sample(x: Double, z: Double): Double {
        var value = 0.0
        for ((layer, weight) in layers) {
            value += ((layer.sample(x, z) + 1) / 2.0) * weight
        }
        return value
    }

    override fun sample3D(x: Double, y: Double, z: Double): Double {
        var total = 0.0
        var totalWeight = 0.0
        for ((layer, weight) in layers) {
            // layer.setSeed(seed)
            total += layer.sample3D(x, y, z) * weight
            totalWeight += weight
        }
        return total / totalWeight
    }
}

class FBMGenerator(seed: Long, octaves: Int = 4, amplitude: Float = 1.0f, frequency: Float = 0.01f, lacunarity: Float = 2.0f, gain: Float = 0.5f): NoiseSampler {
    private val noise = FastNoiseLite(seed.toInt()).apply {
        SetNoiseType(FastNoiseLite.NoiseType.Value)
        SetFractalType(FastNoiseLite.FractalType.FBm)
        SetFractalOctaves(octaves)
        SetFractalLacunarity(lacunarity)
        SetFractalGain(gain)
        SetDomainWarpAmp(amplitude)
        SetFrequency(frequency)
    }

    override fun getSeed(): Long {
        return noise.mSeed.toLong()
    }
    override fun sample(x: Double, z: Double): Double = noise.GetNoise(x.toFloat(), z.toFloat()).toDouble()
    override fun sample3D(x: Double, y: Double, z: Double): Double = noise.GetNoise(x.toFloat(), y.toFloat(), z.toFloat()).toDouble()
}

/**
 * Factory to create JNoise-based samplers with different configurations.
 */
object NoiseSamplerFactory {
    enum class Type {
        PERLIN,
        OPEN_SIMPLEX,
        VALUE,
        CELLULAR,
        WHITE,
        VORONOI
    }

    @JvmOverloads
    fun create(
        type: Type,
        extraOps: (NoiseBuilder<*, *>) -> NoiseBuilder<*, *> = { it },
        seed: Long = 0L,
        frequency: Double = 0.01,
        octaves: Int = 1,
        gain: Double = 0.5,
        lacunarity: Double = 2.0,
        fbm: Boolean = true,
        interpolation: Interpolation = Interpolation.LINEAR,
        ridged: Boolean = false
    ): NoiseGenerator<DefaultedNoiseResult> {
        var baseGenerator: NoiseBuilder<*, *> = when (type) {
            Type.PERLIN -> JNoise.newBuilder().perlin().setSeed(seed).setFrequency(frequency).setInterpolation(interpolation)
            Type.OPEN_SIMPLEX -> JNoise.newBuilder().fastSimplex().setSeed(seed).setFrequency(frequency)
            Type.VALUE -> JNoise.newBuilder().value().setSeed(seed).setFrequency(frequency).setInterpolation(interpolation)
            Type.CELLULAR -> JNoise.newBuilder().worley().setSeed(seed).setFrequency(frequency)
            Type.WHITE -> JNoise.newBuilder().white().setSeed(seed).setFrequency(frequency)
            Type.VORONOI -> VoronoiBuilder().frequency(frequency).seed(seed).jitter(lacunarity).normalized(true)
        }

        if (fbm) {
            baseGenerator.addModule(
                OctavationModule.newBuilder()
                    .setOctaves(octaves)
                    .setPersistence(gain)
                    .setLacunarity(lacunarity)
                    .setFractalFunction(FractalFunction.FBM)
                    .build()
            )
        } else if (ridged) {
            baseGenerator.addModule(
                OctavationModule.newBuilder()
                    .setOctaves(octaves)
                    .setPersistence(gain)
                    .setLacunarity(lacunarity)
                    .setFractalFunction(FractalFunction.RIDGED_MULTI)
                    .build()
            )
        }

        baseGenerator = extraOps(baseGenerator)
        val noise = baseGenerator.build()

        return object : NoiseGenerator<DefaultedNoiseResult> {
            override fun getSeed(): Long = seed
            override fun evaluateNoise(p0: Double): Double = noise.getNoise(p0)
            override fun evaluateNoise(p0: Double, p1: Double): Double = noise.getNoise(p0, p1)
            override fun evaluateNoise(p0: Double, p1: Double, p2: Double): Double = noise.getNoise(p0, p2, p2)
            override fun evaluateNoise(
                p0: Double,
                p1: Double,
                p2: Double,
                p3: Double
            ): Double = noise.getNoise(p0, p1, p2, p3)

            override fun evaluateNoiseResult(
                x: Double,
                seed: Long
            ): DefaultedNoiseResult = DefaultedNoiseResult(evaluateNoise(x))

            override fun evaluateNoiseResult(
                x: Double,
                y: Double,
                seed: Long
            ): DefaultedNoiseResult = DefaultedNoiseResult(evaluateNoise(x, y))

            override fun evaluateNoiseResult(
                x: Double,
                y: Double,
                z: Double,
                seed: Long
            ): DefaultedNoiseResult = DefaultedNoiseResult(evaluateNoise(x, y, z))

            override fun evaluateNoiseResult(
                x: Double,
                y: Double,
                z: Double,
                w: Double,
                seed: Long
            ): DefaultedNoiseResult = DefaultedNoiseResult(evaluateNoise(x, y, z, w))

            override fun evaluateNoiseResult(x: Double): DefaultedNoiseResult = DefaultedNoiseResult(evaluateNoise(x))

            override fun evaluateNoiseResult(
                x: Double,
                y: Double
            ): DefaultedNoiseResult = DefaultedNoiseResult(evaluateNoise(x, y))

            override fun evaluateNoiseResult(
                x: Double,
                y: Double,
                z: Double
            ): DefaultedNoiseResult = DefaultedNoiseResult(evaluateNoise(x, y, z))

            override fun evaluateNoiseResult(
                x: Double,
                y: Double,
                z: Double,
                w: Double
            ): DefaultedNoiseResult = DefaultedNoiseResult(evaluateNoise(x, y, z, w))

            override fun evaluateNoise(p0: Double, p1: Long): Double = noise.getNoise(p0)
            override fun evaluateNoise(p0: Double, p1: Double, p2: Long): Double = noise.getNoise(p0, p1)
            override fun evaluateNoise(
                p0: Double,
                p1: Double,
                p2: Double,
                p3: Long
            ): Double = noise.getNoise(p0, p1, p2)
            override fun evaluateNoise(
                p0: Double,
                p1: Double,
                p2: Double,
                p3: Double,
                p4: Long
            ): Double = noise.getNoise(p0, p1, p2, p3)
        }
    }
}

/**
 * Small wrapper to produce occasional mountains via a mask.
 */
class MaskedNoiseSampler(
    private val source: NoiseSampler,
    private val mask: NoiseSampler,
    private val maskExponent: Double = 3.0
) : NoiseSampler {
    override fun getSeed(): Long = source.getSeed()
    override fun sample(x: Double, z: Double): Double {
        val m = (mask.sample(x, z) + 1.0) / 2.0 // [-1..1] -> [0..1]
        val s = source.sample(x, z)
        return s * m.pow(maskExponent)
    }

    override fun sample3D(x: Double, y: Double, z: Double): Double {
        val m = (mask.sample3D(x, y, z) + 1.0) / 2.0
        val s = source.sample3D(x, y, z)
        return s * m.pow(maskExponent)
    }
}

/**
 * Simple transform to make FBM ridged-like (for sharper mountains).
 * This doesn't replace a true ridged multifractal, but gives sharper peaks.
 */
class RidgedWrapper(private val inner: NoiseSampler) : NoiseSampler {
    override fun getSeed(): Long = inner.getSeed()
    override fun sample(x: Double, z: Double): Double {
        val v = inner.sample(x, z)
        // transform: [-1..1] -> ridge effect [0..1] with sharper peaks
        return 1.0 - abs(v) // result in [0..1], we remap to [-1..1] below when used
    }

    override fun sample3D(x: Double, y: Double, z: Double): Double {
        val v = inner.sample3D(x, y, z)
        return 1.0 - abs(v)
    }
}

/**
 * Maps a NoiseSampler (expected roughly in [-1..1] or 0..1 depending on composition)
 * into a world Y integer. Client may also directly use the NoiseSampler produced by
 * buildSampler(...) if they want more control.
 */
class HeightMapper(private val sampler: NoiseSampler, private val baseY: Int, private val maxY: Int) {
    fun sampleHeight(x: Double, z: Double): Int {
        val raw = sampler.sample(x, z)
        // try to normalize to 0..1 — account for possible 0..1 outputs too
        val v = when {
            raw <= -1.0 -> 0.0
            raw >= 1.0 -> 1.0
            else -> (raw + 1.0) / 2.0 // [-1,1] -> [0,1]
        }
        val clamped = v.coerceIn(0.0, 1.0)
        return baseY + (clamped * (maxY - baseY)).roundToInt()
    }
}

/**
 * Builder for a modular terrain sampler. Java-friendly with defaults via @JvmOverloads.
 *
 * Example (Kotlin):
 * val sampler = TerrainSamplerBuilder(seed = 42L)
 *      .setBaseHeight(73)
 *      .setMaxHeight(167)
 *      .setMountaininess(0.6)
 *      .setHilliness(0.45)
 *      .buildSampler()
 *
 * Example (Java):
 * NoiseSampler sampler = new TerrainSamplerBuilder(42L).setBaseHeight(73).buildSampler();
 */
class TerrainSamplerBuilder @JvmOverloads constructor(private val seed: Long = 0L) {
    // tunables, 0..1 weights (not strict, builder normalizes internally)
    private var mountaininess: Double = 0.4   // strength of mountains
    private var hilliness: Double = 0.45     // strength of rolling hills
    private var bumpiness: Double = 0.25     // small surface noise
    private var mountainRarity: Double = 0.12 // 0..1, lower -> rarer mountains (mask exponent invert)
    private var mountainSharpness: Double = 1.6 // higher -> sharper mountains
    private var useRidgedMountains: Boolean = true

    // height mapping
    private var baseHeight: Int = 73
    private var maxHeight: Int = 167

    // frequency / scale multipliers (you can tweak for different world scales)
    private var mountainFrequency: Double = 0.0006
    private var mountainMaskFrequency: Double = 0.00035
    private var hillFrequency: Double = 0.0045
    private var bumpFrequency: Double = 0.02
    private var gentleFrequency: Double = 0.0015
    private var gentleWeight: Double = 0.08

    // extra: allow custom layers added by the user
    private val customLayers: MutableList<Pair<NoiseSampler, Double>> = ArrayList()

    // ---------- setters (fluent) ----------
    fun setMountaininess(v: Double) = apply { mountaininess = v.coerceIn(0.0, 2.0) } // allow >1 if wanted
    fun setHilliness(v: Double) = apply { hilliness = v.coerceIn(0.0, 2.0) }
    fun setBumpiness(v: Double) = apply { bumpiness = v.coerceIn(0.0, 2.0) }
    fun setMountainRarity(v: Double) = apply { mountainRarity = v.coerceIn(0.0, 1.0) }
    fun setMountainSharpness(v: Double) = apply { mountainSharpness = v.coerceIn(0.1, 8.0) }
    fun setUseRidgedMountains(flag: Boolean) = apply { useRidgedMountains = flag }

    fun setBaseHeight(h: Int) = apply { baseHeight = h }
    fun setMaxHeight(h: Int) = apply { maxHeight = h }

    fun setMountainFrequency(f: Double) = apply { mountainFrequency = f }
    fun setMountainMaskFrequency(f: Double) = apply { mountainMaskFrequency = f }
    fun setHillFrequency(f: Double) = apply { hillFrequency = f }
    fun setBumpFrequency(f: Double) = apply { bumpFrequency = f }
    fun setGentleFrequency(f: Double) = apply { gentleFrequency = f }
    fun setGentleWeight(w: Double) = apply { gentleWeight = w }

    fun addCustomLayer(sampler: NoiseSampler, weight: Double) = apply {
        customLayers.add(Pair(sampler, weight))
    }

    // ---------- build methods ----------
    /** Build the CompositeNoiseLayer (a NoiseSampler) */
    @JvmOverloads
    fun buildSampler(normalizeWeights: Boolean = true): CompositeNoiseLayer {
        // create FBM samplers with different seeds
        val bumps = FBMGenerator(
            (seed + 0xC0FFEE) and 0xffffffffL,
            octaves = 3,
            amplitude = 0.5f,
            frequency = bumpFrequency.toFloat(),
            lacunarity = 2.0f,
            gain = 0.5f
        )
        val hills = FBMGenerator(
            (seed + 0xBEEF) and 0xffffffffL,
            octaves = 4,
            amplitude = 0.8f,
            frequency = hillFrequency.toFloat(),
            lacunarity = 2.0f,
            gain = 0.5f
        )
        val gentle = FBMGenerator(
            (seed + 0xF00D) and 0xffffffffL,
            octaves = 2,
            amplitude = 0.25f,
            frequency = gentleFrequency.toFloat(),
            lacunarity = 2.0f,
            gain = 0.5f
        )

        val mountainBaseFBM = FBMGenerator(
            (seed + 0xDEADBEEFL) and 0xffffffffL,
            octaves = 5,
            amplitude = 1.0f,
            frequency = mountainFrequency.toFloat(),
            lacunarity = 2.0f,
            gain = 0.5f
        )
        val mountainMaskFBM = FBMGenerator(
            (seed + 0xFEED) and 0xffffffffL,
            octaves = 3,
            amplitude = 1.0f,
            frequency = mountainMaskFrequency.toFloat(),
            lacunarity = 2.0f,
            gain = 0.45f
        )

        val mountainSource: NoiseSampler = if (useRidgedMountains) RidgedWrapper(mountainBaseFBM) else mountainBaseFBM
        val maskedMountains = MaskedNoiseSampler(
            mountainSource,
            mountainMaskFBM,
            maskExponent = (1.0 / (mountainRarity.coerceAtLeast(1e-6))).coerceAtMost(8.0)
        )

        // assemble layers and weights
        val layers = ArrayList<Pair<NoiseSampler, Double>>()

        // small bumps -> high frequency, low weight
        layers.add(Pair(bumps, bumpiness))

        // hills -> mid frequency, medium weight
        layers.add(Pair(hills, hilliness))

        // gentle base -> keeps baseline around baseHeight
        layers.add(Pair(gentle, gentleWeight))

        // mountains -> masked + mountaininess + sharpness multiplier
        layers.add(Pair(maskedMountains, mountaininess * mountainSharpness))

        // append user-provided custom layers
        layers.addAll(customLayers)

        // optionally normalize weights to sum to 1.0 (CompositeNoiseLayer expects weights; its sample formula uses ((layer+1)/2)*weight)
        if (normalizeWeights) {
            val total = layers.sumOf { it.second }.coerceAtLeast(1e-12)
            val normalized = layers.map { Pair(it.first, it.second / total) }
            return CompositeNoiseLayer(normalized, seed)
        }

        return CompositeNoiseLayer(layers, seed)
    }

    /**
     * Build a HeightMapper which wraps the produced sampler and maps to ints.
     * @param normalizeSamplerWeights true->normalize layer weights before mapping
     */
    @JvmOverloads
    fun buildHeightMapper(normalizeSamplerWeights: Boolean = true): HeightMapper {
        val sampler = buildSampler(normalizeSamplerWeights)
        return HeightMapper(sampler, baseHeight, maxHeight)
    }
}

class DefaultedNoiseResult(val result: Double): NoiseResult {
    override fun getPureValue(): Double = result
}

class MultiplicationModule(
    noiseGenerator: NoiseGenerator<*>,
    var secondaryNoise: NoiseGenerator<*>
) : NoiseModule(noiseGenerator)
{

    override fun apply1D(noise: Double, x: Double): Double {
        return noise * secondaryNoise.evaluateNoise(x)
    }

    override fun apply1D(noiseResult: NoiseResult, x: Double): NoiseResult {
        val secondary = secondaryNoise.evaluateNoise(x)
        return DefaultedNoiseResult(secondary * noiseResult.pureValue)
    }

    override fun apply2D(noise: Double, x: Double, y: Double): Double {
        return noise * secondaryNoise.evaluateNoise(x, y)
    }

    override fun apply2D(noiseResult: NoiseResult, x: Double, y: Double): NoiseResult {
        val secondary = secondaryNoise.evaluateNoise(x, y)
        return DefaultedNoiseResult(secondary * noiseResult.pureValue)
    }

    override fun apply3D(noise: Double, x: Double, y: Double, z: Double): Double {
        return noise * secondaryNoise.evaluateNoise(x, y, z)
    }

    override fun apply3D(noiseResult: NoiseResult, x: Double, y: Double, z: Double): NoiseResult {
        val secondary = secondaryNoise.evaluateNoise(x, y, z)
        return DefaultedNoiseResult(secondary * noiseResult.pureValue)
    }

    override fun apply4D(noise: Double, x: Double, y: Double, z: Double, w: Double): Double {
        return noise * secondaryNoise.evaluateNoise(x, y, z, w)
    }

    override fun apply4D(noiseResult: NoiseResult, x: Double, y: Double, z: Double, w: Double): NoiseResult {
        val secondary = secondaryNoise.evaluateNoise(x, y, z, w)
        return DefaultedNoiseResult(secondary * noiseResult.pureValue)
    }
}

class MultiplicationModuleBuilder private constructor(): NoiseModuleBuilder<MultiplicationModuleBuilder>
{
    companion object {
        fun newBuilder(): MultiplicationModuleBuilder = MultiplicationModuleBuilder()
    }

    var secondary: NoiseGenerator<*>? = null

    fun withSecondary(secondaryNoise: NoiseGenerator<*>?) : MultiplicationModuleBuilder {
        if (secondaryNoise == null) error("Secondary noise cannot be null")
        secondary = secondaryNoise
        return this
    }

    override fun apply(t: NoiseGenerator<*>): NoiseModule {
        secondary?.let { return MultiplicationModule(t, it) } ?: error("Secondary noise cannot be null")
    }
}

class RadialFalloffModule(
    noiseGenerator: NoiseGenerator<*>,
    var centerX: Double,
    var centerY: Double? = null,
    var centerZ: Double,
    var maxDistance: Double
) : NoiseModule(noiseGenerator) {

    private fun falloffFactor(distance: Double): Double {
        val normalized = (distance / maxDistance).coerceIn(0.0, 1.0)
        return 1.0 - normalized // Linear falloff (can be squared for smoother)
    }

    // ---- 1D ----
    override fun apply1D(noise: Double, x: Double): Double {
        val distance = abs(x - centerX)
        return noise * falloffFactor(distance)
    }

    override fun apply1D(noiseResult: NoiseResult, x: Double): NoiseResult {
        val distance = abs(x - centerX)
        return DefaultedNoiseResult(noiseResult.pureValue * falloffFactor(distance))
    }

    // ---- 2D ----
    override fun apply2D(noise: Double, x: Double, y: Double): Double {
        val dx = x - centerX
        val dz = y - centerZ
        return noise * falloffFactor(sqrt(dx * dx + dz * dz))
    }

    override fun apply2D(noiseResult: NoiseResult, x: Double, y: Double): NoiseResult {
        val dx = x - centerX
        val dz = y - centerZ
        return DefaultedNoiseResult(noiseResult.pureValue * falloffFactor(sqrt(dx * dx + dz * dz)))
    }

    // ---- 3D ----
    override fun apply3D(noise: Double, x: Double, y: Double, z: Double): Double {
        val dy = centerY ?: 0.0
        val dx = x - centerX
        val dyOff = y - dy
        val dz = z - centerZ
        return noise * falloffFactor(sqrt(dx * dx + dyOff * dyOff + dz * dz))
    }

    override fun apply3D(noiseResult: NoiseResult, x: Double, y: Double, z: Double): NoiseResult {
        val dy = centerY ?: 0.0
        val dx = x - centerX
        val dyOff = y - dy
        val dz = z - centerZ
        return DefaultedNoiseResult(noiseResult.pureValue * falloffFactor(sqrt(dx * dx + dyOff * dyOff + dz * dz)))
    }

    // ---- 4D ----
    override fun apply4D(noise: Double, x: Double, y: Double, z: Double, w: Double): Double {
        val dy = centerY ?: 0.0
        val dx = x - centerX
        val dyOff = y - dy
        val dz = z - centerZ
        // For 4D, we include `w` in the distance
        return noise * falloffFactor(sqrt(dx * dx + dyOff * dyOff + dz * dz + w * w))
    }

    override fun apply4D(noiseResult: NoiseResult, x: Double, y: Double, z: Double, w: Double): NoiseResult {
        val dy = centerY ?: 0.0
        val dx = x - centerX
        val dyOff = y - dy
        val dz = z - centerZ
        return DefaultedNoiseResult(noiseResult.pureValue * falloffFactor(sqrt(dx * dx + dyOff * dyOff + dz * dz + w * w)))
    }
}


class RadialFalloffModuleBuilder private constructor(): NoiseModuleBuilder<RadialFalloffModuleBuilder>
{
    companion object {
        fun newBuilder(): RadialFalloffModuleBuilder = RadialFalloffModuleBuilder()
    }

    var centerX: Double? = null
    var centerY: Double? = null
    var centerZ: Double? = null
    var maxDistance: Double? = null

    fun withCenterX(centerX: Double?) : RadialFalloffModuleBuilder {
        if (centerX == null) error("centerX cannot be null")
        this.centerX = centerX
        return this
    }

    fun withCenterY(centerY: Double?) : RadialFalloffModuleBuilder {
        this.centerY = centerY
        return this
    }

    fun withCenterZ(centerZ: Double?) : RadialFalloffModuleBuilder {
        if (centerZ == null) error("centerZ cannot be null")
        this.centerZ = centerZ
        return this
    }

    fun withMaxDistance(maxDistance: Double?) : RadialFalloffModuleBuilder {
        if (centerZ == null) error("maxDistance cannot be null")
        this.maxDistance = maxDistance
        return this
    }

    override fun apply(t: NoiseGenerator<*>): NoiseModule {
        centerX?.let { centerZ?.let {  maxDistance?.let {
            return RadialFalloffModule(t, centerX!!, centerY, centerZ!!, maxDistance!!)
        } ?: error("maxDistance cannot be null") } ?: error("centerZ cannot be null") } ?: error("centerX cannot be null")
    }
}


class AdditionModule(
    noiseGenerator: NoiseGenerator<*>,
    var secondaryNoise: NoiseGenerator<*>
) : NoiseModule(noiseGenerator)
{
    override fun apply1D(noise: Double, x: Double): Double {
        val sum = noise + secondaryNoise.evaluateNoise(x)
        val minPossible = -2.0
        val maxPossible =  2.0
        return ((sum - minPossible) / (maxPossible - minPossible)) * 2.0 - 1.0
    }

    override fun apply1D(noiseResult: NoiseResult, x: Double): NoiseResult {
        val sum = secondaryNoise.evaluateNoise(x) + noiseResult.pureValue
        val minPossible = -2.0
        val maxPossible =  2.0
        return DefaultedNoiseResult(((sum - minPossible) / (maxPossible - minPossible)) * 2.0 - 1.0)
    }

    override fun apply2D(noise: Double, x: Double, y: Double): Double {
        val sum = secondaryNoise.evaluateNoise(x, y) + noise
        val minPossible = -2.0
        val maxPossible =  2.0
        return ((sum - minPossible) / (maxPossible - minPossible)) * 2.0 - 1.0
    }

    override fun apply2D(noiseResult: NoiseResult, x: Double, y: Double): NoiseResult {
        val sum = secondaryNoise.evaluateNoise(x, y) + noiseResult.pureValue
        val minPossible = -2.0
        val maxPossible =  2.0
        return DefaultedNoiseResult(((sum - minPossible) / (maxPossible - minPossible)) * 2.0 - 1.0)
    }

    override fun apply3D(noise: Double, x: Double, y: Double, z: Double): Double {
        val sum = secondaryNoise.evaluateNoise(x, y, z) + noise
        val minPossible = -2.0
        val maxPossible =  2.0
        return ((sum - minPossible) / (maxPossible - minPossible)) * 2.0 - 1.0
    }

    override fun apply3D(noiseResult: NoiseResult, x: Double, y: Double, z: Double): NoiseResult {
        val sum = secondaryNoise.evaluateNoise(x, y, z) + noiseResult.pureValue
        val minPossible = -2.0
        val maxPossible =  2.0
        return DefaultedNoiseResult(((sum - minPossible) / (maxPossible - minPossible)) * 2.0 - 1.0)
    }

    override fun apply4D(noise: Double, x: Double, y: Double, z: Double, w: Double): Double {
        val sum = secondaryNoise.evaluateNoise(x, y, z, w) + noise
        val minPossible = -2.0
        val maxPossible =  2.0
        return ((sum - minPossible) / (maxPossible - minPossible)) * 2.0 - 1.0
    }

    override fun apply4D(noiseResult: NoiseResult, x: Double, y: Double, z: Double, w: Double): NoiseResult {
        val sum = secondaryNoise.evaluateNoise(x, y, z, w) + noiseResult.pureValue
        val minPossible = -2.0
        val maxPossible =  2.0
        return DefaultedNoiseResult(((sum - minPossible) / (maxPossible - minPossible)) * 2.0 - 1.0)
    }
}

class AdditionModuleBuilder private constructor(): NoiseModuleBuilder<AdditionModuleBuilder>
{
    companion object {
        fun newBuilder(): AdditionModuleBuilder = AdditionModuleBuilder()
    }

    var secondary: NoiseGenerator<*>? = null

    fun withSecondary(secondaryNoise: NoiseGenerator<*>?) : AdditionModuleBuilder {
        if (secondaryNoise == null) error("Secondary noise cannot be null")
        secondary = secondaryNoise
        return this
    }
    override fun apply(t: NoiseGenerator<*>): NoiseModule {
        secondary?.let { return AdditionModule(t, it) } ?: error("Secondary noise cannot be null")
    }
}

enum class VoronoiDistance { EUCLIDEAN, MANHATTAN, CHEBYSHEV }
enum class VoronoiReturn { F1, F2, F2_MINUS_F1, CELL_VALUE } // CELL_VALUE returns a hashed pseudo-random value per cell

class VoronoiSampler internal constructor(
    private val seed: Long,
    private val frequency: Double,
    private val jitter: Double,
    private val distanceMetric: VoronoiDistance,
    private val returnMode: VoronoiReturn,
    private val normalized: Boolean
) : NoiseGenerator<DefaultedNoiseResult?> {
    // small 64-bit mixer for pseudo-randomness; deterministic per (seed + coords)
    private fun hash64(x: Long): Long {
        var z = x + -7046029254386353131L
        z = (z xor (z ushr 30)) * -4658895280553007687L
        z = (z xor (z ushr 27)) * -7723592293110705685L
        return z xor (z ushr 31)
    }

    // produce a 0..1 double from a hashed long
    private fun hashToDouble01(h: Long): Double {
        val unsigned = h.toULong()
        // shift to 53 bits of mantissa for safe double conversion
        val mant = (unsigned and ((1UL shl 53) - 1UL)).toLong()
        return mant.toDouble() / ( (1L shl 53) - 1 ).toDouble()
    }

    // distance helper
    private fun distSquared(d: Int, dx: DoubleArray): Double {
        return when (distanceMetric) {
            VoronoiDistance.EUCLIDEAN -> {
                var s = 0.0
                for (i in 0 until d) { s += dx[i]*dx[i] }
                s
            }
            VoronoiDistance.MANHATTAN -> {
                var s = 0.0
                for (i in 0 until d) { s += abs(dx[i]) }
                s * s // return squared-like to keep scale sensible
            }
            VoronoiDistance.CHEBYSHEV -> {
                var max = 0.0
                for (i in 0 until d) { max = max(max, abs(dx[i])) }
                max * max
            }
        }
    }

    // Generic n-dimensional sampler: coords is pre-scaled by frequency.
    private fun sampleND(vararg coords: Double): Double {
        val d = coords.size
        if (d < 1 || d > 4) throw IllegalArgumentException("Only 1..4D supported")

        // scale by frequency so one cell = 1.0 in integer grid
        val scaled = DoubleArray(d) { coords[it] * frequency }
        val cell = IntArray(d) { floor(scaled[it]).toInt() }
        DoubleArray(d) { scaled[it] - cell[it] }

        // neighbor search range: -1..+1 in each axis (covers nearest seed)
        val offsets = -1..1

        // track best two distances and corresponding cell hashes
        var best1 = Double.POSITIVE_INFINITY
        var best2 = Double.POSITIVE_INFINITY
        var bestCellHash1 = 0L
        var bestCellHash2 = 0L

        val offsetsList = ArrayList<IntArray>()
        // build offsets combinations (3^d)
        fun buildOffsets(idx: Int, cur: IntArray) {
            if (idx == d) { offsetsList.add(cur.copyOf()); return }
            for (o in offsets) {
                cur[idx] = o
                buildOffsets(idx + 1, cur)
            }
        }
        buildOffsets(0, IntArray(d))

        for (off in offsetsList) {
            // neighbor cell coords
            val nc = IntArray(d) { cell[it] + off[it] }

            // hash cell coords + seed -> single long
            var h = seed
            for (i in 0 until d) {
                // mix the coordinate bits with prime multipliers
                h = h * -7046029254386353131L + (nc[i].toLong() xor (nc[i].toLong() shl 32))
            }
            val cellHash = hash64(h)

            // jitter/vector inside cell (0..1)
            hashToDouble01(cellHash)
            // create per-dimension jitter by rehashing
            val jitterOffsets = DoubleArray(d) { i ->
                val h2 = hash64(cellHash + i.toLong() * -7046029254386353131L)
                val j = hashToDouble01(h2)
                (j - 0.5) * jitter // range [-jitter/2, jitter/2]
            }

            // compute vector from sample point to this feature point
            val dx = DoubleArray(d) { i ->
                // position of feature point: nc[i] + 0.5 (center) + jitterOffset
                val featurePos = (nc[i].toDouble()) + 0.5 + jitterOffsets[i]
                featurePos - scaled[i]
            }

            val distSq = distSquared(d, dx)
            if (distSq < best1) {
                best2 = best1; bestCellHash2 = bestCellHash1
                best1 = distSq; bestCellHash1 = cellHash
            } else if (distSq < best2) {
                best2 = distSq; bestCellHash2 = cellHash
            }
        }

        // compute outputs according to return mode
        val out = when (returnMode) {
            VoronoiReturn.F1 -> best1
            VoronoiReturn.F2 -> best2
            VoronoiReturn.F2_MINUS_F1 -> (best2 - best1)
            VoronoiReturn.CELL_VALUE -> {
                // hashed cell value mapped to -1..1
                val v = (hashToDouble01(bestCellHash1) * 2.0) - 1.0
                v
            }
        }

        // normalization: for Euclidean, max distance (worst) in unit cell to neighbor seed ~ sqrt(d)*1.0
        if (!normalized) return out
        val maxDist = when (distanceMetric) {
            VoronoiDistance.EUCLIDEAN -> d.toDouble().let { sqrt(it) } // sqrt(d)
            VoronoiDistance.MANHATTAN -> d.toDouble() // approximate
            VoronoiDistance.CHEBYSHEV -> 1.0
        }
        // if we used squared distances (we do for efficiency), take sqrt for euclidean-like results
        return when (distanceMetric) {
            VoronoiDistance.EUCLIDEAN -> sqrt(out) / maxDist
            VoronoiDistance.MANHATTAN -> sqrt(out) / maxDist
            VoronoiDistance.CHEBYSHEV -> sqrt(out) / maxDist
        }.coerceIn(0.0, 1.0)
    }

    // public samplers
    fun sample1(x: Double) = sampleND(x)
    fun sample2(x: Double, y: Double) = sampleND(x, y)
    fun sample3(x: Double, y: Double, z: Double) = sampleND(x, y, z)
    fun sample4(x: Double, y: Double, z: Double, w: Double) = sampleND(x, y, z, w)

    override fun evaluateNoise(x: Double, seed: Long): Double = sample1(x)

    override fun evaluateNoise(x: Double, y: Double, seed: Long): Double = sample2(x, y)

    override fun evaluateNoise(x: Double, y: Double, z: Double, seed: Long): Double = sample3(x, y, z)

    override fun evaluateNoise(
        x: Double,
        y: Double,
        z: Double,
        w: Double,
        seed: Long
    ): Double = sample4(x, y, z, w)

    override fun evaluateNoise(x: Double): Double = sample1(x)

    override fun evaluateNoise(x: Double, y: Double): Double = sample2(x, y)

    override fun evaluateNoise(x: Double, y: Double, z: Double): Double = sample3(x, y, z)

    override fun evaluateNoise(
        x: Double,
        y: Double,
        z: Double,
        w: Double
    ): Double = sample4(x, y, z, w)

    override fun evaluateNoiseResult(
        x: Double,
        seed: Long
    ): DefaultedNoiseResult = DefaultedNoiseResult(sample1(x))

    override fun evaluateNoiseResult(
        x: Double,
        y: Double,
        seed: Long
    ): DefaultedNoiseResult  = DefaultedNoiseResult(sample2(x, y))

    override fun evaluateNoiseResult(
        x: Double,
        y: Double,
        z: Double,
        seed: Long
    ): DefaultedNoiseResult  = DefaultedNoiseResult(sample3(x, y, z))

    override fun evaluateNoiseResult(
        x: Double,
        y: Double,
        z: Double,
        w: Double,
        seed: Long
    ): DefaultedNoiseResult  = DefaultedNoiseResult(sample4(x, y, z, w))


    override fun evaluateNoiseResult(
        x: Double
    ): DefaultedNoiseResult = DefaultedNoiseResult(sample1(x))

    override fun evaluateNoiseResult(
        x: Double,
        y: Double
    ): DefaultedNoiseResult  = DefaultedNoiseResult(sample2(x, y))

    override fun evaluateNoiseResult(
        x: Double,
        y: Double,
        z: Double
    ): DefaultedNoiseResult  = DefaultedNoiseResult(sample3(x, y, z))

    override fun evaluateNoiseResult(
        x: Double,
        y: Double,
        z: Double,
        w: Double
    ): DefaultedNoiseResult  = DefaultedNoiseResult(sample4(x, y, z, w))

    override fun getSeed(): Long = seed
}

class VoronoiBuilder(): NoiseBuilder<DefaultedNoiseResult, VoronoiBuilder>() {
    private var seed: Long = 0L
    private var frequency: Double = 1.0
    private var jitter: Double = 1.0
    private var distanceMetric: VoronoiDistance = VoronoiDistance.EUCLIDEAN
    private var returnMode: VoronoiReturn = VoronoiReturn.F1
    private var normalized: Boolean = true

    fun seed(s: Long) = apply { this.seed = s }
    fun frequency(f: Double) = apply { require(f > 0.0); this.frequency = f }
    fun jitter(j: Double) = apply { require(j in 0.0..2.0); this.jitter = j }
    fun distance(dm: VoronoiDistance) = apply { this.distanceMetric = dm }
    fun returnMode(r: VoronoiReturn) = apply { this.returnMode = r }
    fun normalized(n: Boolean) = apply { this.normalized = n }

    override fun createGenerator(): NoiseGenerator<DefaultedNoiseResult?> = VoronoiSampler(seed, frequency, jitter, distanceMetric, returnMode, normalized)
}
