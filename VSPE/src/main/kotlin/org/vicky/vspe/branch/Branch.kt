package org.vicky.vspe.branch

import org.vicky.platform.utils.Vec3
import org.vicky.vspe.Direction
import org.vicky.vspe.platform.systems.dimension.StructureUtils.CorePointsFactory
import org.vicky.vspe.platform.systems.dimension.StructureUtils.factories.CompoundArchPointFactory
import org.vicky.vspe.platform.systems.dimension.StructureUtils.factories.LCurveFactory
import org.vicky.vspe.platform.systems.dimension.StructureUtils.factories.StraightPointFactory
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.RandomSource
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.SeededRandomSource
import org.vicky.vspe.shuffle
import java.util.stream.Collectors
import kotlin.math.*

@JvmOverloads
fun generateLeafBlob(
    origin: Vec3,
    sizeX: Double,
    sizeY: Double,
    sizeZ: Double,
    density: Double = 0.6,
    refinement: Int = 2,
    hollowFactor: Double = 0.25,
    fluffOffset: Double = 0.5,
    seed: Long = System.nanoTime()
): List<Vec3> {
    val rnd = SeededRandomSource(seed)
    val points = mutableListOf<Vec3>()

    // --- 1️⃣ Base Blob Pass ---
    val baseCount = (sizeX * sizeY * sizeZ * density).toInt()
    repeat(baseCount) {
        val x = rnd.nextDouble(-sizeX, sizeX)
        val y = rnd.nextDouble(-sizeY, sizeY)
        val z = rnd.nextDouble(-sizeZ, sizeZ)

        // Ellipsoid shape bias
        val dist = (x * x / (sizeX * sizeX)) + (y * y / (sizeY * sizeY)) + (z * z / (sizeZ * sizeZ))
        if (dist <= 1.0 + rnd.nextDouble(-0.2, 0.2)) {
            points.add(Vec3(origin.x + x, origin.y + y, origin.z + z))
        }
    }

    // --- 2️⃣ Refinement Pass ---
    // Adds small clusters around random existing points
    repeat(refinement) {
        val newPoints = mutableListOf<Vec3>()
        for (p in points.shuffle(rnd).take(points.size / 3)) {
            repeat(5) {
                val off = Vec3.of(
                    p.x + rnd.nextDouble(-0.8, 0.8),
                    p.y + rnd.nextDouble(-0.5, 0.5),
                    p.z + rnd.nextDouble(-0.8, 0.8)
                )
                newPoints.add(off)
            }
        }
        points.addAll(newPoints)
    }

    // --- 3️⃣ Hollow Noise Offset Pass ---
    val center = origin
    val hollowed = points.filter {
        val d = sqrt((it.x - center.x).pow(2) + (it.y - center.y).pow(2) + (it.z - center.z).pow(2))
        d > (min(sizeX, min(sizeY, sizeZ)) * hollowFactor)
    }.toMutableList()

    // Apply fluffy offset duplication
    val fluff = hollowed.flatMap {
        val dx = rnd.nextDouble(-fluffOffset, fluffOffset)
        val dy = rnd.nextDouble(-fluffOffset, fluffOffset)
        val dz = rnd.nextDouble(-fluffOffset, fluffOffset)
        listOf(it, Vec3(it.x + dx, it.y + dy, it.z + dz))
    }

    // Optional: Random thinning for realism
    return fluff.filter { rnd.nextDouble() > 0.1 }
}

interface BranchGenerator {
    /**
     * Generate branch geometry points attached at attachPoint with local tangent.
     * return: list of world-space Vec3 points for branch centerline (in order root->tip)
     */
    fun generate(
        rnd: RandomSource, attach: Vec3, tangent: Vec3, parentRadius: Double,
        length: Double, segments: Int, upwardBias: Double, forwardBias: Double
    ): MutableList<Vec3>

