package org.vicky.vspe.platform.systems.dimension.StructureUtils;

import org.vicky.vspe.platform.systems.dimension.TimeCurve;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.random.RandomGenerator;

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
    public static Function<Double, Double> noised(double startValue,
                                                  double endValue,
                                                  double fadeStart,
                                                  double fadeEnd,
                                                  TimeCurve curve,
                                                  RandomGenerator rnd) {
        return t -> {
            double progress = (t - fadeStart) / (fadeEnd - fadeStart);
            progress = Math.max(0.0, Math.min(1.0, progress));
            double noisedExponent = rnd.nextDouble() * 0.5;

            float eased = (float) (curve.apply(progress) * noisedExponent);

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

    /**
     * Builds a function that smoothly transitions through multiple start→end segments.
     */
    public static Function<Double, Double> multiFade(Segment... segments) {
        return multiFade(Arrays.stream(segments).toList());
    }

    /**
     * Builds a function that smoothly transitions through multiple start→end segments.
     */
    public static Function<Double, Double> multiFade(List<Segment> segments) {
        // Defensive copy, in case the caller mutates later
        List<Segment> segs = new ArrayList<>(segments);

        return t -> {
            for (Segment s : segs) {
                if (s.contains(t)) {
                    double progress = (t - s.fadeStart) / (s.fadeEnd - s.fadeStart);
                    progress = Math.max(0.0, Math.min(1.0, progress));

                    float eased = s.curve.apply(progress);
                    return s.startValue * (1 - eased) + s.endValue * eased;
                }
            }

            // If t is before or after all ranges, clamp to nearest endpoint
            if (!segs.isEmpty()) {
                if (t < segs.getFirst().fadeStart) return segs.getFirst().startValue;
                if (t > segs.getLast().fadeEnd) return segs.getLast().endValue;
            }
            return 0.0;
        };
    }

    public static class Segment {
        public final double startValue;
        public final double endValue;
        public final double fadeStart;
        public final double fadeEnd;
        public final TimeCurve curve;

        public Segment(double startValue, double endValue, double fadeStart, double fadeEnd, TimeCurve curve) {
            this.startValue = startValue;
            this.endValue = endValue;
            this.fadeStart = fadeStart;
            this.fadeEnd = fadeEnd;
            this.curve = curve;
        }

        public boolean contains(double t) {
            return t >= fadeStart && t <= fadeEnd;
        }
    }
}
