package org.vicky.vspe.platform.systems.dimension.StructureUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vicky.platform.utils.Vec3;
import org.vicky.utilities.Pair;

import java.util.*;
import java.util.function.Function;

import static java.lang.Math.clamp;
import static org.vicky.vspe.platform.systems.dimension.StructureUtils.ProceduralStructureGenerator.*;

public class SpiralUtil {
    public static final double VERTICAL_TOLERANCE = 0.5;
    private static final double EPS = 1e-6;

    public static double findLengthOfPath(List<Vec3> controlPoints) {
        var currentPoint = controlPoints.removeFirst();
        Queue<Vec3> queue = new LinkedList<>(controlPoints);

        double pathLength = 0.0;

        while (!queue.isEmpty()) {
            var current = queue.poll();
            pathLength += currentPoint.distance(current);
            currentPoint = current;
        }

        return pathLength;
    }

    public static Vec3 tangentAtLength(double targetLength, List<Vec3> controlPoints) {
        if (controlPoints.size() < 2) return new Vec3(0, 1, 0);

        double accumulated = 0.0;
        for (int i = 1; i < controlPoints.size(); i++) {
            Vec3 a = controlPoints.get(i - 1);
            Vec3 b = controlPoints.get(i);
            double segmentLength = a.distance(b);

            if (accumulated + segmentLength >= targetLength) {
                // within this segment
                Vec3 tangent = b.subtract(a).normalize();
                return tangent;
            }
            accumulated += segmentLength;
        }

        // fallback: tangent of last segment
        return controlPoints.get(controlPoints.size() - 1)
                .subtract(controlPoints.get(controlPoints.size() - 2))
                .normalize();
    }

    public static Vec3 safeTangentAt(List<Vec3> points, int index) {
        int n = points.size();
        if (n < 2) return new Vec3(0, 1, 0); // fallback straight up

        if (index <= 0) {
            // start: forward difference
            return points.get(1).subtract(points.get(0)).normalize();
        } else if (index >= n - 1) {
            // end: backward difference
            return points.get(n - 1).subtract(points.get(n - 2)).normalize();
        } else {
            // middle: central difference
            Vec3 prev = points.get(index - 1);
            Vec3 next = points.get(index + 1);
            return next.subtract(prev).normalize();
        }
    }

    public static Vec3 findPointOnPathFromLength(double targetLength, List<Vec3> controlPoints) {
        if (controlPoints == null || controlPoints.size() < 2) return null;

        double accumulated = 0.0;

        for (int i = 0; i < controlPoints.size() - 1; i++) {
            Vec3 start = controlPoints.get(i);
            Vec3 end = controlPoints.get(i + 1);

            double segmentLength = start.distance(end);

            if (accumulated + segmentLength >= targetLength) {
                double remaining = targetLength - accumulated;
                double t = remaining / segmentLength; // interpolation factor
                return start.lerp(end, t);
            }

            accumulated += segmentLength;
        }

        // If the target length is beyond the path, return the last point
        return controlPoints.get(controlPoints.size() - 1);
    }

    public static SpiralResult generateThickSpiralWithStart(
            Vec3 startPoint,
            int width,
            int height,
            float spiralTightness,
            double stepRate,
            int numberOfTurns,
            float taper,
            int thickness,
            boolean taperAffectThickness
    ) {
        List<Vec3> positions = new ArrayList<>();
        double totalAngleDegrees = 360.0 * numberOfTurns;

        double worldX = startPoint.x;
        double worldY = startPoint.y;
        double worldZ = startPoint.z;

        for (double angle = 0; angle <= totalAngleDegrees; angle += stepRate) {
            double angleRad = Math.toRadians(angle);
            double progress = angle / totalAngleDegrees;

            double currentRadius = width * Math.pow(taper, progress);

            double localX = currentRadius * Math.cos(angleRad);
            double localZ = currentRadius * Math.sin(angleRad);
            double localY = height * progress * spiralTightness;

            worldX = startPoint.x + localX;
            worldY = startPoint.y + localY;
            worldZ = startPoint.z + localZ;

            int thicknessToUse = thickness;
            if (taperAffectThickness) {
                thicknessToUse = (int) (thicknessToUse * Math.pow(taper, progress));
            }
            thicknessToUse /= 2;

            for (int dx = -thicknessToUse; dx <= thicknessToUse; dx++) {
                for (int dz = -thicknessToUse; dz <= thicknessToUse; dz++) {
                    for (int dy = -thicknessToUse; dy <= thicknessToUse; dy++) {
                        if (dx * dx + dz * dz <= thicknessToUse * thicknessToUse) {
                            positions.add(new Vec3(
                                    worldX + dx,
                                    worldY + dy,
                                    worldZ + dz
                            ));
                        }
                    }
                }
            }
        }

        Vec3 endPoint = new Vec3(worldX, worldY, worldZ);
        return new SpiralResult(positions, endPoint);
    }