    /**
     * Convenience: generate multiple branches at the same attachment according to rule.
     * Each angle produces one branch. Uses deterministic child RNGs derived from the provided rnd
     * so output is reproducible.
     */
    fun generateAt(
        rnd: RandomSource,
        attach: Vec3,
        tangent: Vec3,
        parentRadius: Double,
        length: Double,
        segments: Int,
        upwardBias: Double,
        forwardBias: Double,
        rule: RingBranchRule
    ): MutableList<MutableList<Vec3>> {
        val result: MutableList<MutableList<Vec3>> = ArrayList()
        // derive a base seed for this call
        val baseSeed = rnd.nextLong()

        // decide angles using a RNG derived from base so rule.decide is stable for this call
        val decideRnd = SeededRandomSource(baseSeed)
        val angles = rule.decide(decideRnd)

        for ((idx, angle) in angles.withIndex()) {
            // create a deterministic child RNG per angle so generator's internal rand -> unique but reproducible
            val childSeed = baseSeed xor angle.hashCode().toLong() xor idx.toLong()
            val childRnd: RandomSource = SeededRandomSource(childSeed)

            // call existing generate()
            val pts = generate(childRnd, attach, tangent, parentRadius, length, segments, upwardBias, forwardBias)
            // ensure the anchor is the first point — generator should include it, but be safe:
            if (pts.isNotEmpty() && pts[0] != attach) pts.add(0, attach)
            result.add(pts)
        }

        return result
    }
}

/**
 * Simple rule: spawn n branches around the ring with optional jitter.
 */
class RingBranchRule(
    var maxCount: Int, // e.g., Math.toRadians(10)
    private val jitterRadians: Double
) {

    fun decide(rnd: RandomSource): MutableList<Double> {
        val count = maxCount
        val angles: MutableList<Double> = ArrayList(count)
        if (count <= 0) return angles

        val goldenOffset = rnd.nextDouble() * 2.0 * Math.PI
        val step = (2.0 * Math.PI) / count.toDouble()

        // Prevent jitter from overlapping neighbors: cap jitter to something safely < half step
        val maxJitter = min(jitterRadians, step * 0.45)

        for (i in 0 until count) {
            val base = goldenOffset + step * i
            val j = (rnd.nextDouble() - 0.5) * 2.0 * maxJitter // in [-maxJitter, +maxJitter]
            var angle = base + j

            // normalize to [0, 2π)
            val twoPi = 2.0 * Math.PI
            angle = (angle % twoPi + twoPi) % twoPi

            angles.add(angle)
        }
        return angles
    }
}

/**
 * Produces a straight-line branch biased toward local "up" (v) and optionally slightly forward (w).
 * Performs no sophisticated curvature — easy to replace later with a Helix or Curved generator.
 */
class SimpleStraightBranchGenerator : BranchGenerator {
    override fun generate(
        rnd: RandomSource,
        attach: Vec3,
        tangent: Vec3,
        parentRadius: Double,
        length: Double,
        segments: Int,
        upwardBias: Double,
        forwardBias: Double
    ): MutableList<Vec3> {
        var segs = segments
        if (segs < 2) segs = max(2, segs)

        val w = tangent.normalize() // trunk axis at attachment

        // world up
        val worldUp = Vec3(0.0, 1.0, 0.0)

        // Project worldUp onto plane orthogonal to w: v = worldUp - (w · worldUp) * w
        val proj = w.intX * worldUp.intX + w.intY * worldUp.intY + w.intZ * worldUp.intZ
        var v = worldUp.add(w.multiply(-proj.toDouble())) // worldUp - proj * w

        val u: Vec3
        // If projection near-zero, w is nearly parallel to worldUp -> pick arbitrary perpendicular
        val eps = 1e-8
        if (abs(v.intX) < eps && abs(v.intY) < eps && abs(v.intZ) < eps) {
            val ref = Vec3(1.0, 0.0, 0.0) // stable reference when near-vertical
            u = ref.crossProduct(w).normalize()
            v = w.crossProduct(u).normalize()
        } else {
            v = v.normalize()
            u = v.crossProduct(w).normalize() // u ⟂ v, u ⟂ w
        }

        // base radial direction around the trunk in (u,v) plane
        val baseAngle = rnd.nextDouble() * 2.0 * Math.PI
        val radial = u.multiply(cos(baseAngle)).add(v.multiply(sin(baseAngle))).normalize()

        // bias toward local-up (v) and slight forward (w)
        var dir = radial.multiply(1.0 - upwardBias)
            .add(v.multiply(upwardBias))
            .add(w.multiply(forwardBias))
            .normalize()

        // Ensure branch doesn't point downwards globally.
        // If you want to allow exactly horizontal, set minDot = 0.0; set >0 to force upward tilt.
        val minDotWithLocalUp = upwardBias // >0 to force a tilt upwards
        val localDot = dir.dot(v)

        if (localDot < minDotWithLocalUp) {
            // push direction more toward v until acceptable
            val needed = (minDotWithLocalUp - localDot) + 0.05
            dir = dir.add(v.multiply(needed)).normalize()
        }

        // sample straight line
        val out: MutableList<Vec3> = ArrayList(segs + 1)
        out.add(attach)
        for (i in 1..segs) {
            val t = i.toDouble() / segs
            val p = attach.add(dir.multiply(length * t))
            out.add(p)
        }
        return out
    }
}

