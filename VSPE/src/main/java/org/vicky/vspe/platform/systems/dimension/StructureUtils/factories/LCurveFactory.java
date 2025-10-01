package org.vicky.vspe.platform.systems.dimension.StructureUtils.factories;

import org.vicky.platform.utils.Vec3;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.CorePointsFactory;

/**
 * L-curve factory with explicit target plane and stronger corner control.
 */
public class LCurveFactory implements PointFactory {
    private final double cornerAngleDegrees; // 0..90
    private final Plane plane;
    private final double sharpness; // 0.0 (soft) .. 1.0 (very sharp)

    public LCurveFactory(double cornerAngleDegrees) {
        this(cornerAngleDegrees, Plane.XY, 1.0);
    }

    public LCurveFactory(double cornerAngleDegrees, Plane plane, double sharpness) {
        if (Double.isNaN(cornerAngleDegrees)) cornerAngleDegrees = 0.0;
        this.cornerAngleDegrees = Math.max(0.0, Math.min(90.0, cornerAngleDegrees));
        this.plane = (plane == null) ? Plane.XY : plane;
        this.sharpness = Math.max(0.0, Math.min(2.0, sharpness)); // allow >1 to exaggerate
    }

    @Override
    public PointFactory copy() {
        return new LCurveFactory(cornerAngleDegrees, plane, sharpness);
    }

    @Override
    public Vec3 createFor(double t, CorePointsFactory.Params p) {
        t = Math.max(0.0, Math.min(1.0, t));
        double w = p.width;
        double h = p.height;

        double cornerness = Math.pow(cornerAngleDegrees / 90.0, 0.5) * sharpness;
        cornerness = Math.max(0.0, Math.min(1.0, cornerness));

        // Arc (more pronounced by using slightly different paramization)
        double sweepRad = Math.toRadians(cornerAngleDegrees);
        double theta = t * sweepRad;

        double arcX = (1.0 - Math.cos(theta)) * w;
        double arcY = Math.sin(theta) * h;

        // Step / L
        double stepT = t * 2.0;
        double stepX, stepY;
        if (stepT <= 1.0) {
            stepX = stepT * w;
            stepY = 0.0;
        } else {
            stepX = w;
            stepY = (stepT - 1.0) * h;
        }

        // Soft smoothing around midpoint but scaled by (1 - cornerness) so higher cornerness keeps it sharp.
        double smoothFrac = 0.06 * (1.0 - cornerness); // more cornerness -> smaller smoothing
        if (smoothFrac > 1e-6) {
            double mid = 0.5;
            double d = (t - mid) / smoothFrac;
            double s = 0.5 * (1.0 + Math.tanh(d * 3.0));
            double leftX = Math.min(w, (t / 0.5) * w);
            double rightY = Math.max(0.0, ((t - 0.5) / 0.5) * h);
            double softX = (1.0 - s) * leftX + s * w;
            double softY = (1.0 - s) * 0.0 + s * rightY;
            stepX = softX;
            stepY = softY;
        }

        // Blend arc vs step using cornerness
        double x;
        double y;
        double blend = Math.sin(Math.toRadians(cornerAngleDegrees));
        x = blend * arcX + (1 - blend) * stepX;
        y = blend * arcY + (1 - blend) * stepY;

        // z depends on chosen plane: XY -> (x,y,0) upright; XZ -> (x,0,y) flat.
        if (plane == Plane.XY) {
            return new Vec3(x, y, 0.0);
        } else {
            // XZ plane (horizontal L)
            return new Vec3(x, 0.0, y);
        }
    }

    public enum Plane {XY, XZ}
}
