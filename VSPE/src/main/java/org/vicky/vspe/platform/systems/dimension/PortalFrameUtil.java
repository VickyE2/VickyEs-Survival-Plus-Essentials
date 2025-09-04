package org.vicky.vspe.platform.systems.dimension;


import org.vicky.platform.utils.Direction;
import org.vicky.platform.utils.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility for generating portal frame and portal interior point lists.
 * Each point is represented as an int[3] = {x, y, z} in local coordinates.
 * The generator uses the plane z=0 as the portal plane; rotate with rotateToAxis(...) if needed.
 */
public final class PortalFrameUtil {
    private PortalFrameUtil() {
    }

    /**
     * Create a portal pattern for the given shape.
     *
     * @param shape  shape type
     * @param innerW inner width (portal opening width, in blocks). For RING it is diameter.
     * @param innerH inner height (portal opening height, in blocks). Ignored for pure ring where diameter = innerW.
     * @param axis   orientation axis: Direction.Axis.Z => portal plane is X-Y (z=0). Direction.Axis.X rotates to Z-Y.
     * @return PortalPattern where each point is int[]{x,y,z}
     */
    public static PortalPattern create(PortalShape shape, int innerW, int innerH, Direction axis) {
        PortalPattern p = switch (shape) {
            case NETHER_RECT -> makeNetherRect(innerW, innerH);
            case ROUNDED_ARCH -> makeRoundedArch(innerW, innerH);
            case RING -> makeRing(innerW);
            case TRIANGLE -> makeTriangle(innerW, innerH);
            default -> makeRectangle(innerW, innerH);
        };

        if (axis == Direction.EAST || axis == Direction.WEST) {
            return rotateToAxisX(p);
        } else {
            return p;
        }
    }

    /**
     * Convert an int[][] coordinates array to a {@link List<Vec3>}.
     */
    public static List<Vec3> toVec3List(int[][] coords, Vec3 offset) {
        List<Vec3> list = new ArrayList<>(coords.length);
        for (int[] c : coords) list.add(offset.add(c[0], c[1], c[2]));
        return list;
    }

    // ----------------------------
    // Public API
    // ----------------------------

    public static List<Vec3> toVec3List(int[][] coords) {
        return toVec3List(coords, new Vec3(0, 0, 0));
    }

    /**
     * Vanilla Nether portal pattern generator. The vanilla interior min is width=2 height=3.
     */
    private static PortalPattern makeNetherRect(int innerW, int innerH) {
        if (innerW < 2) innerW = 2;
        if (innerH < 3) innerH = 3;

        // Outer frame dims = inner + 2 (one block frame on each side)
        int outerW = innerW + 2;
        int outerH = innerH + 2;

        // Origin: lower-left corner of outer frame at (0,0,0)
        List<int[]> frame = new ArrayList<>();
        List<int[]> interior = new ArrayList<>();

        for (int x = 0; x < outerW; x++) {
            for (int y = 0; y < outerH; y++) {
                boolean isBorder = (x == 0 || x == outerW - 1 || y == 0 || y == outerH - 1);
                int z = 0;
                if (isBorder) {
                    frame.add(new int[]{x, y, z});
                } else {
                    interior.add(new int[]{x, y, z});
                }
            }
        }

        return new PortalPattern(toArray(frame), toArray(interior));
    }

    /**
     * Generic rectangle with frame=1 thick
     */
    private static PortalPattern makeRectangle(int innerW, int innerH) {
        if (innerW < 1) innerW = 1;
        if (innerH < 1) innerH = 1;

        int outerW = innerW + 2;
        int outerH = innerH + 2;

        List<int[]> frame = new ArrayList<>();
        List<int[]> interior = new ArrayList<>();
        for (int x = 0; x < outerW; x++) {
            for (int y = 0; y < outerH; y++) {
                int z = 0;
                boolean border = (x == 0 || x == outerW - 1 || y == 0 || y == outerH - 1);
                if (border) frame.add(new int[]{x, y, z});
                else interior.add(new int[]{x, y, z});
            }
        }
        return new PortalPattern(toArray(frame), toArray(interior));
    }

    // ----------------------------
    // Implementations for shapes
    // ----------------------------

    /**
     * Rounded arch: rectangular lower portion, semicircle top.
     */
    private static PortalPattern makeRoundedArch(int innerW, int innerH) {
        if (innerW < 3) innerW = 3;
        if (innerH < 3) innerH = 3;

        // split height: rectangle part + semicircle (approx)
        int rectHeight = Math.max(1, innerH - innerW / 2); // keep arch proportional
        int circleRadius = innerW / 2;

        // compute outer bounds
        int outerW = innerW + 2;
        int outerH = rectHeight + circleRadius + 2;

        List<int[]> frame = new ArrayList<>();
        List<int[]> interior = new ArrayList<>();

        double cx = outerW / 2.0 - 0.5; // center x
        double cy = rectHeight + 0.5;   // center y for semicircle

        for (int x = 0; x < outerW; x++) {
            for (int y = 0; y < outerH; y++) {
                int z = 0;
                boolean inSemicircleRegion = y >= rectHeight + 1;
                if (!inSemicircleRegion) {
                    // below semicircle: rectangle with border
                    boolean border = (x == 0 || x == outerW - 1 || y == 0 || y == rectHeight);
                    if (border) frame.add(new int[]{x, y, z});
                    else interior.add(new int[]{x, y, z});
                } else {
                    // semicircle area: compute distance to center
                    double dx = x - cx;
                    double dy = y - cy;
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    double r = circleRadius + 0.0;
                    if (dist <= r - 0.5) {
                        interior.add(new int[]{x, y, z});
                    } else if (Math.abs(dist - r) <= 0.75) {
                        frame.add(new int[]{x, y, z});
                    } // outside ignore
                }
            }
        }
        return new PortalPattern(cleanArray(frame), cleanArray(interior));
    }