/**
 * Produces a straight-line branch biased toward local "up" (v) and optionally slightly forward (w).
 * Performs no sophisticated curvature — easy to replace later with a Helix or Curved generator.
 */
class LCurveBranchGenerator @JvmOverloads constructor(
    val angle: Double = 30.0,
    val horizontalTiltDegrees: Int = 180,
    val verticalTiltDegrees: Int = 20
) : BranchGenerator {
    override fun generate(
        rnd: RandomSource,
        attach: Vec3,
        tangent: Vec3,
        parentRadius: Double,
        length: Double,
        segments: Int,
        upwardBias: Double,
        forwardBias: Double
    ): MutableList<Vec3> {
        var segs = segments
        if (segs < 2) segs = max(2, segs)

        // random offsets
        val horizontalOffset = rnd.nextInt(-horizontalTiltDegrees, max(1, horizontalTiltDegrees)).toDouble()
        val verticalOffset = rnd.nextDouble() * verticalTiltDegrees

        val yawDeg = horizontalOffset
        val pitchDeg = verticalOffset // tilt upward away from trunk

        // Build params for CorePointsFactory directly
        val params = CorePointsFactory.Params.builder()
            .type(LCurveFactory(angle, LCurveFactory.Plane.XZ, 1.0))
            .segments(segs)
            .width(length * 0.35)
            .height(length * 0.65)
            .yawDegrees(yawDeg)
            .pitchDegrees(-90 + pitchDeg)
            .origin(attach)
            .divergenceStrength(0.8)
            .noiseStrength(0.7)
            .build()

        val pts = CorePointsFactory.generate(params)

        return pts
    }
}

/**
 * Produces a realistic like branch biased toward local "up" (v) and optionally slightly forward (w).
 * Performs some sophisticated curvature...
 */
class CrookedBranch @JvmOverloads constructor(
    var crookedness: Double = 0.5,
    val horizontalTiltDegrees: Int = 180,
    val verticalTiltDegrees: Int = 20
) : BranchGenerator {
    var lastUsedYaw = 0.0
    var lastUsedPitch = 0.0

    override fun generate(
        rnd: RandomSource,
        attach: Vec3,
        tangent: Vec3,
        parentRadius: Double,
        length: Double,
        segments: Int,
        upwardBias: Double,
        forwardBias: Double
    ): MutableList<Vec3> {
        var segs = segments
        if (segs < 15) segs = max(10, segs)

        // random offsets
        val horizontalOffset = rnd.nextInt(-horizontalTiltDegrees, max(1, horizontalTiltDegrees)).toDouble()
        val verticalOffset = max(0.7, rnd.nextDouble()) * verticalTiltDegrees

        val pitchDeg = verticalOffset
        var yawDeg = horizontalOffset + if (rnd.nextBoolean()) lastUsedYaw * 0.5 else -lastUsedYaw * 0.5
        if (abs(yawDeg - lastUsedYaw) < 50) yawDeg += 90
        lastUsedYaw = yawDeg
        val bias = pitchDeg * (0.8 + rnd.nextDouble() * 0.4)

        // Build params for CorePointsFactory directly
        val params = CorePointsFactory.Params.builder()
            .type(StraightPointFactory())
            .segments(segs)
            .width(length * 0.25)
            .height(length)
            .yawDegrees(yawDeg - tangent.toYaw())
            .pitchDegrees(tangent.toPitch() - bias)
            .origin(attach)
            .divergenceStrength(0.4 + 0.6 * crookedness)
            .divergenceDecay(1.0 - (0.3 * crookedness))
            .noiseStrength(0.5 + 1.0 * crookedness)
            .noiseSmooth(1.0 - (0.5 * crookedness))
            .noiseWidthFactor(0.7 - 0.3 * crookedness)
            .build(       /*-----------------------------*/)

        val pts = CorePointsFactory.generate(params)

        return pts
    }
}

