package org.vicky.vspe.platform.utilities;

import org.vicky.platform.utils.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class FunctionCurveConverter {

    /**
     * Converts a mathematical function y = f(x) into a curve represented as a list of Vec3 points.
     * The curve is generated on the X-Y plane at a specific Z level.
     *
     * @param function The function mapping x to y.
     * @param startX   The starting x value.
     * @param endX     The ending x value.
     * @param steps    The number of points to generate (resolution).
     * @param z        The z-coordinate for the curve.
     * @return A list of Vec3 points.
     */
    public static List<Vec3> toCurve(Function<Double, Double> function, double startX, double endX, int steps, double z) {
        List<Vec3> points = new ArrayList<>();
        if (steps <= 1) return points;

        double stepSize = (endX - startX) / (steps - 1);

        for (int i = 0; i < steps; i++) {
            double y = startX + (i * stepSize);
            double x = endX * function.apply(1.0 - (i + 0.0) / steps);

            // Casting to float ensures compatibility if Vec3 uses float components
            points.add(new Vec3((float) -x, (float) (double) y, (float) z));
        }
        return points;
    }
}
