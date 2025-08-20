package org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators;

import org.vicky.platform.utils.Vec3;
import org.vicky.platform.world.PlatformBlockState;
import org.vicky.platform.world.PlatformWorld;
import org.vicky.vspe.BlockVec3i;
import org.vicky.vspe.platform.VSPEPlatformPlugin;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.ProceduralStructureGenerator;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.StructureCacheUtils;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.RandomSource;
import org.vicky.vspe.platform.utilities.Math.Vector3;

import java.util.*;

import static java.lang.Math.clamp;

/**
 * Procedural mushroom generator with configurable cap equations, hollow interior, spots and ridges.
 */
public class ProceduralMushroomGenerator<T, N> extends
        ProceduralStructureGenerator<T, N> {
    private final int stemRadius, capRadiusMin, capRadiusMax, capHeightMin, capHeightMax, stemHeightMin, stemHeightMax;
    private final boolean hasSpots, hollowCap;
    private final PlatformBlockState<T> stemMaterial, capMaterial, spotMaterial, ridgeMaterial;
    private final CapEquation capEquation;
    private final float stemTaperMinPct, spotFrequency;
    private final float stemTiltChance;
    private final double stemMaxTiltDeg;

    // caching & batching
    private int stemHeight;

    public ProceduralMushroomGenerator(PlatformWorld<T, N> world, Builder<T, N> b) {
        super(world);
        b.validate();
        this.stemRadius = b.stemRadius;
        this.hasSpots = b.hasSpots;
        this.hollowCap = b.hollowCap;
        this.stemMaterial = b.stemMaterial;
        this.capMaterial = b.capMaterial;
        this.spotMaterial = b.spotMaterial;
        this.ridgeMaterial = b.ridgeMaterial;
        this.capEquation = b.capEquation;
        this.capRadiusMin = b.capRadiusMin;
        this.capRadiusMax = b.capRadiusMax;
        this.capHeightMin = b.capHeightMin;
        this.capHeightMax = b.capHeightMax;
        this.stemHeightMin = b.stemHeightMin;
        this.stemHeightMax = b.stemHeightMax;
        this.stemTaperMinPct = b.stemTaperMinPct;
        this.stemTiltChance = b.stemTiltChance;
        this.stemMaxTiltDeg = b.stemMaxTiltDeg;
        this.spotFrequency = b.spotFrequency;
    }

    public void generate(RandomSource rnd, Vec3 origin) {
        this.rnd = rnd;
        this.stemHeight = rnd.nextInt(stemHeightMin, stemHeightMax);
        prepareFlush();
        placeStem(origin);
        flush.run();
    }

    private void placeStem(Vec3 origin) {
        final double maxYawRad = Math.toRadians(stemMaxTiltDeg); // e.g. 24°
        final double maxPitchRad = Math.toRadians(stemMaxTiltDeg);             // only ±5° pitch
        final double maxCumulative = Math.toRadians(stemMaxTiltDeg * 1.2);            // never lean more than 30°
        final double straighten = 0.4;
        double stepX = 0, stepZ = 0;

        double cumulativeTilt = 0.0;
        double x = origin.getX(), y = origin.getY(), z = origin.getZ();

        int finalX = 0, finalY = 0, finalZ = 0;
        Vector3 dir = Vector3.at(0, 1, 0);
        /*
        VSPEPlatformPlugin.platformLogger().info("📍  Placing stem at " +
                origin.getX() + " " + origin.getY() + " " + origin.getZ());
         */
        for (int i = 0; i < stemHeight; i++) {
            double progress = i / (double) stemHeight; // 0 at base → 1 at top
            double minR = stemRadius * stemTaperMinPct; // e.g. 0.5
            double curr = stemRadius - (stemRadius - minR) * progress;
            int r = Math.max(1, (int) Math.round(curr));

            // — carve out disc at (x,y,z) with radius r —
            int cx = (int) Math.round(x);
            int cy = (int) Math.round(y);
            int cz = (int) Math.round(z);
            short[] scan = StructureCacheUtils.getDiscScanlineWidths(r);
            for (int dz = -r; dz <= r; dz++) {
                int half = scan[dz + r];
                for (int dx = -half; dx <= half; dx++) {
                    guardAndStore(cx + dx, cy, cz + dz, r, stemMaterial, false);
                }
            }
            // 2) straighten a bit toward vertical
            dir = dir.multiply(1 - straighten)
                    .add(Vector3.at(0, 1, 0).multiply(straighten))
                    .normalize();

            // 3) yaw twist always allowed
            if (rnd.nextFloat() < stemTiltChance) {
                /*
                VSPEPlatformPlugin.platformLogger().info("Producing yaw twist...");
                VSPEPlatformPlugin.platformLogger().info(String.format(
                        "pre dir=(%.2f,%.2f,%.2f)",
                        dir.x, dir.y, dir.z
                ));
                 */
                double yawOff = (rnd.nextDouble() * 2 - 1) * maxYawRad;
                dir = rotateAroundY(dir, yawOff).normalize();
                /*
                VSPEPlatformPlugin.platformLogger().info(String.format(
                        "post dir=(%.2f,%.2f,%.2f)",
                        dir.x, dir.y, dir.z
                ));
                 */
            }

            // 4) gentle pitch, capped by cumulativeTilt
            if (rnd.nextFloat() < stemTiltChance * 0.5) {  // half chance for pitch
                /*
                VSPEPlatformPlugin.platformLogger().info("Producing pitch twist...");
                 */
                // pick a truly horizontal axis, NOT dir.cross(Y):
                double theta = rnd.nextDouble() * Math.PI * 2;
                Vector3 hAxis = Vector3.at(Math.cos(theta), 0, Math.sin(theta));
                /*
                VSPEPlatformPlugin.platformLogger().info(String.format(
                        "  pitch around axis=(%.2f,%.2f,%.2f)", hAxis.x, hAxis.y, hAxis.z
                ));
                 */
                double pitchOff = (rnd.nextDouble() * 2 - 1) * maxPitchRad;
                if (Math.abs(cumulativeTilt + pitchOff) <= maxCumulative) {
                    dir = rotateAroundAxis(dir, hAxis, pitchOff).normalize();
                    cumulativeTilt += pitchOff;
                    /*
                    VSPEPlatformPlugin.platformLogger().info(String.format(
                            "  new dir=(%.2f,%.2f,%.2f)", dir.x, dir.y, dir.z
                    ));
                     */
                }
            }

            double minHoriz = 0.3; // tweak: 0.3 → ~0.5 block sideways per step
            double hx = dir.getX(), hz = dir.getZ(), hy = dir.getY();
            double horiz = Math.hypot(hx, hz);
            if(horiz < minHoriz) {
                // scale XZ up so horiz == minHoriz
                double scale = minHoriz / (horiz + 1e-6);
                hx *= scale;
                hz *= scale;
                hy *= scale/2;
                dir = Vector3.at(hx, hy, hz).normalize();
            }

            stepX += dir.getX();
            stepZ += dir.getZ();

            finalX = (int) Math.round(x);
            finalY = (int) Math.round(y);
            finalZ = (int) Math.round(z);

            // 5) advance
            x += dir.getX();
            y += dir.getY();
            z += dir.getZ();

            /*
            if (i % 5 == 0) {
                VSPEPlatformPlugin.platformLogger().info(String.format(
                        "step %d pos=(%.2f,%.2f,%.2f) horiz=(%.2f,%.2f)",
                        i, x, y, z, stepX, stepZ
                ));
            }
             */
        }

        // final cap placement
        placeCap(new Vec3(finalX, finalY, finalZ));
    }


    private void placeCap(Vec3 origin) {
        /*
        VSPEPlatformPlugin.platformLogger().info(String.format("Placing at %s %s %s", origin.getX(), origin.getY(), origin.getZ()));
         */
        int capMaxR = rnd.nextInt(capRadiusMin, capRadiusMax);
        int capHeight = rnd.nextInt(capHeightMin, capHeightMax);
        int startCap = rnd.nextInt((int) (capHeight / 2.5)) + 1;

        Vec3 capBase = new Vec3(
                origin.getX(),
                origin.getY() - startCap,
                origin.getZ()
        );

        int drop = origin.getY() - capBase.getY();
        int gillCount = 10;
        for (int g = 0; g < gillCount; g++) {
            double angle = 2 * Math.PI * g / gillCount;
            double dx = Math.cos(angle), dz = Math.sin(angle);

            // march out from the origin to the rim
            for (int d = 0; d <= capMaxR; d++) {
                int gx = origin.getX() + (int) Math.round(dx * d);
                int gz = origin.getZ() + (int) Math.round(dz * d);

                // interpolate y from origin.y down to capBase.y
                double t = d / (double) capMaxR;
                int gy = origin.getY() - (int) Math.round(drop * t);

                guardAndStore(gx, gy, gz, ridgeMaterial, false, 1);
            }
        }

        Set<Triplet<Integer, Integer, Integer>> used;
        List<SpotShape> spotShapes = null;

        if (hasSpots) {
            used = new HashSet<>();
            spotShapes = new ArrayList<>();
            int spotAttempts = (int) (spotFrequency * capMaxR * capMaxR);  // tune this value

            for (int i = 0; i < spotAttempts; i++) {
                int dy = rnd.nextInt(0, capHeight); // pick a vertical level
                int dx = rnd.nextInt(-capMaxR, capMaxR + 1);
                int dz = rnd.nextInt(-capMaxR, capMaxR + 1);
                double dist = Math.hypot(dx, dz);
                if (dist > capMaxR || used.contains(Triplet.of(dx, dy, dz))) continue;

                int radius = rnd.nextInt(1, 5);  // horizontal radius
                int height = rnd.nextInt(2, 5); // vertical radius

                spotShapes.add(new SpotShape(dx, dy, dz, radius, height));
                used.add(Triplet.of(dx, dy, dz));
            }
        }

        double[][] sliceProfiles = new double[capHeight + 1][];

        for (int dy = 0; dy <= capHeight; dy++) {
            double yNorm = dy / (double) capHeight;
            // 1️⃣ Compute the slice radius once
            double sliceFrac = capEquation.getHeight(0, yNorm, capHeight, rnd);
            int sliceR = (int) Math.round(capMaxR * sliceFrac);
            if (sliceR < 1) continue;

            // 2️⃣ Build a profile lookup for this slice
            double[] profiles = new double[sliceR + 1];
            for (int r = 0; r <= sliceR; r++) {
                double xNorm = r / (double) sliceR;
                // getHeight at (xNorm, yNorm)
                profiles[r] = capEquation.getHeight(xNorm, yNorm, capHeight, rnd);
            }
            sliceProfiles[dy] = profiles;

            short[] scan = StructureCacheUtils.getDiscScanlineWidths(sliceR);
            int worldY = capBase.getY() + dy + 1;

            for (int dz = -sliceR; dz <= sliceR; dz++) {
                int half = scan[dz + sliceR];
                for (int dx = -half; dx <= half; dx++) {
                    // 3️⃣ Use integer radial distance as index into profiles[]
                    int distInt = (int) Math.round(Math.hypot(dx, dz));
                    if (distInt > sliceR) continue;      // safety
                    double xNorm = distInt / (double) sliceR;

                    // 4️⃣ Lookup the precomputed profile
                    double profile = sliceProfiles[dy][distInt];
                    if (xNorm > profile) continue;

                    // 5️⃣ Hollow-cap (example: inner 60% of big upper rings)
                    double rel = sliceR / (double) capMaxR;     // in [0…1]
                    double hollowStart = 0.4;                   // 40% of max
                    double hollowMax   = 0.6;                   // 60% of that slice

                    // compute how much hollow to apply on *this* slice:
                    //   0 for rel<hollowStart
                    //   linearly ramp from 0→hollowMax as rel goes hollowStart→1
                    double hollowFrac = rel <= hollowStart
                            ? 0
                            : hollowMax * ((rel - hollowStart) / (1 - hollowStart));

                    // now your condition becomes:
                    if(hollowCap
                            && dy <= (capHeight * 0.3)                // only on bottom 30% of cap
                            && rel >= hollowStart                     // only on big enough rings
                            && xNorm < hollowFrac                     // use our dynamic threshold
                    ) {
                        continue;
                    }

                    // 6️⃣ Spot logic as before…
                    PlatformBlockState<T> mat = capMaterial;
                    if (hasSpots) {
                        for (SpotShape spot : spotShapes) {
                            int dxRel = dx - spot.dx,
                                    dyRel = dy - spot.dy,
                                    dzRel = dz - spot.dz;
                            double xComp = dxRel / (double) spot.r,
                                    yComp = dyRel / (double) spot.h,
                                    zComp = dzRel / (double) spot.r;
                            if (xComp * xComp + yComp * yComp + zComp * zComp <= 1.0) {
                                mat = spotMaterial;
                                break;
                            }
                        }
                    }

                    guardAndStore(
                            origin.getX() + dx,
                            worldY,
                            origin.getZ() + dz,
                            mat, false, 1
                    );
                }
            }
        }
    }

    @Override
    public BlockVec3i getApproximateSize() {
        return null;
    }


    /** cap height function interface */
    @FunctionalInterface
    public interface CapEquation {
        /**
         * @param xNorm  horizontal distance from cap center, normalized to [0…1]
         * @param yNorm  current vertical slice, normalized to [0…1]
         * @param rnd    a Random, in case you want noise
         * @return       maximum y-fraction that remains inside the cap at this x-slice
         */
        double getHeight(double xNorm, double yNorm, double height, RandomSource rnd);
    }
    record SpotShape(int dx, int dy, int dz, int r, int h) {}

    public static class Builder<T, N> extends BaseBuilder<T, N, ProceduralMushroomGenerator<T, N>> {
        int stemRadius=2, capRadiusMin = 4, capRadiusMax = 10, capHeightMin = 3, capHeightMax = 7, stemHeightMin = 10, stemHeightMax = 16;
        boolean hasSpots=false, hollowCap=false;
        PlatformBlockState<T> stemMaterial, capMaterial, spotMaterial, ridgeMaterial;
        CapEquation capEquation;
        private float stemTaperMinPct = 0.2f, spotFrequency = 0.3f;
        private float stemTiltChance  = 0.1f;
        private double stemMaxTiltDeg = 5;

        /** how much of the original radius remains at the top of the stem (0…1) */
        public Builder<T, N> stemTaperPct(float p) {
            this.stemTaperMinPct = clamp(p, 0, 1);
            return this;
        }
        /** chance each block that the stem will kink by up to ±maxTiltDeg */
        public Builder<T, N> stemTilt(float chance, double maxTiltDeg) {
            this.stemTiltChance   = clamp(chance, 0, 1);
            this.stemMaxTiltDeg   = maxTiltDeg;
            return this;
        }
        public Builder<T, N> capRadiusRange(int min,int max){capRadiusMin=min;capRadiusMax=max;return this;}
        public Builder<T, N> capHeightRange(int min,int max){capHeightMin = min; capHeightMax = max;return this;}
        public Builder<T, N> stemHeightRange(int min,int max, int r){stemRadius = r;stemHeightMin = min; stemHeightMax = max;return this;}
        public Builder<T, N> materials(PlatformBlockState<T> stem, PlatformBlockState<T> cap){this.stemMaterial=stem;this.capMaterial=cap;return this;}
        public Builder<T, N> spots(PlatformBlockState<T> spot, float frequency){this.hasSpots=true;this.spotMaterial=spot;this.spotFrequency=frequency;return this;}
        public Builder<T, N> hollowCap(){this.hollowCap=true;return this;}
        public Builder<T, N> ridgeMat(PlatformBlockState<T> m){this.ridgeMaterial=m;return this;}
        public Builder<T, N> capEq(CapEquation eq){this.capEquation=eq;return this;}

        public void validate() {
            // Stem
            if (stemRadius < 1) throw new IllegalArgumentException("Stem radius must be at least 1");
            if (stemHeightMin > stemHeightMax)
                throw new IllegalArgumentException("stemHeightMin > stemHeightMax");

            if (stemTaperMinPct < 0 || stemTaperMinPct > 1)
                throw new IllegalArgumentException("stemTaperPct must be between 0 and 1");

            if (stemTiltChance < 0 || stemTiltChance > 1)
                throw new IllegalArgumentException("stemTiltChance must be between 0 and 1");

            if (stemMaxTiltDeg < 0 || stemMaxTiltDeg > 45)
                throw new IllegalArgumentException("stemMaxTiltDeg is unusually large");

            // Cap
            if (capRadiusMin > capRadiusMax)
                throw new IllegalArgumentException("capRadiusMin > capRadiusMax");

            if (capHeightMin > capHeightMax)
                throw new IllegalArgumentException("capHeightMin > capHeightMax");

            // Materials
            Objects.requireNonNull(stemMaterial, "stemMaterial must not be null");
            Objects.requireNonNull(capMaterial, "capMaterial must not be null");

            if (hasSpots) {
                Objects.requireNonNull(spotMaterial, "spotMaterial must not be null if hasSpots is true");
                if (spotFrequency < 0 || spotFrequency > 1)
                    throw new IllegalArgumentException("spotFrequency must be between 0 and 1");
            }

            // Optional but good practice
            if (capEquation == null)
                throw new IllegalStateException("capEquation not set — please call capEq(...)");

            // Optional warning
            if (ridgeMaterial == null)
                VSPEPlatformPlugin.platformLogger().warn("Ridge material not set — caps may look bland.");
        }

        @Override
        public ProceduralMushroomGenerator<T, N> build(PlatformWorld<T, N> world) {
            return new ProceduralMushroomGenerator<>(world, this);
        }
    }
}