class WillowBranch @JvmOverloads constructor(
    var droopStrength: Double = 1.0,
    var xDistancer: Double = 1.0,
    var elegance: Double = 0.8,
    val horizontalTiltDegrees: Int = 180
) : BranchGenerator {
    override fun generate(
        rnd: RandomSource,
        attach: Vec3,
        tangent: Vec3,
        parentRadius: Double,
        length: Double,
        segments: Int,
        upwardBias: Double,
        forwardBias: Double
    ): MutableList<Vec3> {
        val segs = max(3, segments)
        val archHeightMag = (length * 0.4 * droopStrength)
            .coerceAtMost(length / Math.PI * 0.99)
            .coerceAtLeast(0.001)
        val zero = Vec3.of(0.0, 0.0, 0.0)

        val exp = 1.25
        val scaledDF = (xDistancer.pow(exp) - 1) / (1.0.pow(exp) - 1)
        val droopForce = (length * 0.50 * scaledDF).coerceAtMost(length * 0.6)

        val scaled = (xDistancer.pow(exp) - 1) / (1.0.pow(exp) - 1)
        val xDistance = length * scaled
        val end = zero.add(xDistance, -droopForce, 0.0)

        val yawShift = rnd.nextInt(-horizontalTiltDegrees, horizontalTiltDegrees) +
                (horizontalTiltDegrees * rnd.nextDouble())
        val factory = CompoundArchPointFactory(zero, end, +archHeightMag, 0.5)

        val params = CorePointsFactory.Params.builder()
            .type(factory)
            .segments(segs)
            .width(length * 0.2)
            .height(length)
            .yawDegrees(yawShift)
            // .pitchDegrees(-90.0)
            .origin(attach)
            .divergenceDecay(0.95)
            .divergenceProbability(0.05)
            .divergenceStrength(0.05 * elegance)
            .noiseStrength(0.1 * (1.0 - elegance))
            .noiseSmooth(0.9 * elegance)
            .noiseWidthFactor(1.0)
            .build()

        val pts = CorePointsFactory.generate(params)
        return pts
    }

    override fun generateAt(
        rnd: RandomSource,
        attach: Vec3,
        tangent: Vec3,
        parentRadius: Double,
        length: Double,
        segments: Int,
        upwardBias: Double,
        forwardBias: Double,
        rule: RingBranchRule
    ): MutableList<MutableList<Vec3>> {
        var segs = segments
        if (segs < 2) segs = max(2, segs)

        val out: MutableList<MutableList<Vec3>> = ArrayList()
        val decideSeed = rnd.nextLong()
        val decideRnd = SeededRandomSource(decideSeed)
        val angles = rule.decide(decideRnd)

        // 2. For each branch
        for (az in angles) {
            val archHeightMag = (length * 0.4 * droopStrength)
                .coerceAtMost(length / Math.PI * 0.99)
                .coerceAtLeast(0.001)
            val zero = Vec3.of(0.0, 0.0, 0.0)

            val droopForce = (length * 0.50 * droopStrength.pow(1.25)).coerceAtMost(length * 0.6)
            val xDistance = length * xDistancer.pow(1.25)
            val end = zero.add(xDistance, -droopForce, 0.0)
            val factory = CompoundArchPointFactory(zero, end, +archHeightMag, 0.5)

            val params = CorePointsFactory.Params.builder()
                .type(factory)
                .segments(segs)
                .width(length * 0.2)
                .height(length)
                .yawDegrees(Math.toDegrees(az))
                // .pitchDegrees(-90.0)
                .origin(attach)
                .divergenceDecay(0.95)
                .divergenceProbability(0.05)
                .divergenceStrength(0.05 * elegance)
                .noiseStrength(0.1 * (1.0 - elegance))
                .noiseSmooth(0.9 * elegance)
                .noiseWidthFactor(1.0)
                .build()
            val pts = CorePointsFactory.generate(params)

            // Safety: ensure anchor is the first point (some factories / origins can be quirky)
            if (pts.isEmpty() || pts[0] != attach) pts.add(0, attach)

            out.add(pts)
        }

        return out
    }
}

