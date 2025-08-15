package org.vicky.vspe.platform.systems.dimension.StructureUtils;

import org.vicky.vspe.platform.VSPEPlatformPlugin;

import java.util.*;

import static java.lang.Math.TAU;

/**
 * Precomputed scanline widths for discs, full sphere offsets, and other shapes,
 * including regular polygons (pentagon, hexagon, heptagon, octagon).
 */
public class StructureCacheUtils {
    private static final Map<Integer, short[]> DISC_SCANLINES = new HashMap<>();
    private static final Map<Integer, short[]> SPHERE_OFFSETS = new HashMap<>();
    private static final Map<Integer, short[]> HEMISPHERE_OFFSETS = new HashMap<>();
    private static final Map<Integer, short[]> CUBE_OFFSETS = new HashMap<>();
    private static final Map<Integer, short[]> SQUARE_SCANLINES = new HashMap<>();
    private static final Map<Integer, short[]> CYLINDER_OFFSETS = new HashMap<>();
    private static final Map<Integer, short[]> CONE_OFFSETS = new HashMap<>();
    // Polygon cache: key = r*10 + sides
    private static final Map<Integer, short[]> POLYGON_OFFSETS = new HashMap<>();

    /**
     * For a given radius r returns a (2r+1)-length array of half-widths for each dy
     */
    public static short[] getDiscScanlineWidths(int r) {
        return DISC_SCANLINES.computeIfAbsent(r, StructureCacheUtils::buildScanlineWidths);
    }

    /**
     * Full [dx,dy,dz,…] triples for spheres (solid)
     */
    public static short[] getSphereOffsets(int r) {
        return SPHERE_OFFSETS.computeIfAbsent(r, StructureCacheUtils::buildSphereOffsets);
    }

    /**
     * Upper hemisphere (dy >= 0) offsets
     */
    public static short[] getHemisphereOffsets(int r) {
        return HEMISPHERE_OFFSETS.computeIfAbsent(r, StructureCacheUtils::buildHemisphereOffsets);
    }

    /**
     * Solid cube of side length 2r+1 centered
     */
    public static short[] getCubeOffsets(int r) {
        return CUBE_OFFSETS.computeIfAbsent(r, StructureCacheUtils::buildCubeOffsets);
    }

    /**
     * 2D square (all points with |dx|,|dz| less_than= r)
     */
    public static short[] getSquareScanlineWidths(int r) {
        return SQUARE_SCANLINES.computeIfAbsent(r, StructureCacheUtils::buildSquareScanlineWidths);
    }

    /**
     * Vertical cylinder of height 2r and radius r
     */
    public static short[] getCylinderOffsets(int r) {
        return CYLINDER_OFFSETS.computeIfAbsent(r, StructureCacheUtils::buildCylinderOffsets);
    }

    /**
     * Cone of height r and base radius r
     */
    public static short[] getConeOffsets(int r) {
        return CONE_OFFSETS.computeIfAbsent(r, StructureCacheUtils::buildConeOffsets);
    }

    /**
     * Regular polygon offsets (2D) for n sides at radius r
     */
    public static short[] getPolygonOffsets(int r, int sides) {
        int key = r * 10 + sides;
        return POLYGON_OFFSETS.computeIfAbsent(key, k -> buildPolygonOffsets(r, sides));
    }

    /**
     * Precompute all shapes at radius r.
     */
    public static void getDefaultOffsets(int r) {
        getDiscScanlineWidths(r);
        getSphereOffsets(r);
        getHemisphereOffsets(r);
        getCubeOffsets(r);
        getSquareScanlineWidths(r);
        getCylinderOffsets(r);
        getConeOffsets(r);
        // common regular polygons
        getPolygonOffsets(r, 5);
        getPolygonOffsets(r, 6);
        getPolygonOffsets(r, 7);
        getPolygonOffsets(r, 8);
        VSPEPlatformPlugin.platformLogger().info("[STRUCTURE-UTILS] Structure caching complete for r=" + r);
    }

    private static short[] buildScanlineWidths(int r) {
        short[] w = new short[2 * r + 1];
        int r2 = r * r;
        for (int dy = -r; dy <= r; dy++) {
            w[dy + r] = (short) Math.floor(Math.sqrt(r2 - dy * dy));
        }
        return w;
    }

    private static short[] buildSquareScanlineWidths(int r) {
        short[] w = new short[2 * r + 1];
        for (int dy = -r; dy <= r; dy++) w[dy + r] = (short) r;
        return w;
    }

