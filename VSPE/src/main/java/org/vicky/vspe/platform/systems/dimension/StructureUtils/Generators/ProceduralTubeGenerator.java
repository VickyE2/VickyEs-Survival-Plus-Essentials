package org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators;

import org.vicky.platform.utils.Vec3;
import org.vicky.platform.world.PlatformBlockState;
import org.vicky.platform.world.PlatformWorld;
import org.vicky.vspe.BlockVec3i;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.ProceduralStructureGenerator;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.StructureCacheUtils;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.BlockPlacement;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.RandomSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Generates a single hollow tube coral structure with an optional one-block-thick spherical reservoir beneath.
 * Supports configurable height range, wall thickness, quadratic flare, rim cap, flare start percentage, and cached scanlines.
 */
public class ProceduralTubeGenerator<T>
        extends ProceduralStructureGenerator<T> {
    // Configuration fields
    private final int heightMin, heightMax;
    private final int tubeRadius;
    private final int wallThickness;
    private final double slopeCoef;
    private final double flareStartPercentage;
    private final boolean includeSphereReservoir;
    private final int reservoirRadius;
    private final boolean includeRimCap;
    private final PlatformBlockState<T> coralMaterial, waterMaterial;

    public ProceduralTubeGenerator(Builder<T> b) {
        b.validate();
        this.heightMin = b.heightMin;
        this.heightMax = b.heightMax;
        this.tubeRadius = b.tubeRadius;
        this.wallThickness = b.wallThickness;
        this.slopeCoef = b.slopeCoef;
        this.flareStartPercentage = b.flareStartPercentage;
        this.includeSphereReservoir = b.includeReservoir;
        this.reservoirRadius = b.reservoirRadius;
        this.includeRimCap = b.includeRimCap;
        this.coralMaterial = b.coralMaterial;
        this.waterMaterial = b.waterMaterial;
    }

    /**
     * Conservative bounding box calculation:
     * - horizontal: tubeRadius + worst-case flare + reservoir radius (if used) + margin
     * - vertical: heightMax + small cap/rim margin above, plus reservoir depth below (if used)
     * <p>
     * This intentionally overestimates a little so chunk bucketing/placement never runs out of space.
     */
    @Override
    public BlockVec3i getApproximateSize() {
        // compute worst-case flare using heightMax
        int flareStartY = (int) Math.floor(flareStartPercentage * heightMax);
        int flareHeight = Math.max(0, heightMax - flareStartY);
        // worst-case extra horizontal due to quadratic flare: slopeCoef * (flareHeight)^2
        int maxFlare = (int) Math.ceil(slopeCoef * (flareHeight * (double) flareHeight));

        int horizRadius = tubeRadius + maxFlare + wallThickness + 4; // +4 margin for rounding/rotation
        // If we include reservoir, it can be wider than the flared tube; account for it.
        if (includeSphereReservoir) {
            horizRadius = Math.max(horizRadius, reservoirRadius + 4);
        }

        int width = horizRadius * 2 + 1;
        int depth = width;

        // vertical: top = heightMax plus rim/cap margin; bottom = reservoir depth (if included)
        int up = heightMax + 4; // some headroom
        int down = includeSphereReservoir ? reservoirRadius + 2 : 2; // small margin below
        int height = up + down + 1;

        return new BlockVec3i(width, height, depth);
    }

    @Override
    protected void performGeneration(RandomSource rnd, Vec3 origin, List<BlockPlacement<T>> outPlacements, Map<Long, BiConsumer<PlatformWorld<T, ?>, Vec3>> outActions) {
        this.rnd = rnd;
        // defensive: if bounds degenerate, use min
        final int height;
        if (heightMax <= heightMin) {
            height = heightMin;
        } else {
            // RandomSource.nextInt(start, bound) expects bound > start; we pass heightMax+1 to make end inclusive
            height = rnd.nextInt(heightMin, heightMax + 1);
        }

        int flareStartY = (int) Math.floor(flareStartPercentage * height);

        // Cache scanlines up to maximum needed radius (tube flare or reservoir)
        int maxFlare = (int) Math.round(slopeCoef * (height - flareStartY) * (height - flareStartY));
        int tubeMaxR = tubeRadius + maxFlare;
        int absMax = includeSphereReservoir ? Math.max(tubeMaxR, reservoirRadius) : tubeMaxR;
        Map<Integer, short[]> scanCache = new HashMap<>();
        for (int r = 0; r <= absMax; r++) {
            scanCache.put(r, StructureCacheUtils.getDiscScanlineWidths(r));
        }

        // Build vertical hollow tube layers
        for (int y = 0; y <= height; y++) {
            double flare = y >= flareStartY ? slopeCoef * (y - flareStartY) * (y - flareStartY) : 0;
            int outerR = tubeRadius + (int) Math.round(flare);
            int innerR = Math.max(outerR - wallThickness, 0);
            drawRing(origin.getIntX(), origin.getIntY() + y, origin.getIntZ(), outerR, innerR, scanCache);
        }

        // Optional rim cap at top
        if (includeRimCap) {
            int capY = origin.getIntY() + height + 1;
            drawRing(origin.getIntX(), capY, origin.getIntZ(), tubeRadius, Math.max(tubeRadius - wallThickness, 0), scanCache);
        }

        // Optional spherical reservoir beneath the tube (shell only)
        if (includeSphereReservoir) {
            int centerY = origin.getIntY() - reservoirRadius;
            drawSphereShell(origin.getIntX(), centerY, origin.getIntZ(), reservoirRadius);
        }
    }

    /**
     * Draws a hollow ring between outerR and innerR (exclusive of inner volume).
     */
    private void drawRing(int cx, int cy, int cz, int outerR, int innerR, Map<Integer, short[]> cache) {
        if (outerR <= 0) return;
        short[] outerScan = cache.get(outerR);
        short[] innerScan = cache.get(innerR);
        for (int dz = -outerR; dz <= outerR; dz++) {
            int oHalf = outerScan[dz + outerR];
            int iHalf = -1;
            if (innerScan != null && dz >= -innerR && dz <= innerR) {
                iHalf = innerScan[dz + innerR];
            }
            for (int dx = -oHalf; dx <= oHalf; dx++) {
                // place coral only in the ring (outside inner radius)
                if (dx > iHalf || dx < -iHalf) {
                    guardAndStore(cx + dx, cy, cz + dz, coralMaterial, false);
                }
            }
        }
    }

    /**
     * Draws a one-block-thick spherical shell of coral material.
     * Also places water inside the shell (one-block-thick reservoir).
     */
    private void drawSphereShell(int cx, int cy, int cz, int radius) {
        if (radius <= 0) return;
        // Iterate vertical slices
        for (int dy = -radius; dy <= radius; dy++) {
            int y = cy + dy;
            double sliceRadius = Math.sqrt(radius * (double) radius - dy * (double) dy);
            int outerR = (int) Math.floor(sliceRadius);
            int innerR = outerR - 1;
            // Use scanlines dynamically since radius varies per slice
            short[] outerScan = StructureCacheUtils.getDiscScanlineWidths(outerR);
            short[] innerScan = innerR >= 0 ? StructureCacheUtils.getDiscScanlineWidths(innerR) : null;
            for (int dz = -outerR; dz <= outerR; dz++) {
                int oHalf = outerScan[dz + outerR];
                int iHalf = -1;
                if (innerScan != null && dz >= -innerR && dz <= innerR) {
                    iHalf = innerScan[dz + innerR];
                }
                for (int dx = -oHalf; dx <= oHalf; dx++) {
                    if (dx > iHalf || dx < -iHalf) {
                        // shell block
                        guardAndStore(cx + dx, y, cz + dz, coralMaterial, false);
                    } else {
                        // inside the shell — put water (or the configured water material)
                        if (waterMaterial != null) {
                            guardAndStore(cx + dx, y, cz + dz, waterMaterial, false);
                        }
                    }
                }
            }
        }
    }

    /**
     * Builder with parameter validation.
     */
    public static class Builder<T> extends BaseBuilder<T, ProceduralTubeGenerator<T>> {
        private int heightMin = 6, heightMax = 12;
        private int tubeRadius = 2;
        private int wallThickness = 1;
        private double slopeCoef = 0.02;
        private double flareStartPercentage = 0.5;
        private boolean includeReservoir = true;
        private int reservoirRadius = 3;
        private boolean includeRimCap = true;
        private PlatformBlockState<T> coralMaterial;
        private PlatformBlockState<T> waterMaterial;

        public Builder<T> heightRange(int min, int max) {
            heightMin = min;
            heightMax = max;
            return this;
        }

        public Builder<T> tubeRadius(int r) {
            tubeRadius = r;
            return this;
        }

        public Builder<T> wallThickness(int t) {
            wallThickness = t;
            return this;
        }

        public Builder<T> slope(double coef) {
            slopeCoef = coef;
            return this;
        }

        public Builder<T> flareStart(double percent) {
            flareStartPercentage = percent;
            return this;
        }

        public Builder<T> rimCap(boolean inc) {
            includeRimCap = inc;
            return this;
        }

        public Builder<T> reservoir(boolean inc, int radius) {
            includeReservoir = inc;
            reservoirRadius = radius;
            return this;
        }

        public Builder<T> materials(PlatformBlockState<T> coral, PlatformBlockState<T> water) {
            coralMaterial = coral;
            waterMaterial = water;
            return this;
        }

        public void validate() {
            Objects.requireNonNull(coralMaterial, "Coral material must be set");
            if (heightMin > heightMax) throw new IllegalArgumentException("heightMin > heightMax");
            if (wallThickness < 1) throw new IllegalArgumentException("Wall thickness must be >=1");
            if (flareStartPercentage < 0 || flareStartPercentage > 1)
                throw new IllegalArgumentException("flareStartPercentage must be between 0 and 1");
            if (slopeCoef > 0.5)
                throw new IllegalArgumentException("Slope coefficient is too large... please use a lower value...");
        }

        @Override
        protected ProceduralTubeGenerator<T> create() {
            return new ProceduralTubeGenerator<>(this);
        }
    }
}