/**
 * Produces a realistic like branch biased toward local "up" (v) and optionally slightly forward (w).
 * Performs some sophisticated curvature...
 */
class MultiAroundBranch @JvmOverloads constructor(
    val verticalTiltDegrees: Double = 20.0,
    val crookedness: Double = 0.5
) : BranchGenerator {
    override fun generate(
        rnd: RandomSource,
        attach: Vec3,
        tangent: Vec3,
        parentRadius: Double,
        length: Double,
        segments: Int,
        upwardBias: Double,
        forwardBias: Double
    ): MutableList<Vec3> = mutableListOf()

    override fun generateAt(
        rnd: RandomSource,
        attach: Vec3,
        tangent: Vec3,
        parentRadius: Double,
        length: Double,
        segments: Int,
        upwardBias: Double,
        forwardBias: Double,
        rule: RingBranchRule
    ): MutableList<MutableList<Vec3>> {
        var segs = segments
        if (segs < 2) segs = max(2, segs)

        val out: MutableList<MutableList<Vec3>> = ArrayList()

        // Decide angles (azimuths around trunk) using the provided RNG
        // We create a decideRnd so the rule's decide() is deterministic for this call
        val decideSeed = rnd.nextLong()
        val decideRnd = SeededRandomSource(decideSeed)
        val angles = rule.decide(decideRnd)

        val forward = tangent.normalize()
        val worldUp = Vec3(0.0, 1.0, 0.0)
        val tmpUp = if (abs(forward.dot(worldUp)) > 0.9) Vec3(1.0, 0.0, 0.0) else worldUp

        val right = forward.crossProduct(tmpUp).normalize()
        val up = right.crossProduct(forward).normalize()

        // 2. For each branch
        for (az in angles) {
            val elevationRad = Math.toRadians(verticalTiltDegrees)

            // local spherical direction
            val localDir =
                forward * (cos(elevationRad)) +
                        right * (cos(az) * sin(elevationRad)) +
                        up * (sin(az) * sin(elevationRad))

            val dir = localDir.normalize()

            // 3. Convert back to yaw/pitch
            val yawDeg = Math.toDegrees(atan2(dir.x, dir.z))
            val pitchDeg = Math.toDegrees(atan2(dir.y, sqrt(dir.x * dir.x + dir.z * dir.z)))


            // Build params and generate
            val params = CorePointsFactory.Params.builder()
                .type(StraightPointFactory()) // or LCurveFactory / others as desired
                .segments(segs)
                .width(length * 0.35)
                .height(length * 0.65)
                .yawDegrees(yawDeg)
                .pitchDegrees(pitchDeg)
                .origin(attach)
                .divergenceStrength(crookedness)
                .divergenceProbability(max(0.01, crookedness * 0.5))
                .noiseStrength(0.8 * crookedness)
                .noiseSmooth(2.0.pow(crookedness))
                .noiseHighFreq(0.50)
                .noiseLowFreq(0.007)
                .noiseWidthFactor(1.7)
                .noiseDriftStrength(1.0)
                .build()

            val pts = CorePointsFactory.generate(params).toMutableList()

            // Safety: ensure anchor is the first point (some factories / origins can be quirky)
            if (pts.isEmpty() || pts[0] != attach) pts.add(0, attach)

            out.add(pts)
        }

        return out
    }
}

private fun Double.toRadians() = Math.toRadians(this)
operator fun Vec3.plus(other: Vec3): Vec3 =
    Vec3(this.x + other.x, this.y + other.y, this.z + other.z)

operator fun Vec3.minus(other: Vec3): Vec3 =
    Vec3(this.x - other.x, this.y - other.y, this.z - other.z)

// Scalar multiply/divide
operator fun Vec3.times(scalar: Double): Vec3 =
    Vec3(this.x * scalar, this.y * scalar, this.z * scalar)

operator fun Vec3.div(scalar: Double): Vec3 =
    Vec3(this.x / scalar, this.y / scalar, this.z / scalar)

operator fun Vec3.plus(scalar: Double): Vec3 =
    Vec3(this.x + scalar, this.y + scalar, this.z + scalar)

operator fun Vec3.minus(scalar: Double): Vec3 =
    Vec3(this.x - scalar, this.y - scalar, this.z - scalar)

