package org.vicky.vspe.platform.systems.dimension.StructureUtils.spiralutilsdecorators;

import org.vicky.platform.utils.Vec3;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.SpiralUtil;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.SeededRandomSource;
import org.vicky.vspe.platform.utilities.CircleDivider;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

public class NoodleSpline implements SpiralUtil.MultiStrandedCurveDecoration {
    public enum Mode { SPIRAL, BRAID, NONE }

    private final List<CircleDivider.Circle> layout; // cached
    private final Map<Integer, double[]> persistentOffsets = new HashMap<>();
    private final double noiseFreq;
    private final double noiseSmoothness;
    private final long noiseSeed;
    private final double majorCircleRadius;
    private final double minDistanceFromNoodles;
    private final double noiseJitter;
    private final int bundleSize;        // micro-strands per layout center (>=1)
    private final int baseStrandIndex;

    private final Mode mode;
    private final double twistsPerUnit;
    private final double braidPitch;
    private final double braidPhaseShift;

    private NoodleSpline(Builder builder) {
        double strandRadius = builder.strandRadius;
        int maxCircleCount = builder.maxCircleCount;

        this.minDistanceFromNoodles = builder.minDistanceFromNoodles;
        this.noiseFreq = builder.noiseFreq;
        this.noiseSmoothness = builder.noiseSmoothness;
        this.noiseSeed = builder.noiseSeed;
        this.majorCircleRadius = builder.majorCircleRadius;
        this.noiseJitter = builder.noiseJitter;
        this.bundleSize = Math.max(1, builder.bundleSize);
        this.baseStrandIndex = Math.max(0, builder.baseStrandIndex);

        this.mode = builder.mode;
        this.twistsPerUnit = builder.twistsPerUnit;
        this.braidPitch = builder.braidPitch;
        this.braidPhaseShift = builder.braidPhaseShift;

        this.layout = Collections.unmodifiableList(
                CircleDivider.packBySpacing(
                        majorCircleRadius,
                        strandRadius,
                        minDistanceFromNoodles,
                        maxCircleCount
                )
        );
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    // cheap deterministic pseudo-random double in [-1,1] from integer coords and seed
    private static double hashToUnit(long seed, long x) {
        long z = x * 0x9E3779B97F4A7C15L + seed;
        z = (z ^ (z >> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >> 27)) * 0x94D049BB133111EBL;
        z = z ^ (z >> 31);
        // map to [-1,1]
        return ((z >>> 1) / (double)(1L << 62)) * 2.0 - 1.0;
    }

    // value noise 1D: interpolate between hashed lattice points smoothly
    private double valueNoise1D(long seed, double t) {
        long x0 = (long) Math.floor(t);
        double f = t - x0;
        double v0 = hashToUnit(seed, x0);
        double v1 = hashToUnit(seed, x0 + 1);
        double u = fade(f);
        return v0 * (1.0 - u) + v1 * u;
    }

    // 2D coherent noise vector (two independent 1D noises with offset seeds)
    private double[] noiseVec(long baseSeed, double t) {
        double nx = valueNoise1D(baseSeed ^ 0x9E3779B97F4A7C15L, t * noiseFreq);
        double ny = valueNoise1D(baseSeed ^ 0xC2B2AE3D27D4EB4FL, t * (noiseFreq * 1.03)); // slight freq offset
        return new double[]{nx, ny};
    }

    @Override
    public Set<SpiralUtil.PWR> generateMulti(double progress, double phase, double radius, double thickness, int step) {
        Set<SpiralUtil.PWR> out = new LinkedHashSet<>();
        if (layout.isEmpty()) return out;

        int centerIndex = 0;
        for (CircleDivider.Circle c : layout) {
            double xr = c.center.x;
            double yr = c.center.y;

            // rotate by phase (no layout scaling — user asked not to)
            double cos = Math.cos(phase),
                   sin = Math.sin(phase);
            double x = cos * xr - sin * yr;
            double y = sin * xr + cos * yr;

            // jitter in layout units
            if (noiseJitter > 1e-9) {
                double[] nv = noiseVec(noiseSeed + centerIndex * 0x9E3779B97F4A7C15L, progress); // in [-1..1]
                double targetX = nv[0];
                double targetY = nv[1];

                double amplitude = Math.max(minDistanceFromNoodles, c.radius) * noiseJitter; // e.g. 0.2 -> 20% of radius
                targetX *= amplitude;
                targetY *= amplitude;

                double[] cur = persistentOffsets.computeIfAbsent(centerIndex, k -> new double[]{0.0, 0.0});

                double alpha = 1.0 - Math.exp(-Math.max(1e-6, 60.0 / Math.max(1e-6, noiseSmoothness)));
                cur[0] = cur[0] + (targetX - cur[0]) * alpha;
                cur[1] = cur[1] + (targetY - cur[1]) * alpha;
                x += cur[0];
                y += cur[1];
            }

            // produce micro-strands
            double padding = 0.0001; // tiny safety so strand doesn't poke out
            double maxOffset = Math.max(0.0, c.radius - radius - padding);

            for (int b = 0; b < bundleSize; b++) {
                double baseAngleLocal = 2.0 * Math.PI * b / bundleSize;

                // --- radial placement: uniform-in-area ---
                double u = (b + 0.5) / (double) bundleSize;         // evenly spaced samples in [0,1]
                double radial = maxOffset * Math.sqrt(u);          // uniform area mapping
                // alternative: concentric ring logic could go here instead

                // --- angular (twist) ---
                double spiralAngle;
                double localZ;

                if (mode == Mode.SPIRAL) {
                    spiralAngle = baseAngleLocal + 2.0 * Math.PI * twistsPerUnit * progress;
                    localZ = 0.0;
                } else if (mode == Mode.BRAID) { // BRAID
                    double strandPhaseShift = (b % 2 == 0) ? 0.0 : braidPhaseShift;
                    spiralAngle = baseAngleLocal + 2.0 * Math.PI * twistsPerUnit * progress + strandPhaseShift;
                    // axial offset so strands cross; you can tune braidPitch
                    // small per-strand axial jitter so not all micro-bundles align
                    double perStrandAxial = (b / (double)bundleSize - 0.5) * (maxOffset * 0.2);
                    localZ = progress * braidPitch + perStrandAxial;
                }
                else {
                    spiralAngle = 0.0;
                    localZ = 0.0;
                }

                double bx = x + Math.cos(spiralAngle) * radial;
                double by = y + Math.sin(spiralAngle) * radial;

                int id = baseStrandIndex + centerIndex * bundleSize + b;

                // use stored strandRadius (NOT the spiral radius)
                Vec3 local = new Vec3(bx, localZ, by); // (right, up, forward) per your mapping
                out.add(new SpiralUtil.PWR(local, Math.min(c.radius, radius), false, id));
            }

            centerIndex++;
        }

        return out;
    }

    public static Builder newBuilder() {
        return new Builder();
    }


    /**
     * {@code NoodleSpline} builder static inner class.
     */
    public static final class Builder {
        public int baseStrandIndex = 0;
        public int bundleSize = 1;
        private double strandRadius = 2;
        private double majorCircleRadius = 10;
        private int maxCircleCount = 10;
        private double noiseJitter = 0.00;
        private double minDistanceFromNoodles = 0.0;
        public Mode mode = Mode.NONE;
        public double twistsPerUnit = 0;
        public double braidPitch = 0.2;
        public double braidPhaseShift = 0.3;
        public double noiseFreq = 1.0;
        public double noiseSmoothness = 8.0;
        public long noiseSeed = 0xC0FFEE12345L;

        private Builder() {
        }

        @Nonnull public Builder setNoiseFreq(double f) { this.noiseFreq = f; return this; }
        @Nonnull public Builder setNoiseSmoothness(double s) { this.noiseSmoothness = s; return this; }
        @Nonnull public Builder setNoiseSeed(long seed) { this.noiseSeed = seed; return this; }

        @Nonnull
        public Builder setBraidPitch(double braidPitch) {
            this.braidPitch = braidPitch;
            return this;
        }

        @Nonnull
        public Builder setBraidPhaseShift(double braidPhaseShift) {
            this.braidPhaseShift = braidPhaseShift;
            return this;
        }

        @Nonnull
        public Builder setTwistsPerUnit(double twistsPerUnit) {
            this.twistsPerUnit = twistsPerUnit;
            return this;
        }

        @Nonnull
        public Builder setMode(Mode mode) {
            this.mode = mode;
            return this;
        }

        /**
         * Sets the {@code baseStrandIndex} and returns a reference to this Builder enabling method chaining.
         *
         * @param baseStrandIndex the {@code baseStrandIndex} to set
         * @return a reference to this Builder
         */
        @Nonnull
        public Builder setBaseStrandIndex(int baseStrandIndex) {
            this.baseStrandIndex = baseStrandIndex;
            return this;
        }

        /**
         * Sets the {@code bundleSize} and returns a reference to this Builder enabling method chaining.
         *
         * @param bundleSize the {@code bundleSize} to set
         * @return a reference to this Builder
         */
        @Nonnull
        public Builder setBundleSize(int bundleSize) {
            this.bundleSize = bundleSize;
            return this;
        }

        /**
         * Sets the {@code strandRadius} and returns a reference to this Builder enabling method chaining.
         *
         * @param strandRadius the {@code strandRadius} to set
         * @return a reference to this Builder
         */
        @Nonnull
        public Builder setStrandRadius(double strandRadius) {
            this.strandRadius = strandRadius;
            return this;
        }

        /**
         * Sets the {@code majorCircleRadius} and returns a reference to this Builder enabling method chaining.
         *
         * @param majorCircleRadius the {@code majorCircleRadius} to set
         * @return a reference to this Builder
         */
        @Nonnull
        public Builder setMajorCircleRadius(double majorCircleRadius) {
            this.majorCircleRadius = majorCircleRadius;
            return this;
        }

        /**
         * Sets the {@code maxCircleCount} and returns a reference to this Builder enabling method chaining.
         *
         * @param maxCircleCount the {@code maxCircleCount} to set
         * @return a reference to this Builder
         */
        @Nonnull
        public Builder setMaxCircleCount(int maxCircleCount) {
            this.maxCircleCount = maxCircleCount;
            return this;
        }

        /**
         * Sets the {@code noiseJitter} and returns a reference to this Builder enabling method chaining.
         *
         * @param noiseJitter the {@code noiseJitter} to set
         * @return a reference to this Builder
         */
        @Nonnull
        public Builder setNoiseJitter(double noiseJitter) {
            this.noiseJitter = noiseJitter;
            return this;
        }

        /**
         * Sets the {@code minDistanceFromNoodles} and returns a reference to this Builder enabling method chaining.
         *
         * @param minDistanceFromNoodles the {@code minDistanceFromNoodles} to set
         * @return a reference to this Builder
         */
        @Nonnull
        public Builder setMinDistanceFromNoodles(double minDistanceFromNoodles) {
            this.minDistanceFromNoodles = minDistanceFromNoodles;
            return this;
        }

        /**
         * Returns a {@code NoodleSpline} built from the parameters previously set.
         *
         * @return a {@code NoodleSpline} built with parameters of this {@code NoodleSpline.Builder}
         */
        @Nonnull
        public NoodleSpline build() {
            return new NoodleSpline(this);
        }
    }
}
