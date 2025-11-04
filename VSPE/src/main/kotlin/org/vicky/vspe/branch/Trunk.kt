package org.vicky.vspe.branch

import org.vicky.platform.utils.Vec3
import org.vicky.vspe.platform.systems.dimension.StructureUtils.BezierCurve
import org.vicky.vspe.platform.systems.dimension.StructureUtils.CorePointsFactory
import org.vicky.vspe.platform.systems.dimension.StructureUtils.CurveFunctions
import org.vicky.vspe.platform.systems.dimension.StructureUtils.CurveFunctions.pitch
import org.vicky.vspe.platform.systems.dimension.StructureUtils.CurveFunctions.radius
import org.vicky.vspe.platform.systems.dimension.StructureUtils.SpiralUtil
import org.vicky.vspe.platform.systems.dimension.StructureUtils.SpiralUtil.generateHelixAroundCurve
import org.vicky.vspe.platform.systems.dimension.StructureUtils.factories.StraightPointFactory
import org.vicky.vspe.platform.systems.dimension.TimeCurve
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.RandomSource
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.SeededRandomSource
import java.util.function.Function
import kotlin.math.cos
import kotlin.math.sin


interface TrunkGenerator {
    fun generatePath(seed: Long, start: Vec3, height: Double, rnd: RandomSource): List<Vec3>
    fun voxelize(path: List<Vec3>): Set<Vec3>
}

data class TrunkResult(
    val mainPath: List<Vec3>,
    val voxels: Set<Vec3>,
    val trunkThicknessFun: Function<Double, Double> = Function { i -> 0.0 },
    val trunkRadiusFun: Function<Double, Double> = Function { i -> 0.0 }
)

class WillowTrunk(
    private val droopStrength: Double = 1.0,
    private val elegance: Double = 0.8
) : TrunkGenerator {

    override fun generatePath(seed: Long, start: Vec3, height: Double, rnd: RandomSource): List<Vec3> {
        val path = mutableListOf<Vec3>()
        var current = start
        var direction = Vec3(0.0, 1.0, 0.0)

        for (i in 0..height.toInt()) {
            val sway = Vec3(
                (rnd.nextDouble() - 0.5) * droopStrength,
                1.0 - rnd.nextDouble() * 0.1,
                (rnd.nextDouble() - 0.5) * droopStrength
            )
            direction = direction.add(sway).normalize()
            current = current.add(direction.multiply(0.8 * elegance))
            path += current
        }
        return path
    }

    override fun voxelize(path: List<Vec3>): Set<Vec3> =
        path.flatMap { point -> sphereAround(point, 0.5) }.toSet()
}

class ConiferousTrunk(
    private val lean: Double = 0.1
) : TrunkGenerator {

    override fun generatePath(seed: Long, start: Vec3, height: Double, rnd: RandomSource): List<Vec3> {
        val direction = Vec3(
            (rnd.nextDouble() - 0.5) * lean,
            1.0,
            (rnd.nextDouble() - 0.5) * lean
        ).normalize()

        return (0..height.toInt()).map { i ->
            start.add(direction.multiply(i.toDouble()))
        }
    }

    override fun voxelize(path: List<Vec3>): Set<Vec3> =
        path.flatMap { point -> sphereAround(point, 0.5) }.toSet()
}

class OakTrunk(
    private val twistStrength: Double = 0.3
) : TrunkGenerator {

    override fun generatePath(seed: Long, start: Vec3, height: Double, rnd: RandomSource): List<Vec3> {
        val path = mutableListOf<Vec3>()
        var angle = 0.0
        var current = start

        for (i in 0..height.toInt()) {
            angle += rnd.nextDouble() * twistStrength
            val dx = cos(angle) * 0.2
            val dz = sin(angle) * 0.2
            current = current.add(Vec3(dx, 1.0, dz))
            path += current
        }
        return path
    }

    override fun voxelize(path: List<Vec3>): Set<Vec3> =
        path.flatMap { point -> sphereAround(point, 0.9) }.toSet()
}