    /**
     * Ring: approximate circular portal. innerW is diameter in blocks.
     */
    private static PortalPattern makeRing(int diameter) {
        if (diameter < 3) diameter = 3;
        double radius = diameter / 2.0;
        int size = diameter + 2;

        double cx = size / 2.0 - 0.5;
        double cy = size / 2.0 - 0.5;

        List<int[]> frame = new ArrayList<>();
        List<int[]> interior = new ArrayList<>();

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                double dx = x - cx, dy = y - cy;
                double dist = Math.sqrt(dx * dx + dy * dy);
                int z = 0;
                if (dist < radius - 0.5) {
                    interior.add(new int[]{x, y, z});
                } else if (Math.abs(dist - radius) <= 0.75) {
                    frame.add(new int[]{x, y, z});
                }
            }
        }
        return new PortalPattern(cleanArray(frame), cleanArray(interior));
    }

    /**
     * Triangle: isosceles triangle portal with base innerW and height innerH (opening).
     */
    private static PortalPattern makeTriangle(int innerW, int innerH) {
        if (innerW < 1) innerW = 1;
        if (innerH < 1) innerH = 1;

        int outerW = innerW + 2;
        int outerH = innerH + 2;

        List<int[]> frame = new ArrayList<>();
        List<int[]> interior = new ArrayList<>();

        // origin bottom-left of outer bounding
        double bx = outerW / 2.0 - 0.5; // center x
        for (int x = 0; x < outerW; x++) {
            for (int y = 0; y < outerH; y++) {
                int z = 0;
                // compute normalized vertical position 0..1
                double t = (double) y / (outerH - 1);
                // width at this y
                double halfAtY = (outerW - 1) / 2.0 * (1.0 - t); // tapers to tip
                double dx = Math.abs(x - bx);
                boolean inside = dx <= halfAtY - 0.5;
                boolean border = !inside && dx <= halfAtY + 0.5;
                if (inside) {
                    interior.add(new int[]{x, y, z});
                } else if (border) {
                    frame.add(new int[]{x, y, z});
                }
            }
        }
        return new PortalPattern(cleanArray(frame), cleanArray(interior));
    }

    /**
     * Rotate a PortalPattern from plane z=0 (X-Y) to plane x=0 (Z-Y) (Axis.X).
     */
    private static PortalPattern rotateToAxisX(PortalPattern p) {
        int[][] f = new int[p.frame.length][3];
        int[][] i = new int[p.interior.length][3];
        // mapping: original (x,y,z=0) -> rotated (z' = x, y' = y, x' = 0)
        // We choose to put the portal plane at x=0 and vary z upward.
        for (int idx = 0; idx < p.frame.length; idx++) {
            int[] c = p.frame[idx];
            f[idx] = new int[]{0, c[1], c[0]}; // x'=0, y'=y, z'=x
        }
        for (int idx = 0; idx < p.interior.length; idx++) {
            int[] c = p.interior[idx];
            i[idx] = new int[]{0, c[1], c[0]};
        }
        return new PortalPattern(f, i);
    }

    // ----------------------------
    // Helpers
    // ----------------------------
    private static int[][] toArray(List<int[]> list) {
        return list.toArray(new int[0][]);
    }

    // ----------------------------
    // Rotation utilities
    // ----------------------------

    /**
     * Removes duplicates and sorts deterministically (x,y,z)
     */
    private static int[][] cleanArray(List<int[]> list) {
        // simple dedupe with string key (small lists so fine)
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        List<int[]> out = new ArrayList<>();
        for (int[] c : list) {
            String key = c[0] + "," + c[1] + "," + c[2];
            if (seen.add(key)) out.add(c);
        }
        out.sort((a, b) -> {
            if (a[1] != b[1]) return Integer.compare(a[1], b[1]); // sort by y then x
            if (a[0] != b[0]) return Integer.compare(a[0], b[0]);
            return Integer.compare(a[2], b[2]);
        });
        return out.toArray(new int[0][]);
    }

    /**
     * Built-in shapes
     */
    public enum PortalShape {
        /**
         * Vanilla nether-style rectangular portal. innerWidth/innerHeight are the interior (air/portal) size.
         */
        NETHER_RECT,

        /**
         * Simple rectangular frame of given inner size
         */
        RECTANGLE,

        /**
         * Arch: rectangle with semicircular top (rounded top)
         */
        ROUNDED_ARCH,

        /**
         * Ring (rough circle) - innerWidth used as diameter
         */
        RING,

        /**
         * Isosceles triangle portal (pointing up). innerWidth = base, innerHeight = height.
         */
        TRIANGLE
    }

    /**
     * Result holder: frame blocks (the structural blocks) and portal blocks (the inside area).
     */
    public static class PortalPattern {
        public final int[][] frame;   // coordinates of frame blocks
        public final int[][] interior; // coordinates of portal (air/portal material) positions

        public PortalPattern(int[][] frame, int[][] interior) {
            this.frame = frame;
            this.interior = interior;
        }
    }
}