    public static List<Vec3> generateThickSpiral(int width, int height, float spiralTightness, double stepRate, int numberOfTurns, float taper, int thickness, boolean taperAffectThickness) {
        List<Vec3> positions = new ArrayList<>();
        double totalAngleDegrees = 360 * numberOfTurns;

        for (double angle = 0; angle <= totalAngleDegrees; angle += stepRate) {
            double angleRad = Math.toRadians(angle);
            double progress = angle / totalAngleDegrees;

            // Taper: multiply radius by taper factor per full turn
            double currentRadius = (double) width * Math.pow(taper, progress);

            double x = currentRadius * Math.cos(angleRad);
            double z = currentRadius * Math.sin(angleRad);
            double y = height * progress * spiralTightness;

            int thicknessToUse = thickness;
            if (taperAffectThickness) {
                thicknessToUse *= (int) Math.pow(taper, progress);
            }
            thicknessToUse = thicknessToUse / 2;
            for (int dx = -thicknessToUse; dx <= thicknessToUse; dx++) {
                for (int dz = -thicknessToUse; dz <= thicknessToUse; dz++) {
                    for (int dy = -thicknessToUse; dy <= thicknessToUse; dy++) {
                        if (dx * dx + dz * dz <= thicknessToUse * thicknessToUse) { // circle in X-Z plane
                            positions.add(new Vec3(x + dx, y + dy, z + dz));
                        }
                    }
                }
            }
        }
        return positions;
    }

    public static Set<Vec3> generateVineWithSpiralNoBezier(
            List<Vec3> controlPoints,
            Function<Double, Double> thickness,
            int strands,
            float steps,
            Function<Double, Double> radiusFunction,
            Function<Double, Double> pitchFunction) {

        Set<Vec3> result = new HashSet<>();
        if (controlPoints == null || controlPoints.size() < 2) {
            return result;
        }
        // 1. Sample smooth path by arc length
        result.addAll(generateHelixAroundCurve(controlPoints, radiusFunction, pitchFunction, thickness, strands, steps, DefaultDecorators.SPIRAL.decorator, false, true));

        return result;
    }

    public static List<Vec3> generateSpiralBundle(int width,
                                                  int height,
                                                  float spiralTightness,
                                                  double stepRate,
                                                  int numberOfTurns,
                                                  float taper,
                                                  int strandThickness,
                                                  boolean taperAffectThickness,
                                                  int strands) {
        // Generate the points for the first spiral
        List<Vec3> baseSpiralPoints = generateThickSpiral(width, height, spiralTightness, stepRate, numberOfTurns, taper, strandThickness, taperAffectThickness);
        List<Vec3> totalSpirals = new ArrayList<>();

        for (int i = 0; i < strands; i++) {
            double rotationAngle = (360.0 / strands) * i;
            double rotationAngleRad = Math.toRadians(rotationAngle);

            // Apply rotation to each point in the base spiral
            List<Vec3> currentSpiralPoints = new ArrayList<>();
            for (Vec3 point : baseSpiralPoints) {
                // Translate the point so the center is the origin
                double translatedX = point.x - 0;
                double translatedZ = point.z - 0;

                // Apply the rotation
                double rotatedX = translatedX * Math.cos(rotationAngleRad) - translatedZ * Math.sin(rotationAngleRad);
                double rotatedZ = translatedX * Math.sin(rotationAngleRad) + translatedZ * Math.cos(rotationAngleRad);

                // Translate the point back
                currentSpiralPoints.add(new Vec3(rotatedX + 0, point.y, rotatedZ + 0));
            }
            totalSpirals.addAll(currentSpiralPoints);
        }

        for (double curr = 0; curr <= height; curr++) {
            double progress = curr / height;
            double currentRadius = (double) width * Math.pow(taper, progress);
            int thicknessToUse = strandThickness;
            if (taperAffectThickness) {
                thicknessToUse *= Math.pow(taper, progress);
            }
            thicknessToUse = thicknessToUse / 2;
            int radiusOfFiller = (int) (currentRadius - thicknessToUse);
            for (int dx = -radiusOfFiller; dx <= radiusOfFiller; dx++) {
                for (int dz = -radiusOfFiller; dz <= radiusOfFiller; dz++) {
                    if (dx * dx + dz * dz <= radiusOfFiller * radiusOfFiller) {
                        totalSpirals.add(new Vec3(dx, curr, dz));
                    }
                }
            }
        }

        return totalSpirals;
    }