// Scalar multiply/divide
operator fun Vec3.times(other: Vec3): Vec3 =
    this.multiply(other)

operator fun Vec3.div(other: Vec3): Vec3 =
    this.divide(other)

/**
 * Produces a straight-line branch biased toward local "up" (v) and optionally slightly forward (w).
 * Performs no sophisticated curvature — easy to replace later with a Helix or Curved generator.
 */
class DirectableBranch @JvmOverloads constructor(
    val direction: Direction = Direction.EAST,
    val horizontalTiltDegrees: Int = 180,
    val verticalTiltDegrees: Int = 20,
    val crookedness: Double = 0.4
) : BranchGenerator {
    override fun generate(
        rnd: RandomSource,
        attach: Vec3,
        tangent: Vec3,
        parentRadius: Double,
        length: Double,
        segments: Int,
        upwardBias: Double,
        forwardBias: Double
    ): MutableList<Vec3> {
        var segs = if (segments < 2) 2 else segments
        val baseDir = Vec3(direction.dx.toDouble(), 0.0, direction.dz.toDouble()).normalize()

        val baseYaw = Math.toDegrees(atan2(baseDir.x, baseDir.z))

        // sample offsets
        val horizontalOffset = rnd.nextInt(-horizontalTiltDegrees, max(1, horizontalTiltDegrees)).toDouble()
        val elevationDeg = rnd.nextDouble() * verticalTiltDegrees.toDouble() // 0..V (only upward)

        // combine: yaw = baseYaw + horizontal jitter
        val yawDeg = baseYaw + horizontalOffset

        // pitch: 90 - elevation so that elevation==0 -> pitch=90 (horizontal)
        val pitchDeg = 90.0 - elevationDeg

        // Build params for CorePointsFactory
        val params = CorePointsFactory.Params.builder()
            .type(StraightPointFactory())
            .segments(segs)
            .width(length * 0.35)
            .height(length * 0.65)
            .yawDegrees(yawDeg)
            .pitchDegrees(pitchDeg)
            .origin(attach)
            .divergenceStrength(crookedness)
            .divergenceProbability(max(0.01, crookedness * 0.5))
            .noiseStrength(0.8 * crookedness)
            .noiseSmooth(2.0.pow(crookedness))
            .noiseHighFreq(0.50)
            .noiseLowFreq(0.007)
            .noiseWidthFactor(1.7)
            .noiseDriftStrength(1.0)
            .build()

        return CorePointsFactory.generate(params).toMutableList()
    }
}

data class AttachmentPoint(
    val position: Vec3, val tangent: Vec3, val parentRadius: Double, // optional semantic use
    val heightFactor: Double
)

data class Sphere(val center: Vec3, val radius: Double)

/**
 * Walks attachments and spawns branches using a rule + generator.
 * Keeps a list of occupied spheres (trunk + branches) to test collisions.
 */
