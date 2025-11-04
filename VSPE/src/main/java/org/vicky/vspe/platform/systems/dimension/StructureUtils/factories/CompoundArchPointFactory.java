package org.vicky.vspe.platform.systems.dimension.StructureUtils.factories;

import org.vicky.platform.utils.Vec3;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.CorePointsFactory;

import java.util.ArrayList;
import java.util.List;

public class CompoundArchPointFactory implements PointFactory {
    private final Vec3 start;
    private final Vec3 end;
    private final double height;
    private final double secondaryRatio;
    private final List<Vec3> arc;

    public CompoundArchPointFactory(Vec3 start, Vec3 end, double height) {
        this(start, end, height, 0.3);
    }

    public CompoundArchPointFactory(Vec3 start, Vec3 end, double height, double secondaryRatio) {
        this.start = start;
        this.end = end;
        this.height = height;
        this.secondaryRatio = secondaryRatio;
        this.arc = generateCompoundArc(start, end, height, secondaryRatio, 50);
    }

    private static List<Vec3> generateCompoundArc(Vec3 p1, Vec3 p2, double height, double secondaryRatio, int steps) {

        // --- Primary arc ---
        List<Vec3> mainArc = generateArc(p1, p2, height, steps);
        List<Vec3> arcPoints = new ArrayList<>(mainArc);

        // --- Compute small reverse arc ---
        Vec3 endOfMain = mainArc.getLast();
        Vec3 beforeEnd = mainArc.get(mainArc.size() - 2);
        Vec3 tangent = endOfMain.subtract(beforeEnd).normalize();

        double subLength = p2.subtract(p1).length() * secondaryRatio;
        Vec3 subEnd = endOfMain.add(tangent.multiply(subLength));
        double subHeight = -height * secondaryRatio; // opposite bulge

        List<Vec3> smallArc = generateArc(endOfMain, subEnd, subHeight, (int) (steps * secondaryRatio));

        // append small arc but skip first point to avoid duplication
        for (int i = 1; i < smallArc.size(); i++) {
            arcPoints.add(smallArc.get(i));
        }

        return arcPoints;
    }

    /**
     * Generates a single arch between two points, with height controlling bulge direction.
     */
    private static List<Vec3> generateArc(Vec3 p1, Vec3 p2, double height, int steps) {
        List<Vec3> arcPoints = new ArrayList<>(steps);
        Vec3 center = new Vec3(
                (p1.x + p2.x) / 2.0,
                (p1.y + p2.y) / 2.0 + height,
                (p1.z + p2.z) / 2.0
        );

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double x = p1.x + (p2.x - p1.x) * t;
            double y = p1.y + (p2.y - p1.y) * t;
            double z = p1.z + (p2.z - p1.z) * t;

            // Interpolate with simple quadratic curve for stability
            double archY = y + height * Math.sin(Math.PI * t);
            arcPoints.add(new Vec3(x, archY, z));
        }
        return arcPoints;
    }

    @Override
    public PointFactory copy() {
        return new CompoundArchPointFactory(start, end, height, secondaryRatio);
    }

    @Override
    public Vec3 createFor(double progress, CorePointsFactory.Params params) {
        progress = Math.max(0.0, Math.min(1.0, progress));
        int idx = (int) Math.round((arc.size() - 1) * progress);
        return arc.get(idx);
    }
}