    private static short[] buildSphereOffsets(int r) {
        List<Short> voxels = new ArrayList<>();
        int r2 = r * r;
        for (int dy = -r; dy <= r; dy++) {
            int sliceR = (int) Math.floor(Math.sqrt(r2 - dy * dy));
            int sliceR2 = sliceR * sliceR;
            for (int dx = -sliceR; dx <= sliceR; dx++) {
                int dx2 = dx * dx;
                int maxDz = (int) Math.floor(Math.sqrt(sliceR2 - dx2));
                for (int dz = -maxDz; dz <= maxDz; dz++) {
                    voxels.add((short) dx);
                    voxels.add((short) dy);
                    voxels.add((short) dz);
                }
            }
        }
        return flatten(voxels);
    }

    private static short[] buildHemisphereOffsets(int r) {
        List<Short> voxels = new ArrayList<>();
        int r2 = r * r;
        for (int dy = 0; dy <= r; dy++) {
            int sliceR = (int) Math.floor(Math.sqrt(r2 - dy * dy));
            int sliceR2 = sliceR * sliceR;
            for (int dx = -sliceR; dx <= sliceR; dx++) {
                int dx2 = dx * dx;
                int maxDz = (int) Math.floor(Math.sqrt(sliceR2 - dx2));
                for (int dz = -maxDz; dz <= maxDz; dz++) {
                    voxels.add((short) dx);
                    voxels.add((short) dy);
                    voxels.add((short) dz);
                }
            }
        }
        return flatten(voxels);
    }

    private static short[] buildCubeOffsets(int r) {
        List<Short> voxels = new ArrayList<>();
        for (int dy = -r; dy <= r; dy++)
            for (int dx = -r; dx <= r; dx++)
                for (int dz = -r; dz <= r; dz++) {
                    voxels.add((short) dx);
                    voxels.add((short) dy);
                    voxels.add((short) dz);
                }
        return flatten(voxels);
    }

    private static short[] buildCylinderOffsets(int r) {
        List<Short> voxels = new ArrayList<>();
        short[] scan = buildScanlineWidths(r);
        for (int dy = -r; dy <= r; dy++) {
            for (int idx = 0; idx < scan.length; idx++) {
                int row = idx - r;
                int half = scan[idx];
                for (int dx = -half; dx <= half; dx++) {
                    voxels.add((short) dx);
                    voxels.add((short) row);
                    voxels.add((short) dy);
                }
            }
        }
        return flatten(voxels);
    }

    private static short[] buildConeOffsets(int r) {
        List<Short> voxels = new ArrayList<>();
        for (int dy = 0; dy <= r; dy++) {
            double frac = 1.0 - dy / (double) r;
            int sliceR = (int) Math.floor(r * frac);
            int sliceR2 = sliceR * sliceR;
            for (int dx = -sliceR; dx <= sliceR; dx++) {
                int dx2 = dx * dx;
                int maxDz = (int) Math.floor(Math.sqrt(sliceR2 - dx2));
                for (int dz = -maxDz; dz <= maxDz; dz++) {
                    voxels.add((short) dx);
                    voxels.add((short) dy);
                    voxels.add((short) dz);
                }
            }
        }
        return flatten(voxels);
    }

    /**
     * Build offsets for a 2D regular polygon of given number of sides at radius r.
     */
    private static short[] buildPolygonOffsets(int r, int sides) {
        List<Short> coords = new ArrayList<>();
        double angleStep = TAU / sides;
        double halfSector = angleStep / 2;
        double cosHalfInterior = Math.cos(Math.PI / sides);

        for (int dz = -r; dz <= r; dz++) {
            for (int dx = -r; dx <= r; dx++) {
                double angle = Math.atan2(dz, dx);
                // Normalize angle to [0, TAU)
                if (angle < 0) angle += TAU;

                // Find center angle of nearest polygon sector
                double sectorAngle = Math.round(angle / angleStep) * angleStep;

                // Compute max radius at this angle for regular polygon
                double maxRadius = r * cosHalfInterior / Math.cos(angle - sectorAngle);

                if (Math.hypot(dx, dz) <= maxRadius) {
                    coords.add((short) dx);
                    coords.add((short) dz);
                }
            }
        }

        short[] flat = new short[coords.size()];
        for (int i = 0; i < flat.length; i++) flat[i] = coords.get(i);
        return flat;
    }


    private static short[] flatten(List<Short> voxels) {
        short[] flat = new short[voxels.size()];
        for(int i = 0; i < flat.length; i++) flat[i] = voxels.get(i);
        return flat;
    }
}