    public static List<Vec3> generateVine(List<Vec3> controlPoints,
                                          double baseThickness,
                                          double taperRate,
                                          double twistRate,
                                          boolean hollow) {

        List<Vec3> vinePath = BezierCurve.generatePoints(controlPoints, 100); // 1. Generate a smooth path
        List<Vec3> vineBlocks = new ArrayList<>();

        // 2. Iterate along the path and add thickness, wiggles, and taper
        for (int i = 0; i < vinePath.size(); i++) {
            Vec3 currentPos = vinePath.get(i);

            // Calculate taper and thickness for the current point
            double progress = (double) i / vinePath.size();
            double currentThickness = baseThickness * Math.pow(taperRate, progress);

            // Add thickness around the path point
            int radius = (int) (currentThickness / 2);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double distSq = dx * dx + dz * dz;

                    // Check if the point is within the circular cross-section
                    if (distSq <= radius * radius) {
                        // Apply rotation (twist) to the cross-section
                        double angle = progress * twistRate;
                        double rotatedX = dx * Math.cos(angle) - dz * Math.sin(angle);
                        double rotatedZ = dx * Math.sin(angle) + dz * Math.cos(angle);

                        // Add the wiggles to the position
                        Vec3 blockPos = new Vec3(
                                currentPos.x + rotatedX,
                                currentPos.y,
                                currentPos.z + rotatedZ
                        );

                        // 3. Fill the hollow space if not hollow
                        if (!hollow && distSq > (radius - 1) * (radius - 1)) {
                            // Only place solid blocks if it's the outermost layer
                            vineBlocks.add(blockPos);
                        } else if (hollow && distSq > (radius - 2) * (radius - 2)) {
                            // Only place the outer shell for hollow vines
                            vineBlocks.add(blockPos);
                        }
                    }
                }
            }
        }

        // For filling a *single* solid vine, a simpler approach is to use the path to define a solid
        // shape and not rely on the hollow flag. You would generate the entire volume.

        return vineBlocks;
    }

    public static Set<Vec3> generateVineWithSpiral(
            List<Vec3> controlPoints,
            Function<Double, Double> thickness,
            int strands,
            float steps,
            Function<Double, Double> radiusFunction,
            Function<Double, Double> pitchFunction) {

        Set<Vec3> result = new HashSet<>();
        if (controlPoints == null || controlPoints.size() < 2) {
            return result;
        }

        // 1. Sample smooth path by arc length
        List<Vec3> path = BezierCurve.generatePoints(controlPoints, 200);
        result.addAll(generateHelixAroundCurve(path, radiusFunction, pitchFunction, thickness, strands, steps, DefaultDecorators.SPIRAL.decorator, false, true));

        return result;
    }

    /**
     * Generates a spiral/helix structure wrapped around a curve.
     *
     * @param bezierPoints   List of curve points (sampled Bezier).
     * @param radiusFunction Radius from curve index -> radius of the spiral.
     * @param pitchFunction  Pitch curve: progress (0..1) -> turns per unit progress.
     *                       Example: progress -> 5.0 * progress gives 0→5 turns.
     * @param thickness      Thickness of each strand (radius for generateSphere).
     * @param strands        Number of spiral strands (2 = double helix, etc).
     * @return Set of Vec3 positions.
     */
    public static Set<Vec3> generateHelixAroundCurve(
            @NotNull List<Vec3> bezierPoints,
            @NotNull Function<Double, Double> radiusFunction,
            @NotNull Function<Double, Double> pitchFunction,
            @NotNull Function<Double, Double> thickness, int strands,
            float steps,
            @Nullable CurveDecoration decorator,
            boolean hollow,
            boolean fillDisks) {

        Set<Vec3> result = new HashSet<>();
        int m = bezierPoints.size();
        if (m < 2) return result;

        Vec3[] t = new Vec3[m];
        Vec3[] u = new Vec3[m];
        Vec3[] v = new Vec3[m];

        // --- 1) compute tangents (central difference where possible) ---
        for (int i = 0; i < m; i++) {
            if (i == 0) {
                t[i] = bezierPoints.get(1).subtract(bezierPoints.get(0)).normalize();
            } else if (i == m - 1) {
                t[i] = bezierPoints.get(m - 1).subtract(bezierPoints.get(m - 2)).normalize();
            } else {
                t[i] = bezierPoints.get(i + 1).subtract(bezierPoints.get(i - 1)).normalize();
            }
        }

        // --- 2) initial frame at i=0 ---
        {
            Vec3 tangent0 = t[0];
            Vec3 worldUp = new Vec3(0, 1, 0);
            Vec3 arbUp = Math.abs(tangent0.dot(worldUp)) > 0.99 ? new Vec3(1, 0, 0) : worldUp;
            Vec3 proj = arbUp.subtract(tangent0.multiply(tangent0.dot(arbUp)));
            if (proj.lengthSq() < EPS) proj = new Vec3(1, 0, 0); // robust fallback
            u[0] = proj.normalize();
            v[0] = t[0].crossProduct(u[0]).normalize();
        }

        // --- 3) propagate frames robustly ---
        int zeroCount = 0;
        for (int i = 1; i < m; i++) {
            Vec3 tPrev = t[i - 1];
            Vec3 tCur = t[i];

            // axis = tPrev x tCur
            Vec3 axis = tPrev.crossProduct(tCur);
            double axisLen = Math.sqrt(axis.lengthSq());
            double cosAngle = clamp(tPrev.dot(tCur), -1.0, 1.0);
            double angle = Math.acos(cosAngle);

            Vec3 candidateU;

            if (axisLen > 1e-8 && Math.abs(angle) > 1e-8) {
                // rotate previous u around axis by angle
                axis = axis.multiply(1.0 / axisLen); // normalize
                candidateU = rotateAroundAxis(u[i - 1], axis, angle);
                // remove tiny drift: Gram-Schmidt w.r.t. tCur
                double proj = tCur.dot(candidateU);
                if (Math.abs(proj) > 1e-12) {
                    candidateU = candidateU.subtract(tCur.multiply(proj));
                }
            } else {
                // tangents nearly aligned -> project previous u into new plane
                candidateU = u[i - 1].subtract(tCur.multiply(tCur.dot(u[i - 1])));
            }

            // if candidate degenerated, rebuild from worldUp projection
            if (candidateU == null || candidateU.lengthSq() < 1e-12) {
                Vec3 worldUp = Math.abs(tCur.dot(new Vec3(0, 1, 0))) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
                candidateU = worldUp.subtract(tCur.multiply(tCur.dot(worldUp)));
                if (candidateU.lengthSq() < 1e-12) candidateU = new Vec3(1, 0, 0); // final fallback
            }

            u[i] = candidateU.normalize();
            v[i] = tCur.crossProduct(u[i]).normalize();

            if (u[i].lengthSq() < 1e-12 || v[i].lengthSq() < 1e-12) zeroCount++;
        }

        // debug: make sure we didn't accidentally produce degenerate frames
        // System.out.println("frame degenerates: " + zeroCount + " / " + m);
        // System.out.println("t");
        // System.out.println(Arrays.stream(t).map(it -> " x: " + it.x + " y: " + it.y + " z: " + it.z + "\n").collect(Collectors.joining(",")));
        // System.out.println("u");
        // System.out.println(Arrays.stream(u).map(it -> " x: " + it.x + " y: " + it.y + " z: " + it.z + "\n").collect(Collectors.joining(",")));
        // System.out.println("v");
        // System.out.println(Arrays.stream(v).map(it -> " x: " + it.x + " y: " + it.y + " z: " + it.z + "\n").collect(Collectors.joining(",")));

        // --- 2. Generate strands with pitch accumulation
        double[] accumulatedPhase = new double[strands];
        double[] accumulatedAntiPhase = new double[strands];
        double accumulatedPhaseSingle = 0;
        List<List<PWR>> shellStrands = new ArrayList<>(Math.max(1, strands * 2));
        for (int s = 0; s < Math.max(1, strands * 2); s++) shellStrands.add(new ArrayList<>());

        // choose sample step for cylinder sweep (tune if needed)
        double sampleStep = 0.4; // 0.4..0.6 typical; smaller = denser

        for (double j = 0; j < m; j += steps) {
            int i = (int) j;
            double progress = (double) i / (m - 1);  // 0 → 1
            double radius = radiusFunction.apply(progress);

            // estimate step length for integration
            double ds = (i == 0) ? 0 : Math.sqrt(bezierPoints.get(i).subtract(bezierPoints.get(i - 1)).lengthSq());

            if (decorator != null) {
                switch (decorator) {
                    case MultiStrandedCurveDecoration multi -> {
                        // For multi-decorators we call generateMulti once per bundle/phase and dispatch by id.
                        // Caller must ensure that the decorator emits PWR.id values within [0, shellStrands.size()-1].
                        Set<PWR> locals = multi.generateMulti(progress, accumulatedPhaseSingle, radius, thickness.apply(progress), i);

                        // Optionally update phase(s) if multi decorator expects external phase evolution:
                        accumulatedPhaseSingle += ds * pitchFunction.apply(progress);

                        // group and dispatch
                        Map<Integer, List<PWR>> grouped = groupById(locals);
                        for (Map.Entry<Integer, List<PWR>> e : grouped.entrySet()) {
                            int strandIndex = e.getKey();
                            List<PWR> strandList;
                            if (shellStrands.size() <= strandIndex) {
                                shellStrands.add(strandIndex, new ArrayList<>());
                            }
                            strandList = shellStrands.get(strandIndex);
                            for (PWR local : e.getValue()) {
                                Vec3 world = u[i].multiply(local.p.x)
                                        .add(t[i].multiply(local.p.y))
                                        .add(v[i].multiply(local.p.z))
                                        .add(bezierPoints.get(i));
                                // preserve id in created world PWR (use the 4-arg ctor)
                                strandList.add(new PWR(world, local.r, local.h, local.id));
                            }
                        }
                    }
                    case StrandedCurveDecoration strandedCurveDecoration -> {
                        for (int s = 0; s < strands; s++) {
                            accumulatedPhase[s] += ds * pitchFunction.apply(progress);

                            double basePhase = 2 * Math.PI * s / strands;
                            double phi = basePhase + accumulatedPhase[s];

                            Set<PWR> locals = decorator.generate(progress, phi, radius, thickness.apply(progress), i);
                            for (var local : locals) {
                                Vec3 world = u[i].multiply(local.p.x)
                                        .add(t[i].multiply(local.p.y))
                                        .add(v[i].multiply(local.p.z))
                                        .add(bezierPoints.get(i));
                                shellStrands.get(s).add(new PWR(world, local.r, local.h));
                            }
                        }
                    }
                    case DoubleStrandedCurveDecoration counterer -> {
                        for (int s = 0; s < strands * 2; s++) {
                            if (s % 2 == 0) accumulatedPhase[s / 2] += ds * pitchFunction.apply(progress);
                            else accumulatedAntiPhase[s / 2] += ds * pitchFunction.apply(progress);

                            double basePhase = 2 * Math.PI * s / (strands * 2);
                            double phi =
                                    s % 2 == 0 ?
                                            basePhase + accumulatedPhase[s / 2]
                                            : basePhase + accumulatedAntiPhase[s / 2];

                            Set<PWR> locals =
                                    s % 2 == 0 ?
                                            counterer.generate(progress, phi, radius, thickness.apply(progress), i)
                                            : counterer.generateAnti(progress, phi, radius, thickness.apply(progress), i);
                            for (var local : locals) {
                                Vec3 world = u[i].multiply(local.p.x)
                                        .add(t[i].multiply(local.p.y))
                                        .add(v[i].multiply(local.p.z))
                                        .add(bezierPoints.get(i));
                                shellStrands.get(s).add(new PWR(world, local.r, local.h));
                            }
                        }
                    }
                    default -> {
                        accumulatedPhaseSingle += ds * pitchFunction.apply(progress);
                        double phi = accumulatedPhaseSingle;

                        Set<PWR> locals = decorator.generate(progress, phi, radius, thickness.apply(progress), i);
                        List<PWR> strand0 = shellStrands.getFirst();
                        for (var local : locals) {
                            Vec3 world = u[i].multiply(local.p.x)
                                    .add(t[i].multiply(local.p.y))
                                    .add(v[i].multiply(local.p.z))
                                    .add(bezierPoints.get(i));
                            strand0.add(new PWR(world, local.r, local.h));
                        }
                    }
                }
            }
        }

        Set<Vec3> outerVoxels = connectStrandsWithCylinders(shellStrands, sampleStep);
        if (!hollow) {
            if (fillDisks) {
                double innerSteps = steps / 5;
                Set<Vec3> innerVoxels = new HashSet<>();
                for (double j = 0; j < m; j += innerSteps) {
                    double progress = j / (m - 1);
                    double radius = radiusFunction.apply(progress);
                    Vec3 center = interpolateAlong(bezierPoints, progress);
                    Vec3 tangent = interpolateTangent(bezierPoints, progress);

                    // fill a disk with radius reduced by thickness
                    double innerRadius = Math.max(0, radius - thickness.apply(progress));
                    fillDiskAtVoxel(innerVoxels, center, tangent, innerRadius);
                }
                outerVoxels.addAll(innerVoxels);
            } else {
                Set<Vec3> innerVoxels = new HashSet<>();
                for (double j = 0; j < m; j += steps) {
                    int i = (int) j;
                    double progress = (double) i / (m - 1);
                    double radius = radiusFunction.apply(progress);

                    // center of this cross-section
                    Vec3 center = bezierPoints.get(i);

                    // fill a disk with radius reduced by thickness
                    double innerRadius = Math.max(0, radius - thickness.apply(progress));
                    fillDiskAtVoxel(innerVoxels, center, t[i], innerRadius);
                }
                outerVoxels.addAll(innerVoxels);
            }
        }

        for (Vec3 b : outerVoxels) {
            result.add(new Vec3(b.x, b.y, b.z));
        }

        return result;
    }

    /**
     * Rasterize a filled disk at world center 'center' with plane normal 'normal' and world radius 'radius'.
     * Writes block coords into 'out' (Set<Vec3>).
     * <p>
     * Inclusion test: project candidate block center into disk-local coordinates (px,pz) and accept if sqrt(px^2+pz^2) <= radius + 0.5*SQRT2_MARGIN.
     */
    private static void fillDiskAtVoxel(Set<Vec3> out, Vec3 center, Vec3 normal, double radius) {
        if (radius <= 0.0001) {
            out.add(center);
            return;
        }
        Vec3 n = normal.normalize();
        Pair<Vec3, Vec3> uvPair = buildPerpFrame(n);
        Vec3 u = uvPair.key();
        Vec3 v = uvPair.value();

        // margin to include block centers that intersect the circle boundary.
        // 0.5 is safe; using 0.707 (sqrt(2)/2) is slightly more inclusive; use 0.5 by default.
        double margin = 0.5;

        int intR = (int) Math.round(radius + margin);
        // iterate integer block candidates in square [-intR..intR] in disk-plane coords
        for (int iz = -intR; iz <= intR; iz++) {
            for (int ix = -intR; ix <= intR; ix++) {
                // world candidate = center + u*ix + v*iz
                Vec3 candidate = u.multiply(ix).add(v.multiply(iz)).add(center);
                // project back to local coords px = dot(candidate-center, u) etc (should be ix,iz exactly)
                double px = candidate.subtract(center).dot(u);
                double pz = candidate.subtract(center).dot(v);
                double ld = Math.hypot(px, pz);
                if (ld <= radius + margin + 1e-9) {
                    out.add(candidate);
                }
            }
        }
    }

    private static Vec3 interpolateAlong(List<Vec3> pts, double progress) {
        double scaled = progress * (pts.size() - 1);
        int i = (int) Math.floor(scaled);
        int j = Math.min(i + 1, pts.size() - 1);

        double alpha = scaled - i;
        return pts.get(i).multiply(1 - alpha).add(pts.get(j).multiply(alpha));
    }

    private static Vec3 interpolateTangent(List<Vec3> pts, double progress) {
        double scaled = progress * (pts.size() - 1);
        int i = (int) Math.floor(scaled);
        int j = Math.min(i + 1, pts.size() - 1);

        return pts.get(j).subtract(pts.get(i)).normalize();
    }


    /**
     * Connect consecutive points in each strand with cylinder sweeps, but skip any connection
     * whose vertical difference exceeds verticalTolerance (prevents vertical bridges).
     *
     * @param shellStrands ordered points-per-strand (each PWR contains world pos + radius)
     * @param sampleStep   max spacing along an edge when sampling disks (0.4..0.6 typical)
     * @return set of Vec3 block centers that form the connected geometry (integer centers)
     */
    private static Set<Vec3> connectStrandsWithCylinders(
            List<List<PWR>> shellStrands,
            double sampleStep) {

        // internal integer voxel set for dedupe & performance
        Set<Vec3> filledVox = new HashSet<>();

        for (List<PWR> strand : shellStrands) {
            if (strand == null || strand.isEmpty()) continue;

            // fill endcap at first point
            PWR prev = strand.getFirst();
            fillDiskAtVoxel(filledVox, prev.p, new Vec3(0, 1, 0), prev.r);

            for (int i = 1; i < strand.size(); i++) {
                PWR cur = strand.get(i);

                double dy = Math.abs(prev.p.y - cur.p.y);
                // if (dy <= verticalTolerance) {
                // allowed: rasterize cylinder between prev -> cur
                fillCylinderBetween(filledVox, prev.p, cur.p, prev.r, cur.r, sampleStep);
                // small endcaps to make seams robust
                Vec3 seg = cur.p.subtract(prev.p);
                Vec3 normal = seg.lengthSq() < 1e-9 ? new Vec3(0, 1, 0) : seg;
                fillDiskAtVoxel(filledVox, prev.p, normal, prev.r);
                fillDiskAtVoxel(filledVox, cur.p, normal, cur.r);
                // } else {
                // vertical difference too large: do NOT connect vertically
                // still add disks at the endpoints so geometry remains closed locally
                // fillDiskAtVoxel(filledVox, prev.p, new Vec3(0, 1, 0), prev.r);
                // fillDiskAtVoxel(filledVox, cur.p, new Vec3(0, 1, 0), cur.r);
                // }

                prev = cur;
            }

            // ensure last endcap
            fillDiskAtVoxel(filledVox, prev.p, new Vec3(0, 1, 0), prev.r);
        }

        // convert integer voxels back to Vec3 centers for compatibility with rest of code
        Set<Vec3> result = new HashSet<>(filledVox.size());
        for (Vec3 b : filledVox) result.add(new Vec3(b.x, b.y, b.z));
        return result;
    }

    private static Pair<Vec3, Vec3> buildPerpFrame(Vec3 dir) {
        Vec3 n = dir;
        Vec3 worldUp = Math.abs(n.dot(new Vec3(0, 1, 0))) > 0.999 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = worldUp.subtract(n.multiply(n.dot(worldUp)));
        if (u.lengthSq() < 1e-12) u = new Vec3(1, 0, 0);
        u = u.normalize();
        Vec3 v = n.crossProduct(u).normalize();
        return new Pair<>(u, v); // simple pair: left=u, right=v (use your Pair or return array)
    }

    /**
     * Rasterize a filled disk at world center 'center' with plane normal 'normal' and world radius 'radius'.
     * Writes block coords into 'out' (Set<Vec3>).
     * <p>
     * Inclusion test: project candidate block center into disk-local coordinates (px,pz) and accept if sqrt(px^2+pz^2) <= radius + 0.5*SQRT2_MARGIN.
     */
    private static Set<PWR> fillDiskAtVoxelAsPWR(Vec3 center, Vec3 normal, double radius) {
        Set<PWR> result = new HashSet<>();
        if (radius <= 0.0001) {
            result.add(new PWR(center, 1.0, true));
            return result;
        }

        Vec3 n = normal.normalize();
        Pair<Vec3, Vec3> uvPair = buildPerpFrame(n);
        Vec3 u = uvPair.key();
        Vec3 v = uvPair.value();

        // margin to include block centers that intersect the circle boundary
        double margin = 0.5;

        int intR = (int) Math.round(radius + margin);
        for (int iz = -intR; iz <= intR; iz++) {
            for (int ix = -intR; ix <= intR; ix++) {
                Vec3 candidate = u.multiply(ix).add(v.multiply(iz)).add(center);
                double px = candidate.subtract(center).dot(u);
                double pz = candidate.subtract(center).dot(v);
                double ld = Math.hypot(px, pz);
                if (ld <= radius + margin + 1e-9) {
                    // Wrap candidate into PWR
                    result.add(new PWR(candidate, 1.0, true));
                }
            }
        }

        return result;
    }

    /**
     * Fill a true cylinder between points a and b with bottom radius rA and top radius rB.
     * Samples disks along the axis and rasterizes them into 'out'. The sample spacing is chosen adaptively:
     * step = min(maxStep, max(0.25, min(rA,rB)*0.5))
     * <p>
     * Using a margin of 0.5 makes block coverage stable and prevents small gaps/blobs.
     */
    private static void fillCylinderBetween(Set<Vec3> out, Vec3 a, Vec3 b, double rA, double rB, double maxStep) {
        double L = distance(a, b);
        if (L < 1e-9) {
            // degenerate: single disk
            fillDiskAtVoxel(out, a, new Vec3(0, 1, 0), Math.max(rA, rB));
            return;
        }

        Vec3 dir = b.subtract(a).multiply(1.0 / L); // normalized direction
        // adaptive step: ensure good overlap relative to radii
        double minRadius = Math.max(0.0001, Math.min(Math.abs(rA), Math.abs(rB)));
        double step = Math.min(maxStep, Math.max(0.25, minRadius * 0.5));
        int steps = Math.max(1, (int) Math.round(L / step));

        // iterate samples including endpoints
        for (int k = 0; k <= steps; k++) {
            double u = (double) k / steps;
            Vec3 pos = lerp(a, b, u);
            double radius = rA * (1.0 - u) + rB * u;
            // disk normal is cylinder axis (dir)
            fillDiskAtVoxel(out, pos, dir, Math.abs(radius));
        }
    }

    /**
     * Generates a list of positions for a sphere with the given parameters.
     *
     * @param center   The central point of the sphere.
     * @param diameter The diameter of the sphere.
     * @param hollow   True for a hollow shell, false for a solid sphere.
     * @return A Set of Vec3 objects for the sphere's blocks.
     */
    public static Set<Vec3> generateSphere(Vec3 center, double diameter, boolean hollow) {
        Set<Vec3> result = new HashSet<>();
        double radius = diameter / 2;

        // Use squared radius for faster distance comparisons
        double radiusSq = radius * radius;

        // Create a bounding box to iterate through
        int minX = (int) Math.floor(center.x - radius);
        int minY = (int) Math.floor(center.y - radius);
        int minZ = (int) Math.floor(center.z - radius);
        int maxX = (int) Math.round(center.x + radius);
        int maxY = (int) Math.round(center.y + radius);
        int maxZ = (int) Math.round(center.z + radius);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    // Calculate the squared distance from the center
                    double distSq = distance(center, new Vec3(x, y, z));

                    if (hollow) {
                        // For a hollow sphere, check if the point is on the surface
                        // by comparing it to the radius and a slightly smaller inner radius
                        double innerRadiusSq = (radius - 1.0) * (radius - 1.0);
                        if (distSq <= radiusSq && distSq >= innerRadiusSq) {
                            result.add(new Vec3(x, y, z));
                        }
                    } else {
                        // For a solid sphere, simply check if the point is inside the radius
                        if (distSq <= radiusSq) {
                            result.add(new Vec3(x, y, z));
                        }
                    }
                }
            }
        }
        return result;
    }

    public static Set<Vec3> generateThicknessPath(
            List<Vec3> path,
            Function<Double, Double> breadthFunction, // maps progress [0..1] → breadth (radius)
            float step
    ) {
        Set<Vec3> result = new LinkedHashSet<>();
        if (path.size() < 2) return result;

        // Compute total path length
        double totalLength = 0.0;
        for (int i = 1; i < path.size(); i++) {
            totalLength += path.get(i).distance(path.get(i - 1));
        }

        double traveled = 0.0;

        for (int i = 1; i < path.size(); i++) {
            Vec3 start = path.get(i - 1);
            Vec3 end = path.get(i);
            Vec3 segment = end.subtract(start);
            double segLength = segment.length();

            // Normalize segment direction
            Vec3 direction = segment.normalize();

            for (double t = 0; t <= segLength; t += step) {
                double progress = (traveled + t) / totalLength;

                // Find lateral breadth (like leaf width or branch radius)
                double breadth = breadthFunction.apply(progress);

                // compute left-right offset using a perpendicular vector
                Vec3 perp = findPerpendicular(direction).normalize().multiply(breadth);

                // combine main path, droop, and width expansion
                Vec3 point = start.add(direction.multiply(t)).add(perp);
                result.add(point);
            }

            traveled += segLength;
        }

        return result;
    }

    public static Set<Vec3> generateWallPath(
            List<Vec3> path,
            Function<Double, Double> horizontalBreadthFn,  // width along curve
            Function<Double, Double> verticalThicknessFn,  // height along curve
            float step,           // spacing along the path
            float fillStep        // spacing inside the wall cross-section
    ) {
        Set<Vec3> result = new LinkedHashSet<>();
        if (path.size() < 2) return result;

        // Compute total path length for progress parameterization
        double totalLength = 0.0;
        for (int i = 1; i < path.size(); i++) {
            totalLength += path.get(i).distance(path.get(i - 1));
        }

        double traveled = 0.0;

        for (int i = 1; i < path.size(); i++) {
            Vec3 start = path.get(i - 1);
            Vec3 end = path.get(i);
            Vec3 segment = end.subtract(start);
            double segLength = segment.length();

            Vec3 forward = segment.normalize();

            // Establish local axes (right, up)
            Vec3 up = new Vec3(0, 1, 0);
            if (Math.abs(forward.dot(up)) > 0.95) up = new Vec3(1, 0, 0);
            Vec3 right = forward.crossProduct(up).normalize();
            Vec3 trueUp = right.crossProduct(forward).normalize();

            for (double t = 0; t <= segLength; t += step) {
                double progress = (traveled + t) / totalLength;

                // Horizontal (width) and vertical (height) scaling
                double hBreadth = horizontalBreadthFn.apply(progress);
                double vThickness = verticalThicknessFn.apply(progress);

                // Optional blending to soften transitions
                double blend = 0.5 + 0.5 * Math.sin(progress * Math.PI);
                double effectiveWidth = hBreadth * (1.0 - blend * 0.3);
                double effectiveHeight = vThickness * (0.7 + blend * 0.3);

                Vec3 center = start.add(forward.multiply(t));

                // --- Fill the cross-section (like a rectangle grid) ---
                for (double x = -effectiveWidth; x <= effectiveWidth; x += fillStep) {
                    for (double y = -effectiveHeight; y <= effectiveHeight; y += fillStep) {
                        Vec3 point = center
                                .add(right.multiply(x))
                                .add(trueUp.multiply(y));
                        result.add(point);
                    }
                }
            }

            traveled += segLength;
        }

        return result;
    }


    private static Vec3 findPerpendicular(Vec3 v) {
        // Choose a vector that isn't parallel, to avoid zero cross product
        Vec3 ref = Math.abs(v.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        return v.crossProduct(ref).normalize();
    }


    public enum DefaultDecorators {
        SINGLES(new StrandedCurveDecoration() {
            public Set<PWR> generate(double progress, double phase, double radius, double thickness, int step) {
                double y = 0;
                return Set.of(new PWR(new Vec3(radius, y, radius), thickness, false));
            }
        }),
        SPIRAL(new StrandedCurveDecoration() {
            public Set<PWR> generate(double progress, double phase, double radius, double thickness, int step) {
                double x = Math.cos(phase) * radius; // right
                double y = 0;                        // up (flat)
                double z = Math.sin(phase) * radius; // forward
                return Set.of(new PWR(new Vec3(x, y, z), thickness, false));
            }
        }),
        COUNTER_SPIRAL(new DoubleStrandedCurveDecoration() {
            public Set<PWR> generate(double progress, double phase, double radius, double thickness, int step) {
                Set<PWR> points = new HashSet<>();

                // strand A
                double x1 = Math.cos(phase) * radius;
                double y1 = 0;
                double z1 = Math.sin(phase) * radius;
                points.add(new PWR(new Vec3(x1, y1, z1), thickness, false));

                return points;
            }

            public Set<PWR> generateAnti(double progress, double phase, double radius, double thickness, int step) {
                Set<PWR> points = new HashSet<>();

                // strand B (180° opposite)
                double x2 = Math.cos(-phase + Math.PI) * radius;
                double y2 = 0;
                double z2 = Math.sin(-phase + Math.PI) * radius;
                points.add(new PWR(new Vec3(x2, y2, z2), thickness, false));

                return points;
            }
        }),
        ONCOUNTER_SPIRAL(new DoubleStrandedCurveDecoration() {
            public Set<PWR> generate(double progress, double phase, double radius, double thickness, int step) {
                Set<PWR> points = new HashSet<>();

                // normal spiral
                double x1 = Math.cos(phase) * radius;
                double y1 = 0;
                double z1 = Math.sin(phase) * radius;
                points.add(new PWR(new Vec3(x1, y1, z1), thickness, false));

                return points;
            }

            public Set<PWR> generateAnti(double progress, double phase, double radius, double thickness, int step) {
                Set<PWR> points = new HashSet<>();

                // counter spiral (opposite phase)
                double x2 = Math.cos(-phase) * radius;
                double y2 = 0;
                double z2 = Math.sin(-phase) * radius;
                points.add(new PWR(new Vec3(x2, y2, z2), thickness, false));

                return points;
            }
        }),
        BEAD((progress, phase, radius, thickness, step) -> {
            int spacing = (int) Math.max(1, thickness * 3);
            if (step % spacing == 0) {
                double x = Math.cos(phase) * radius; // right
                double y = 0;                        // up (flat)
                double z = Math.sin(phase) * radius; // forward
                return Set.of(new PWR(new Vec3(x, y, z), thickness, false));
            } else {
                return Set.of();
            }
        }),
        RIBBON((progress, phase, radius, thickness, step) -> {
            Set<PWR> ribbon = new HashSet<>();
            int segments = 12; // resolution of the strip
            double width = radius * 1.2; // how wide the ribbon spreads

            for (int i = -segments; i <= segments; i++) {
                double offset = (i / (double) segments) * width;
                double twist = Math.sin(progress * Math.PI * 4) * 0.3; // flutter effect
                double x = Math.cos(phase + twist) * (radius + offset);
                double y = 0;
                double z = Math.sin(phase + twist) * (radius + offset);
                ribbon.add(new PWR(new Vec3(x, y, z), thickness, false));
            }
            return ribbon;
        });
        public final CurveDecoration decorator;

        DefaultDecorators(CurveDecoration decorator) {
            this.decorator = decorator;
        }
    }

    public static class SpiralResult {
        public final List<Vec3> positions;
        public final Vec3 endPoint;

        public SpiralResult(List<Vec3> positions, Vec3 endPoint) {
            this.positions = positions;
            this.endPoint = endPoint;
        }
    }

    /**
     * Must only generate for conical coordinates up (0, 1, 0) and right (1, 0, 0)
     */
    @FunctionalInterface
    public interface CurveDecoration {
        Set<PWR> generate(double progress, double phase, double radius, double thickness, int step);
    }

    /**
     * Must only generate for conical coordinates up (0, 1, 0) and right (1, 0, 0)
     */
    @FunctionalInterface
    public interface StrandedCurveDecoration extends CurveDecoration {
        Set<PWR> generate(double progress, double phase, double radius, double thickness, int step);
    }

    /**
     * A decorator that can emit multiple strand outputs in a single call.
     * <p>
     * PWR.id MUST be set to the absolute target strand index (0..strands-1).
     * The caller (outer spiral walker) is responsible for allocating shellStrands
     * with size >= max strand index used.
     */
    public interface MultiStrandedCurveDecoration extends CurveDecoration {
        /**
         * Generate a set of PWR points. Each PWR.id must indicate which strand
         * that point belongs to (absolute index).
         *
         * @param progress  curve progress (0..1)
         * @param phase     phase offset (radians)
         * @param radius    current radius
         * @param thickness current thickness scalar
         * @param step      step index
         * @return set of PWRs whose 'id' fields are absolute strand indices
         */
        Set<PWR> generateMulti(double progress, double phase, double radius, double thickness, int step);

        @Override
        default Set<PWR> generate(double progress, double phase, double radius, double thickness, int step) {
            return Set.of();
        }
    }

    public static Map<Integer, List<PWR>> groupById(Collection<PWR> pwrs) {
        Map<Integer, List<PWR>> map = new LinkedHashMap<>();
        for (PWR p : pwrs) {
            map.computeIfAbsent(p.id, k -> new ArrayList<>()).add(p);
        }
        return map;
    }


    /**
     * Must only generate for conical coordinates up (0, 1, 0) and right (1, 0, 0)
     */
    public interface DoubleStrandedCurveDecoration extends CurveDecoration {
        Set<PWR> generate(double progress, double phase, double radius, double thickness, int step);

        Set<PWR> generateAnti(double progress, double phase, double radius, double thickness, int step);
    }

    public static final class PWR {
        public final Vec3 p;
        public final double r;
        public final boolean h;
        public final int id;

        public PWR(Vec3 p, double r, boolean h) {
            this.p = p;
            this.r = r;
            this.h = h;
            this.id = 1;
        }

        public PWR(Vec3 p, double r, boolean h, int id) {
            this.p = p;
            this.r = r;
            this.h = h;
            this.id = id;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof PWR)) return false;
            if (!(((PWR) obj).h == this.h)) return false;
            if (!(((PWR) obj).r == this.r)) return false;
            return ((PWR) obj).p.equals(this.p);
        }

        @Override
        public String toString() {
            return "PWR{origin: " + p + ", radius: " + r + ", isH: " + h + ", id: " + id + "}";
        }
    }
}