class MangroveTrunk(
    private val rootCount: Int = 3,
    private val rootSpread: Double = 2.0
) : TrunkGenerator {

    override fun generatePath(seed: Long, start: Vec3, height: Double, rnd: RandomSource): List<Vec3> {
        val path = mutableListOf<Vec3>()
        val baseRoots = (0 until rootCount).map { i ->
            val angle = i * (2 * Math.PI / rootCount)
            Vec3(cos(angle) * rootSpread, 0.0, sin(angle) * rootSpread)
        }

        // Merge all roots into one vertical trunk
        for (root in baseRoots) {
            val rootStart = start.add(root)
            for (i in 0..(height / 2).toInt()) {
                val t = i / (height / 2)
                val interp = rootStart.lerp(start, t)
                path += interp
            }
        }

        // Then main vertical stem
        for (i in 0..(height / 2).toInt()) {
            path += start.add(Vec3(0.0, i.toDouble(), 0.0))
        }

        return path.distinct()
    }

    override fun voxelize(path: List<Vec3>): Set<Vec3> =
        path.flatMap { point -> sphereAround(point, 0.7) }.toSet()
}

fun sphereAround(center: Vec3, radius: Double): List<Vec3> {
    val voxels = mutableListOf<Vec3>()
    val r = radius.toInt() + 1
    for (x in -r..r) for (y in -r..r) for (z in -r..r) {
        val v = Vec3(x.toDouble(), y.toDouble(), z.toDouble())
        if (v.length() <= radius) voxels += center.add(v)
    }
    return voxels
}

/**
 * Generate a multi-root -> spiral trunk assembly.
 *
 * @param center         ground center from which roots are placed (y = ground).
 * @param numRoots       number of initial root strands (e.g. 5).
 * @param rootSpread     elliptical spread (circular shape).
 * @param rootLength     approximate length of each root until meet point.
 * if null, generated automatically above center.
 * @param trunkHeight    vertical height of spiral trunk above meetPoint.
 * @param trunkRadius    base radius of the spiral trunk at its base.
 * @param trunkTaper     taper factor (0..1) applied over trunk length (1 = no taper).
 * @param numberOfTurns  desired turns of the helix along trunkHeight.
 * @param rootThickness  thickness for each root strand.
 * @param trunkThickness thickness of spiral strand(s).
 * @param sourceRnd      the random source to use
 */
