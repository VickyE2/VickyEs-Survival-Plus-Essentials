package org.vicky.vspe.platform.systems.dimension.StructureUtils;

import org.vicky.platform.utils.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.vicky.vspe.platform.systems.dimension.StructureUtils.ProceduralStructureGenerator.distance;

public final class BezierCurve {
    private BezierCurve() {
    } // utility class

    /**
     * Generate 'samples' points along a Bezier curve defined by controlPoints.
     * Uses De Casteljau's algorithm and supports any number of control points >= 1.
     *
     * @param controlPoints list of Vec3 control points (must be non-null)
     * @param samples       number of points to generate (minimum 2 -> yields start and end)
     * @return {@link List<Vec3>}of generated points (size == max(2, samples))
     */
    public static List<Vec3> generatePoints(List<Vec3> controlPoints, int samples) {
        if (controlPoints == null || controlPoints.isEmpty()) return Collections.emptyList();
        int n = Math.max(2, samples); // ensure endpoints
        List<Vec3> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            double t = (double) i / (double) (n - 1); // 0..1 inclusive
            out.add(deCasteljau(controlPoints, t));
        }
        return out;
    }

    /**
     * Compute a single point on the Bezier curve at parameter t using De Casteljau.
     *
     * @param controlPoints control points
     * @param t             parameter in [0,1]
     * @return Vec3 point on curve
     */
    public static Vec3 deCasteljau(List<Vec3> controlPoints, double t) {
        // Defensive copy of the points into a mutable array list
        int m = controlPoints.size();
        if (m == 1) return controlPoints.getFirst();
        List<Vec3> tmp = new ArrayList<>(controlPoints);

        // r = 1 .. m-1
        for (int r = 1; r < m; r++) {
            for (int i = 0; i < m - r; i++) {
                Vec3 a = tmp.get(i);
                Vec3 b = tmp.get(i + 1);
                tmp.set(i, a.lerp(b, t));
            }
        }
        return tmp.getFirst();
    }

    /**
     * Resamples a path of Vec3 points based on arc length for even spacing.
     *
     * @param originalPath   The list of points defining the original path.
     * @param sampleDistance The fixed distance between each new sample point.
     * @return A new list of Vec3 objects with points spaced evenly by arc length.
     */
    public static List<Vec3> sampleByArcLength(List<Vec3> originalPath, double sampleDistance) {
        return sampleByArcLength(originalPath, sampleDistance, 0.0, Double.NaN, true);
    }

    /**
     * Sample points along originalPath at fixed arc-length intervals between startDistance and stopDistance.
     *
     * @param originalPath   input vertex polyline (must have >= 2 points)
     * @param sampleDistance desired spacing between samples (must be > 0)
     * @param startDistance  arc-length position to start sampling from (>=0). If NaN uses 0.
     * @param stopDistance   arc-length position to stop sampling at. If NaN or less than 0 uses total length.
     * @param forward        if true sample from start->stop, if false sample from stop->start (result reversed)
     * @return sampled points (starts with exact point at startDistance, ends with exact point at stopDistance)
     */
    public static List<Vec3> sampleByArcLength(List<Vec3> originalPath, double sampleDistance,
                                               double startDistance, double stopDistance, boolean forward) {
        List<Vec3> sampledPoints = new ArrayList<>();
        if (originalPath == null || originalPath.size() < 2 || sampleDistance <= 0) {
            return originalPath == null ? List.of() : new ArrayList<>(originalPath);
        }

        // 1. cumulative arc lengths
        List<Double> cumulativeLengths = new ArrayList<>(originalPath.size());
        cumulativeLengths.add(0.0);
        double totalLength = 0.0;
        for (int i = 1; i < originalPath.size(); i++) {
            totalLength += distance(originalPath.get(i - 1), originalPath.get(i));
            cumulativeLengths.add(totalLength);
        }

        // Normalize start/stop
        double start = Double.isNaN(startDistance) ? 0.0 : Math.max(0.0, startDistance);
        double stop = Double.isNaN(stopDistance) ? totalLength : stopDistance;

        start = Math.min(start, totalLength);
        stop = Math.min(stop, totalLength);

        // If start > stop, swap so range is valid (we'll reverse if forward==false later)
        if (start > stop) {
            double tmp = start;
            start = stop;
            stop = tmp;
        }

        // If user wants reverse direction, we still compute forward samples then reverse result at the end
        // Always include exact start point
        sampledPoints.add(interpolatePointAtArc(originalPath, cumulativeLengths, start));

        // If the sampling range is effectively a single point, return (ensure stop included)
        if (Math.abs(stop - start) < 1e-9) {
            // include stop (same as start) and return
            return sampledPoints;
        }

        double currentArc = start + sampleDistance;
        while (currentArc < stop - 1e-9) { // sample interior points
            sampledPoints.add(interpolatePointAtArc(originalPath, cumulativeLengths, currentArc));
            currentArc += sampleDistance;
        }

        // Always include exact stop point
        sampledPoints.add(interpolatePointAtArc(originalPath, cumulativeLengths, stop));

        // If the caller asked for reverse direction, flip the order
        if (!forward) {
            Collections.reverse(sampledPoints);
        }

        return sampledPoints;
    }

    /**
     * Convenience: sample by start/end points (closest projection on path).
     */
    public static List<Vec3> sampleByArcLength(List<Vec3> originalPath, double sampleDistance,
                                               Vec3 startPoint, Vec3 stopPoint, boolean forward) {
        if (originalPath == null || originalPath.size() < 2) return List.of();
        // build cumulative lengths once
        List<Double> cumulativeLengths = new ArrayList<>(originalPath.size());
        cumulativeLengths.add(0.0);
        double totalLength = 0.0;
        for (int i = 1; i < originalPath.size(); i++) {
            totalLength += distance(originalPath.get(i - 1), originalPath.get(i));
            cumulativeLengths.add(totalLength);
        }

        double startArc = findClosestArcLength(originalPath, cumulativeLengths, startPoint);
        double stopArc = findClosestArcLength(originalPath, cumulativeLengths, stopPoint);
        return sampleByArcLength(originalPath, sampleDistance, startArc, stopArc, forward);
    }

    /* --------------------- helpers ---------------------- */

    /**
     * Interpolates a point on the path at the given arc-length (clamped to [0, totalLength]).
     */
    private static Vec3 interpolatePointAtArc(List<Vec3> path, List<Double> cumulativeLengths, double arc) {
        double total = cumulativeLengths.getLast();
        if (arc <= 0.0) return path.getFirst();
        if (arc >= total) return path.getLast();

        // find segment index where cumulativeLengths[idx-1] < arc <= cumulativeLengths[idx]
        int idx = Collections.binarySearch(cumulativeLengths, arc);
        if (idx < 0) idx = -idx - 1;
        // idx is the first index with cumulativeLengths[idx] >= arc
        int startIdx = Math.max(1, idx);
        Vec3 a = path.get(startIdx - 1);
        Vec3 b = path.get(startIdx);
        double segStartArc = cumulativeLengths.get(startIdx - 1);
        double segLen = cumulativeLengths.get(startIdx) - segStartArc;
        if (segLen <= 1e-9) return a; // degenerate segment
        double t = (arc - segStartArc) / segLen;
        // linear interpolation
        return new Vec3(
                a.x + t * (b.x - a.x),
                a.y + t * (b.y - a.y),
                a.z + t * (b.z - a.z)
        );
    }

    /**
     * Finds the arc-length along the path whose projected point is closest to target.
     */
    private static double findClosestArcLength(List<Vec3> path, List<Double> cumulativeLengths, Vec3 target) {
        double bestArc = 0.0;
        double bestDist2 = Double.POSITIVE_INFINITY;

        for (int i = 1; i < path.size(); i++) {
            Vec3 a = path.get(i - 1);
            Vec3 b = path.get(i);
            Vec3 ab = b.subtract(a);
            double segLen = ab.length();
            if (segLen <= 1e-9) {
                double d2 = a.distanceSq(target);
                if (d2 < bestDist2) {
                    bestDist2 = d2;
                    bestArc = cumulativeLengths.get(i - 1);
                }
                continue;
            }
            // project (target - a) onto ab
            Vec3 at = target.subtract(a);
            double t = (ab.dot(at)) / (segLen * segLen);
            double tClamped = Math.max(0.0, Math.min(1.0, t));
            Vec3 proj = new Vec3(a.x + tClamped * ab.x, a.y + tClamped * ab.y, a.z + tClamped * ab.z);
            double d2 = proj.distanceSq(target);
            if (d2 < bestDist2) {
                bestDist2 = d2;
                double arcAtProj = cumulativeLengths.get(i - 1) + tClamped * segLen;
                bestArc = arcAtProj;
            }
        }
        return bestArc;
    }
}

