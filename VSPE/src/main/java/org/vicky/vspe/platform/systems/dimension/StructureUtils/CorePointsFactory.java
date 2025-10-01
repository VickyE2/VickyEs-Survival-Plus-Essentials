package org.vicky.vspe.platform.systems.dimension.StructureUtils;

import org.vicky.platform.utils.Vec3;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.factories.PointFactory;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.factories.StraightPointFactory;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.FBMGenerator;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.JNoiseNoiseSampler;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.NoiseSampler;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.NoiseSamplerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * CorePointsFactory - modular and parametric.
 * Produces List of Vec3 points along a path.
 */
public class CorePointsFactory {
    // Helper: signed power for sharpness adjustments
    public static double signedPow(double v, double p) {
        return Math.signum(v) * Math.pow(Math.abs(v), p);
    }

    // Generate points
    public static List<Vec3> generate(Params p) {
        if (p == null) throw new IllegalArgumentException("Params must not be null");
        if (p.segments <= 1) throw new IllegalArgumentException("segments must be > 0");

        List<Vec3> out = new ArrayList<>(p.segments);
        JNoiseNoiseSampler noise = new JNoiseNoiseSampler(NoiseSamplerFactory.INSTANCE.create(NoiseSamplerFactory.Type.PERLIN, (it) -> it, p.seed == 0 ? 1234567L : p.seed, p.noiseLowFreq, p.noiseOctaves));
        NoiseSampler hNoise = new FBMGenerator(0x9A);
        Random rand = new Random(p.seed ^ 0x9e3779b97f4a7c15L);

        // divergence state: a lateral vector (x,z) that decays
        double kinkX = 0.0, kinkZ = 0.0;
        double prevNoiseX = 0.0, prevNoiseZ = 0.0, prevNoiseY = 0.0;

        for (int i = 0; i <= p.segments; i++) {
            double t = (double) i / p.segments;

            var generated = p.type.createFor(t, p);
            double x = generated.getX(), z = generated.getZ(), y = generated.getIntY();
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
                System.err.println("warning: archetype returned non-finite coords at t=" + t + " — replacing with 0");
                if (!Double.isFinite(x)) x = 0.0;
                if (!Double.isFinite(y)) y = 0.0;
                if (!Double.isFinite(z)) z = 0.0;
            }

            // --- sample & center noise to [-1,1] ---
            // low frequency (drift) and high frequency (detail)
            double lowX = safeSample1D(noise, t + 12.345, () -> System.err.println("warning: invalid lowNoise sample produced; treating as 0: x"));
            double lowZ = safeSample1D(noise, t + 98.765, () -> System.err.println("warning: invalid lowNoise sample produced; treating as 0: y"));
            double lowY = safeSample1D(noise, t + 55.555, () -> System.err.println("warning: invalid lowNoise sample produced; treating as 0: z"));

            double highX = safeSample1D(hNoise, t + 12.345,
                    () -> System.err.println("warning: invalid hNoise sample produced; treating as 0: x"));
            double highZ = safeSample1D(hNoise, t + 98.765,
                    () -> System.err.println("warning: invalid hNoise sample produced; treating as 0: y"));
            double highY = safeSample1D(hNoise, t + 55.555,
                    () -> System.err.println("warning: invalid hNoise sample produced; treating as 0: z"));

            // --- mix bands: drift gets some fraction, detail gets rest ---
            double driftFrac = p.noiseDriftStrength;            // e.g. 0.25
            double detailFrac = 1.0 - driftFrac;

            double mixedX = (lowX * driftFrac) + (highX * detailFrac);
            double mixedZ = (lowZ * driftFrac) + (highZ * detailFrac);
            double mixedY = (lowY * driftFrac) + (highY * detailFrac);

            // --- compute amplitude scaling, then clamp to an absolute max ---
            double widthScale = p.width * p.noiseWidthFactor;    // relative contribution from width
            double relAmplitude = p.noiseStrength * widthScale; // what you had before
            double absMax = p.noiseAbsoluteMax;                  // hard clamp in world units

            // final amplitude = clamp(relAmplitude, -absMax..absMax) but relAmplitude >= 0 so:
            double amplitude = Math.min(relAmplitude, absMax);

            // target offsets (centered) in world units
            double targetOffsetX = mixedX * amplitude;
            double targetOffsetZ = mixedZ * amplitude;
            double targetOffsetY = mixedY * Math.min(p.noiseStrength * p.height, p.noiseAbsoluteMax); // vertical uses height clamp

            // --- smooth toward the target to avoid jaggies ---
            double smooth = clamp01(p.noiseSmooth); // 0..1
            double appliedNoiseX = lerp(prevNoiseX, targetOffsetX, 1.0 - Math.pow(1.0 - smooth, 1.0));
            double appliedNoiseZ = lerp(prevNoiseZ, targetOffsetZ, 1.0 - Math.pow(1.0 - smooth, 1.0));
            double appliedNoiseY = lerp(prevNoiseY, targetOffsetY, 1.0 - Math.pow(1.0 - smooth, 1.0));

            // store prev for next step
            prevNoiseX = appliedNoiseX;
            prevNoiseZ = appliedNoiseZ;
            prevNoiseY = appliedNoiseY;

            // --- divergence/kinks (unchanged) ---
            double divergenceStep = p.height / p.segments;
            if (rand.nextDouble() < p.divergenceProbability) {
                double ang = rand.nextDouble() * Math.PI * 2.0;
                kinkX += Math.cos(ang) * p.divergenceStrength * Math.min(p.width, p.noiseAbsoluteMax);
                kinkZ += Math.sin(ang) * p.divergenceStrength * Math.min(p.width, p.noiseAbsoluteMax);
            }
            double appliedKinkX = kinkX;
            kinkX *= p.divergenceDecay;
            double appliedKinkZ = kinkZ;
            kinkZ *= p.divergenceDecay;

            // --- combine offsets into final position ---
            double finalX = x + appliedNoiseX + appliedKinkX;
            double finalZ = z + appliedNoiseZ + appliedKinkZ;
            double finalY = y + appliedNoiseY;
            if (!Double.isFinite(finalX) || !Double.isFinite(finalY) || !Double.isFinite(finalZ)) {
                System.err.println("warning: final point contained non-finite values; replacing with 0 (t=" + t + ")");
                finalX = Double.isFinite(finalX) ? finalX : 0.0;
                finalY = Double.isFinite(finalY) ? finalY : 0.0;
                finalZ = Double.isFinite(finalZ) ? finalZ : 0.0;
            }
            Vec3 pt = new Vec3(finalX, finalY, finalZ);
            Vec3 rotated = rotateByYawPitch(pt.getIntX(), pt.getIntY(), pt.getIntZ(), p.yawDegrees, p.pitchDegrees);
            if (p.rotationOrigin != null) {
                out.add(rotated.add(p.rotationOrigin));
            } else {
                out.add(rotated);
            }
        }
        // System.out.println(out.size());
        return out;
    }

    private static double clamp01(double v) {
        if (v <= 0) return 0;
        if (v >= 1) return 1;
        return v;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double safeSample1D(NoiseSampler sampler, double coord, Runnable onInvalid) {
        double v;
        try {
            // call the 1D sample; if your sampler API is different change this back to sample1D(...)
            v = sampler.sample1D(coord);
        } catch (Throwable ex) {
            // sampler threw — treat as zero and warn
            if (onInvalid != null) onInvalid.run();
            return 0.0;
        }
        if (!Double.isFinite(v)) {
            if (onInvalid != null) onInvalid.run();
            return 0.0;
        }
        return v;
    }

    public static Vec3 rotateAroundOrigin(Vec3 pt, Vec3 origin, double yawDeg, double pitchDeg) {
        // translate so origin -> (0,0,0)
        double lx = pt.getIntX() - origin.getIntX();
        double ly = pt.getIntY() - origin.getIntY();
        double lz = pt.getIntZ() - origin.getIntZ();

        // rotate in origin-space
        Vec3 r = rotateByYawPitch(lx, ly, lz, yawDeg, pitchDeg);

        // translate back
        return new Vec3(r.getIntX() + origin.getIntX(), r.getIntY() + origin.getIntY(), r.getIntZ() + origin.getIntZ());
    }

    /**
     * Rotate a local point by pitch (X axis) then yaw (Y axis).
     * yawDeg and pitchDeg are in degrees.
     * <p>
     * Convention:
     * - pitch: rotation about X (positive = tilt so +Y moves toward +Z)
     * - yaw:   rotation about Y (positive = rotate around up axis)
     * <p>
     * Order: pitch -> yaw
     */
    private static Vec3 rotateByYawPitch(double x, double y, double z, double yawDeg, double pitchDeg) {
        double pitch = Math.toRadians(pitchDeg);
        double yaw = Math.toRadians(yawDeg);

        // Pitch rotation around X:
        double cosP = Math.cos(pitch), sinP = Math.sin(pitch);
        double y1 = cosP * y - sinP * z;
        double z1 = sinP * y + cosP * z;
        double x1 = x;

        // Yaw rotation around Y (use same convention as your other rotateAroundY)
        double cosY = Math.cos(yaw), sinY = Math.sin(yaw);
        double x2 = x1 * cosY - z1 * sinY;
        double z2 = x1 * sinY + z1 * cosY;
        double y2 = y1;

        return new Vec3(x2, y2, z2);
    }

    /**
     * small utility to guard NaN/Inf -> 0
     */
    private static double safeFinite(double v) {
        return Double.isFinite(v) ? v : 0.0;
    }

    public static class Params {
        // size
        public double height = 15.0;
        public double width = 5.0;          // base width scale (affects lateral amplitude)
        public int segments = 10;

        // archetype
        public PointFactory type = new StraightPointFactory();
        public double noiseStrength = 0.0;
        public long seed = 0;
        public int noiseOctaves = 3;
        public double divergenceProbability = 0.0;
        public double divergenceStrength = 0.8;
        public double divergenceDecay = 0.85;
        public double baseRadius = 0.5, topRadius = 0.15;

        public double noiseWidthFactor = 1.0;     // multiply with p.width to produce width-based scale
        public double noiseAbsoluteMax = 10.0;     // hard clamp in world units (max lateral offset)
        public double noiseLowFreq = 0.5;         // multiplier for low-frequency band (smaller freq -> slower drift)
        public double noiseHighFreq = 3.0;        // multiplier for high-frequency detail
        public double noiseDriftStrength = 0.57;  // fraction of noiseStrength allocated to drift
        public double noiseSmooth = 0.6;

        public double yawDegrees = 0.0;
        public double pitchDegrees = 0.0;
        public Vec3 rotationOrigin = null;

        /**
         * Create a fresh Builder with default values
         */
        public static Builder builder() {
            return new Builder();
        }

        // helpers
        public Params copy() {
            Params p = new Params();
            p.height = height;
            p.width = width;
            p.segments = segments;
            p.type = type.copy();
            p.seed = seed;
            p.noiseStrength = noiseStrength;
            p.noiseOctaves = noiseOctaves;
            p.divergenceProbability = divergenceProbability;
            p.divergenceStrength = divergenceStrength;
            p.divergenceDecay = divergenceDecay;
            p.baseRadius = baseRadius;
            p.topRadius = topRadius;
            p.noiseWidthFactor = noiseWidthFactor;
            p.noiseAbsoluteMax = noiseAbsoluteMax;
            p.noiseLowFreq = noiseLowFreq;
            p.noiseHighFreq = noiseHighFreq;
            p.noiseDriftStrength = noiseDriftStrength;
            p.noiseSmooth = noiseSmooth;
            p.yawDegrees = yawDegrees;
            p.pitchDegrees = pitchDegrees;
            p.rotationOrigin = rotationOrigin;

            return p;
        }

        /**
         * Create a builder initialized from this Params instance
         */
        public Builder toBuilder() {
            return new Builder().from(this);
        }

        public static final class Builder {
            public Vec3 rotationOrigin = null;
            private double height = 15.0;
            private double width = 5.0;
            private int segments = 10;
            private PointFactory type = new StraightPointFactory();
            private double noiseStrength = 0.2;
            private long seed = 0L;
            private int noiseOctaves = 3;
            private double divergenceProbability = 0.02;
            private double divergenceStrength = 0.8;
            private double divergenceDecay = 0.85;
            private double baseRadius = 0.5;
            private double topRadius = 0.15;
            private double noiseWidthFactor = 1.0;     // multiply with p.width to produce width-based scale
            private double noiseAbsoluteMax = 10.0;     // hard clamp in world units (max lateral offset)
            private double noiseLowFreq = 0.5;         // multiplier for low-frequency band (smaller freq -> slower drift)
            private double noiseHighFreq = 3.0;        // multiplier for high-frequency detail
            private double noiseDriftStrength = 0.25;  // fraction of noiseStrength allocated to drift
            private double noiseSmooth = 0.6;
            private double yawDegrees = 0.0;
            private double pitchDegrees = 0.0;

            public Builder() {
            }

            // --- fluent setters ---
            public Builder height(double height) {
                this.height = height;
                return this;
            }

            public Builder width(double width) {
                this.width = width;
                return this;
            }

            public Builder segments(int segments) {
                this.segments = segments;
                return this;
            }

            /**
             * Supply an instance of PointFactory (builder will copy it during build).
             */
            public Builder type(PointFactory type) {
                this.type = (type == null) ? new StraightPointFactory() : type;
                return this;
            }

            public Builder noiseStrength(double s) {
                this.noiseStrength = s;
                return this;
            }

            public Builder seed(long seed) {
                this.seed = seed;
                return this;
            }

            public Builder noiseOctaves(int oct) {
                this.noiseOctaves = oct;
                return this;
            }

            public Builder noiseWidthFactor(double widthFactor) {
                this.noiseWidthFactor = widthFactor;
                return this;
            }

            public Builder divergenceProbability(double p) {
                this.divergenceProbability = p;
                return this;
            }

            public Builder noiseAbsoluteMax(double absoluteMax) {
                this.noiseAbsoluteMax = absoluteMax;
                return this;
            }

            public Builder divergenceStrength(double s) {
                this.divergenceStrength = s;
                return this;
            }

            public Builder noiseLowFreq(double freq) {
                this.noiseLowFreq = freq;
                return this;
            }

            public Builder divergenceDecay(double d) {
                this.divergenceDecay = d;
                return this;
            }

            public Builder noiseHighFreq(double freq) {
                this.noiseHighFreq = freq;
                return this;
            }

            public Builder baseRadius(double r) {
                this.baseRadius = r;
                return this;
            }

            public Builder noiseDriftStrength(double str) {
                this.noiseDriftStrength = str;
                return this;
            }

            public Builder topRadius(double r) {
                this.topRadius = r;
                return this;
            }

            public Builder noiseSmooth(double smooth) {
                this.noiseSmooth = smooth;
                return this;
            }

            public Builder yawDegrees(double d) {
                this.yawDegrees = d;
                return this;
            }

            public Builder pitchDegrees(double d) {
                this.pitchDegrees = d;
                return this;
            }

            public Builder origin(Vec3 o) {
                this.rotationOrigin = o;
                return this;
            }

            /**
             * Initialize builder fields from an existing Params instance
             */
            public Builder from(Params src) {
                if (src == null) return this;
                this.height = src.height;
                this.width = src.width;
                this.segments = src.segments;
                // copy the factory so changes to it don't mutate the source
                this.type = (src.type == null) ? new StraightPointFactory() : src.type.copy();
                this.noiseStrength = src.noiseStrength;
                this.seed = src.seed;
                this.noiseOctaves = src.noiseOctaves;
                this.divergenceProbability = src.divergenceProbability;
                this.divergenceStrength = src.divergenceStrength;
                this.divergenceDecay = src.divergenceDecay;
                this.baseRadius = src.baseRadius;
                this.topRadius = src.topRadius;
                this.noiseWidthFactor = src.noiseWidthFactor;
                this.noiseAbsoluteMax = src.noiseAbsoluteMax;
                this.noiseLowFreq = src.noiseLowFreq;
                this.noiseHighFreq = src.noiseHighFreq;
                this.noiseDriftStrength = src.noiseDriftStrength;
                this.noiseSmooth = src.noiseSmooth;
                this.yawDegrees = src.yawDegrees;
                this.pitchDegrees = src.pitchDegrees;
                this.rotationOrigin = src.rotationOrigin;
                return this;
            }

            /**
             * Validate (lightweight) before building; throw IllegalArgumentException on critical bad values
             */
            private void validate() {
                if (segments <= 0) throw new IllegalArgumentException("segments must be > 0");
                if (height <= 0) throw new IllegalArgumentException("height must be > 0");
                if (width <= 0) throw new IllegalArgumentException("width must be > 0");
                if (noiseOctaves < 1) noiseOctaves = 1;
                if (type == null) type = new StraightPointFactory();
            }

            /**
             * Build a Params instance. The PointFactory is copied to preserve immutability-ish behavior.
             */
            public Params build() {
                validate();
                Params p = new Params();
                p.height = this.height;
                p.width = this.width;
                p.segments = this.segments;
                // defensive copy of the factory so the built Params owns its own factory instance
                p.type = (this.type == null) ? new StraightPointFactory() : this.type.copy();
                p.noiseStrength = this.noiseStrength;
                p.seed = this.seed;
                p.noiseOctaves = this.noiseOctaves;
                p.divergenceProbability = this.divergenceProbability;
                p.divergenceStrength = this.divergenceStrength;
                p.divergenceDecay = this.divergenceDecay;
                p.baseRadius = this.baseRadius;
                p.topRadius = this.topRadius;
                p.noiseWidthFactor = this.noiseWidthFactor;
                p.noiseAbsoluteMax = this.noiseAbsoluteMax;
                p.noiseLowFreq = this.noiseLowFreq;
                p.noiseHighFreq = this.noiseHighFreq;
                p.noiseDriftStrength = this.noiseDriftStrength;
                p.noiseSmooth = this.noiseSmooth;
                p.yawDegrees = this.yawDegrees;
                p.pitchDegrees = this.pitchDegrees;
                p.rotationOrigin = this.rotationOrigin;
                return p;
            }
        }
    }
}