@JvmOverloads
fun generateMultiRootSpiralTrunk(
    center: Vec3 = Vec3.of(0.0, 0.0, 0.0),
    numRoots: Int = 5,
    rootSpread: Double = 38.1,
    rootLength: Double = 20.0,
    trunkHeight: Double = 100.0,
    trunkRadius: Double = 16.0,            // scaled down from 15
    trunkTaper: Double = 0.5,
    numberOfTurns: Int = 10,
    rootThickness: Double = 2.2,          // slim roots
    trunkThickness: Double = 1.5,         // chunky trunk strand
    sourceRnd: RandomSource = SeededRandomSource(System.currentTimeMillis())
): TrunkResult {
    val rnd = sourceRnd.fork((sourceRnd.nextLong() + sourceRnd.nextInt() * sourceRnd.nextFloat()).toLong())
    val ends = CorePointsFactory.generate(
        CorePointsFactory.Params.builder()
            .origin(center)
            .height(trunkHeight)
            .width(trunkRadius)
            .segments(20)
            .type(StraightPointFactory())
            .noiseStrength(0.45)
            .noiseLowFreq(0.005)
            .noiseHighFreq(2.0)
            .divergenceProbability(0.7)
            .divergenceDecay(0.67)
            .divergenceStrength(0.65)
            .noiseSmooth(0.4)
            .build()
    )
    var lastNext = 0.0

    val pitchSegments = L {
        val totalTurns = numberOfTurns // <-- pass from spiral generator
        val turnsPerSegment = rootLength / numberOfTurns // each lasts 4–6 turns
        val segmentLength = turnsPerSegment / totalTurns.toDouble()

        var last = 0.0

        while (last < 1.0) {
            val next = (last + segmentLength).coerceAtMost(1.0)
            val lastValue = rnd.nextDouble(-0.08, 0.07)
            val nextValue = 0.15 * (if (rnd.nextBoolean()) -1 else 1)

            add(
                CurveFunctions.Segment(
                    lastValue,
                    nextValue,
                    last,
                    next,
                    TimeCurve.INVERTED_QUADRATIC
                )
            )

            last = next
            lastNext = nextValue
        }
    }

    val roots = generateHelixAroundCurve(
        BezierCurve.generatePoints(listOf(center.subtract(0.0, rootLength, 0.0), center), 100),
        radius(rootSpread, trunkRadius, 0.0, 1.0, TimeCurve.INVERTED_QUADRATIC),
        CurveFunctions.multiFade(pitchSegments),
        numRoots,
        0.8f,
        radius(rootThickness, trunkThickness, 0.0, 1.0, TimeCurve.QUADRATIC),
        SpiralUtil.DefaultDecorators.SPIRAL.decorator,
        true,
        false
    )

    val thickness = radius(trunkThickness, trunkThickness * trunkTaper, 0.0, 1.0, TimeCurve.QUADRATIC)
    val radius = CurveFunctions.multiFade(L {
        add(CurveFunctions.Segment(trunkRadius, trunkRadius * (trunkTaper), 0.0, 0.7, TimeCurve.INVERTED_QUADRATIC))
        add(CurveFunctions.Segment(trunkRadius * (trunkTaper), 0.0, 0.7, 1.0, TimeCurve.QUADRATIC))
    })

    val trunk = generateHelixAroundCurve(
        BezierCurve.generatePoints(ends, 200),
        radius,
        pitch(lastNext, lastNext + 0.15, 0.0, 1.0, TimeCurve.LINEAR),
        numRoots,
        0.8f,
        thickness,
        SpiralUtil.DefaultDecorators.SPIRAL.decorator,
        false,
        true
    )

    val final = buildSet {
        addAll(trunk)
        addAll(roots)
    }

    return TrunkResult(ends, final, thickness, radius)
}

private fun thickenSpiral(points: List<Vec3>, thickness: Int): List<Vec3> {
    val result = mutableListOf<Vec3>()
    val radius = thickness / 2.0

    for (p in points) {
        // Simple circular cross-section fill
        for (dx in -radius.toInt()..radius.toInt()) {
            for (dz in -radius.toInt()..radius.toInt()) {
                if (dx * dx + dz * dz <= radius * radius) {
                    result.add(Vec3(p.x + dx, p.y, p.z + dz))
                }
            }
        }
    }
    return result
}

inline fun <T> L(block: MutableList<T>.() -> Unit) =
    mutableListOf<T>().apply(block)

fun noisyEllipsePoints(
    length: Double,          // X-axis size
    breadth: Double,         // Z-axis size
    count: Int,              // number of points
    rnd: RandomSource,       // your RandomSource instance
    noiseStrength: Double = 0.25, // 0.0–1.0 typical range
    verticalBias: Double = 0.0     // optional upward tilt
): List<Vec3> {
    return List(count) { i ->
        val baseAngle = (i.toDouble() / count) * (2 * Math.PI)

        // --- Noise offsets ---
        val angleNoise = (rnd.nextDouble() - 0.5) * noiseStrength * 2.0
        val radiusNoise = (rnd.nextDouble() - 0.5) * noiseStrength * 0.5

        // --- Apply base ellipse math ---
        val angle = baseAngle + angleNoise
        val x = Math.cos(angle) * (length * (1.0 + radiusNoise))
        val z = Math.sin(angle) * (breadth * (1.0 + radiusNoise))

        // Optional: add vertical wiggle or bias
        val y = (rnd.nextDouble() - 0.5) * noiseStrength * 2.0 + verticalBias

        Vec3(x, y, z)
    }
}