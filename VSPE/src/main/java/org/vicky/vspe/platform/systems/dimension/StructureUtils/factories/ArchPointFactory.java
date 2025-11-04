package org.vicky.vspe.platform.systems.dimension.StructureUtils.factories;

import org.vicky.platform.utils.Vec3;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.CorePointsFactory;

import java.util.ArrayList;
import java.util.List;

public class ArchPointFactory implements PointFactory {
    private final Vec3 start;
    private final Vec3 end;
    private final double height; // positive = arch bulges up, negative = arch bulges down
    private final List<Vec3> arc;

    public ArchPointFactory(Vec3 start, Vec3 end, double height) {
        this.start = start;
        this.end = end;
        this.height = height;
        this.arc = generateArc(start, end, 50);
    }

    public static List<Vec3> generateArc(Vec3 p1, Vec3 p2, int steps) {
        Vec3 center = new Vec3(
                (p1.x + p2.x) / 2.0,
                (p1.y + p2.y) / 2.0,
                (p1.z + p2.z) / 2.0
        );
        double R = p2.subtract(p1).length() / 2.0;
        List<Vec3> arcPoints = new ArrayList<>(steps);

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double x = p1.x + (p2.x - p1.x) * t;
            double y = p1.y + (p2.y - p1.y) * t;
            double underSqrt = R * R - (x - center.x) * (x - center.x) - (y - center.y) * (y - center.y);

            double z;
            if (underSqrt >= 0) {
                z = center.z + Math.sqrt(underSqrt);
            } else {
                z = center.z;
            }
            arcPoints.add(new Vec3(x, y, z));
        }

        return arcPoints;
    }

    @Override
    public PointFactory copy() {
        return new ArchPointFactory(start, end, height);
    }

    @Override
    public Vec3 createFor(double progress, CorePointsFactory.Params params) {
        progress = Math.max(0.0, Math.min(1.0, progress));
        return arc.get((int) Math.round((arc.size() - 1) * progress));
    }
}
