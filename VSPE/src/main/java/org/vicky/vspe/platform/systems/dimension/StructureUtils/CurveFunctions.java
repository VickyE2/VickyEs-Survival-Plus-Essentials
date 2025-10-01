package org.vicky.vspe.platform.systems.dimension.StructureUtils;

import org.vicky.vspe.platform.systems.dimension.TimeCurve;

import java.util.function.Function;

public class CurveFunctions {
    /**
     * Creates a function that interpolates between startValue and endValue,
     * applied over [fadeStart, fadeEnd] in normalized t (0..1).
     *
     * @param startValue the value at fadeStart
     * @param endValue   the value at fadeEnd
     * @param fadeStart  where fading begins (0..1)
     * @param fadeEnd    where fading ends   (0..1)
     * @param curve      easing curve to apply to progress
     */
    public static Function<Double, Double> fade(double startValue,
                                                double endValue,
                                                double fadeStart,
                                                double fadeEnd,
                                                TimeCurve curve) {
        return t -> {
            double progress = (t - fadeStart) / (fadeEnd - fadeStart);
            progress = Math.max(0.0, Math.min(1.0, progress));

            float eased = curve.apply(progress);

            return startValue * (1 - eased) + endValue * eased;
        };
    }

    /**
     * Creates a function that interpolates between startValue and endValue,
     * applied over [fadeStart, fadeEnd] in normalized t (0..1).
     *
     * @param startValue the value at fadeStart
     * @param endValue   the value at fadeEnd
     * @param fadeStart  where fading begins (0..1)
     * @param fadeEnd    where fading ends   (0..1)
     * @param curve      easing curve to apply to progress
     */
    public static Function<Double, Double> invertFade(double startValue,
                                                      double endValue,
                                                      double fadeStart,
                                                      double fadeEnd,
                                                      TimeCurve curve) {
        return t -> {
            double progress = (t - fadeStart) / (fadeEnd - fadeStart);
            progress = Math.max(0.0, Math.min(1.0, progress));

            float eased = curve.apply(progress);

            return 1.00 - (startValue * (1 - eased) + endValue * eased);
        };
    }

    /**
     * Shortcut for radius fade
     */
    public static Function<Double, Double> radius(double startRadius,
                                                  double endRadius,
                                                  double fadeStart,
                                                  double fadeEnd,
                                                  TimeCurve curve) {
        return fade(startRadius, endRadius, fadeStart, fadeEnd, curve);
    }

    /**
     * Shortcut for radius fade
     */
    public static Function<Double, Double> radius(double startRadius,
                                                  double endRadius,
                                                  double fadeStart,
                                                  double fadeEnd,
                                                  TimeCurve curve,
                                                  boolean invert) {
        if (invert) return invertFade(startRadius, endRadius, fadeStart, fadeEnd, curve);
        else return fade(startRadius, endRadius, fadeStart, fadeEnd, curve);
    }

    /**
     * Shortcut for pitch fade
     */
    public static Function<Double, Double> pitch(double startPitch,
                                                 double endPitch,
                                                 double fadeStart,
                                                 double fadeEnd,
                                                 TimeCurve curve) {
        return fade(startPitch, endPitch, fadeStart, fadeEnd, curve);
    }
}