data class BranchingEngine(
    private val generator: BranchGenerator,
    private val rule: RingBranchRule,
    private val branchThickness: Double,
    private var safetyMargin: Double,
    private val useMulti: Boolean = false
) {
    init {
        this.safetyMargin = safetyMargin
    }

    /**
     * Generate branches for all attachments. Returns list of branch centerlines (each a {@link List<Vec3>}).
    </Vec3> */
    fun generateAll(
        rnd: RandomSource,
        attachments: MutableList<AttachmentPoint>,
        segmentsPerBranch: Int,
        baseLengthMultiplier: Double
    ): MutableList<MutableList<Vec3>> {
        val allBranches: MutableList<MutableList<Vec3>> = ArrayList()
        val occupied: MutableList<Sphere> = ArrayList()

        // --- configurable knobs ---
        val trunkCollisionFactor = 0.35               // trunk spheres shrink to 35% of parent radius
        val minBranchThickness = 0.10                 // realistic branch radius
        val defaultForwardBias = 0.06                 // small forward push
        val allowAngleJitterAttempts = 3              // if immediate collision, try small jitter a few times

        // Pre-fill trunk occupancy with smaller spheres to avoid blocking neighbors
        for (ap in attachments) {
            val r = 0.01.coerceAtLeast(ap.parentRadius * trunkCollisionFactor)
            occupied.add(Sphere(ap.position, r))
        }

        for (i in attachments.indices) {
            val ap = attachments[i]

            // upwardBias scaled by height as before (tweakable)
            val upwardBias: Double = 0.4 + 0.6 * (1.0 - ap.heightFactor)

            // force forwardBias to be small positive — avoid inward negative pushes
            val forwardBias = defaultForwardBias

            val length = baseLengthMultiplier * (1.0 + (rnd.nextDouble() - 0.5) * 0.3)

            // per-attachment seed for determinism
            val attachSeed = rnd.nextLong() xor i.toLong()

            if (useMulti) {
                // Use generateAt to produce multiple branches for this single attachment (rule-driven)
                val multi = generator.generateAt(
                    SeededRandomSource(attachSeed),
                    ap.position,
                    ap.tangent,
                    ap.parentRadius,
                    length,
                    segmentsPerBranch,
                    upwardBias,
                    forwardBias,
                    rule
                )

                // Trim & accept each returned branch (single pass)
                for (branch in multi) {
                    // ensure anchor present
                    if (branch.isNotEmpty() && branch[0] != ap.position) branch.add(0, ap.position)
                    allBranches.add(branch)
                }
            } else {
                // Original single-angle-per-rule behavior (with jitter attempts)
                val angles = rule.decide(rnd.fork(attachSeed))
                for (baseAngle in angles) {
                    var branchSafe: MutableList<Vec3>? = null

                    // deterministic child RNG based on baseAngle so generator is reproducible
                    val childSeedBase = rnd.nextLong() xor baseAngle.hashCode().toLong() xor i.toLong()
                    var attempts = 0
                    var angleToUse = baseAngle

                    while (attempts < allowAngleJitterAttempts) {
                        val childRnd = rnd.fork(childSeedBase xor attempts.toLong())

                        val branchPts = generator.generate(
                            childRnd,
                            ap.position,
                            ap.tangent,
                            ap.parentRadius,
                            length,
                            segmentsPerBranch,
                            upwardBias,
                            forwardBias
                        )

                        // ensure anchor present
                        if (branchPts.isNotEmpty() && branchPts[0] != ap.position) branchPts.add(0, ap.position)

                        val safe = trimBranchAgainstSpheres(
                            branchPts, occupied, ap.position,
                            minBranchThickness.coerceAtLeast(branchThickness), safetyMargin
                        )

                        if (safe.size > 1) {
                            branchSafe = safe
                            break
                        } else {
                            // small jitter and retry — mutate attempt angle; generator should use childRnd so it varies
                            angleToUse += (attempts + 1) * Math.toRadians(6.0) * (if (attempts % 2 == 0) 1.0 else -1.0)
                        }
                        attempts++
                    }

                    // If we got a safe branch, accept it and add occupancy
                    if (branchSafe != null) {
                        val segRadius = minBranchThickness.coerceAtLeast(branchThickness)
                        for (p in branchSafe) {
                            occupied.add(Sphere(p, segRadius))
                        }
                        allBranches.add(branchSafe)
                    }
                }
            }
        }
        return allBranches
    }

    /**
     * Removes points from the end of `branch` if they collide with any occupied spheres (excluding anchor).
     * Returns the trimmed list.
     */
    private fun trimBranchAgainstSpheres(
        branch: MutableList<Vec3>?,
        occupied: MutableList<Sphere>,
        anchor: Vec3?,
        branchRadius: Double,
        margin: Double
    ): MutableList<Vec3> {
        if (branch == null || branch.size <= 1) return branch!!

        var lastSafeIdx = 0 // anchor always safe
        for (i in 1..<branch.size) {
            val p = branch[i]
            var collides = false
            for (s in occupied) {
                // allow overlap with the anchor sphere (so branches attach)
                if (s.center == anchor) continue

                val minDist: Double = s.radius + branchRadius + margin
                val dx: Double = p.x - s.center.x
                val dy: Double = p.y - s.center.y
                val dz: Double = p.z - s.center.z
                val distSq = dx * dx + dy * dy + dz * dz
                if (distSq < minDist * minDist) {
                    collides = true
                    break
                }
            }
            if (collides) break
            lastSafeIdx = i
        }

        // copy safe portion (0..lastSafeIdx)
        return branch.subList(0, lastSafeIdx + 1)
            .stream().collect(Collectors.toList())
    }
}