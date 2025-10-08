package org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators.parts;

import org.vicky.platform.utils.Vec3;
import org.vicky.platform.world.PlatformBlockState;
import org.vicky.utilities.Pair;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

public class DefaultDecorations {

    // -------------------------
    // Helpers
    // -------------------------
    private static <T> void addSphere(Set<Pair<Vec3, PlatformBlockState<T>>> parts,
                                      double cx, double cy, double cz,
                                      double radius, PlatformBlockState<T> mat) {
        int minX = (int) Math.floor(cx - radius);
        int maxX = (int) Math.ceil(cx + radius);
        int minY = (int) Math.floor(cy - radius);
        int maxY = (int) Math.ceil(cy + radius);
        int minZ = (int) Math.floor(cz - radius);
        int maxZ = (int) Math.ceil(cz + radius);

        double r2 = radius * radius;
        for (int x = minX; x <= maxX; x++) {
            double dx = x + 0.5 - cx;
            double dx2 = dx * dx;
            for (int y = minY; y <= maxY; y++) {
                double dy = y + 0.5 - cy;
                double dy2 = dy * dy;
                for (int z = minZ; z <= maxZ; z++) {
                    double dz = z + 0.5 - cz;
                    double dz2 = dz * dz;
                    if (dx2 + dy2 + dz2 <= r2) {
                        parts.add(new Pair<>(new Vec3(x, y, z), mat));
                    }
                }
            }
        }
    }

    private static <T> void addEllipsoid(Set<Pair<Vec3, PlatformBlockState<T>>> parts,
                                         double cx, double cy, double cz,
                                         double rx, double ry, double rz,
                                         PlatformBlockState<T> mat) {
        int minX = (int) Math.floor(cx - rx);
        int maxX = (int) Math.ceil(cx + rx);
        int minY = (int) Math.floor(cy - ry);
        int maxY = (int) Math.ceil(cy + ry);
        int minZ = (int) Math.floor(cz - rz);
        int maxZ = (int) Math.ceil(cz + rz);

        double invRx2 = 1.0 / (rx * rx);
        double invRy2 = 1.0 / (ry * ry);
        double invRz2 = 1.0 / (rz * rz);

        for (int x = minX; x <= maxX; x++) {
            double nx = (x + 0.5 - cx) * (x + 0.5 - cx) * invRx2;
            for (int y = minY; y <= maxY; y++) {
                double ny = (y + 0.5 - cy) * (y + 0.5 - cy) * invRy2;
                for (int z = minZ; z <= maxZ; z++) {
                    double nz = (z + 0.5 - cz) * (z + 0.5 - cz) * invRz2;
                    if (nx + ny + nz <= 1.0) {
                        parts.add(new Pair<>(new Vec3(x, y, z), mat));
                    }
                }
            }
        }
    }

    // -------------------------
    // Scaled simple rose
    // -------------------------
    public static <T> Function<Integer, Set<Pair<Vec3, PlatformBlockState<T>>>> smRoseTipDecoration(
            PlatformBlockState<T> leafMaterial,
            double scale
    ) {
        return (branchIndex) -> {
            Set<Pair<Vec3, PlatformBlockState<T>>> parts = new HashSet<>();
            // Map original ~1-unit pattern -> blocks via M (scale=1 -> ~5 block radius -> ~10 block diameter)
            double M = 5.0 * Math.max(0.0001, scale);

            // Core sphere
            double coreRadius = Math.max(1.0, 0.6 * M * 0.25); // small core relative to M
            addSphere(parts, 0.0, 0.0, 0.0, coreRadius, leafMaterial);

            // Petal ring (use ellipsoids to form flattened petals)
            double petalRadius = 0.8 * M;       // original local 0.8 -> scaled
            int petalCount = Math.max(6, (int) Math.round(6 * scale)); // more petals at larger scales
            double petalThickness = Math.max(0.6, 0.25 * M); // thickness/depth of petal mass
            double petalHeight = 0.3 * M;

            Random rnd = new Random(branchIndex * 9781L + 12345L);
            for (int i = 0; i < petalCount; i++) {
                double angle = (2 * Math.PI / petalCount) * i + (rnd.nextDouble() - 0.5) * 0.2;
                double cx = Math.cos(angle) * petalRadius;
                double cz = Math.sin(angle) * petalRadius * 0.85;
                double cy = petalHeight + (rnd.nextDouble() - 0.5) * (0.15 * M);

                // petals as ellipsoids: wider horizontally, flattened vertically
                double rx = Math.max(1.0, petalRadius * 0.25);
                double ry = Math.max(0.6, petalThickness * 0.25);
                double rz = Math.max(1.0, petalRadius * 0.15);
                addEllipsoid(parts, cx, cy, cz, rx, ry, rz, leafMaterial);
            }

            // Top crown / fluff
            int crownCount = Math.max(3, (int) Math.round(3 * scale));
            for (int i = 0; i < crownCount; i++) {
                double angle = rnd.nextDouble() * Math.PI * 2;
                double r = Math.max(0.6, 0.35 * M + rnd.nextDouble() * 0.25 * M);
                double x = Math.cos(angle) * r * 0.5;
                double z = Math.sin(angle) * r * 0.5;
                double y = 0.9 * M + rnd.nextDouble() * 0.6 * M;
                addSphere(parts, x, y, z, Math.max(0.6, M * 0.12), leafMaterial);
            }

            return parts;
        };
    }

    // -------------------------
    // Scaled multi-material rose (dark core, bright petals, mid accents)
    // -------------------------
    public static <T> Function<Integer, Set<Pair<Vec3, PlatformBlockState<T>>>> mmRoseTipDecoration(
            PlatformBlockState<T> darkMaterial,
            PlatformBlockState<T> brightMaterial,
            PlatformBlockState<T> midMaterial,
            double scale
    ) {
        return (branchIndex) -> {
            Set<Pair<Vec3, PlatformBlockState<T>>> parts = new HashSet<>();
            double M = 5.0 * Math.max(0.0001, scale);
            Random rnd = new Random(branchIndex * 7919L + 424242L);

            // Dark core (solid)
            double coreRadius = Math.max(1.0, 0.5 * M * 0.35);
            addSphere(parts, 0.0, 0.0, 0.0, coreRadius, darkMaterial);

            // Inner petal cluster (bright) — thicker and denser
            int innerPetalCount = Math.max(6, (int) Math.round(6 * scale));
            double innerRadius = Math.max(1.0, 0.6 * M);
            for (int i = 0; i < innerPetalCount; i++) {
                double angle = (2 * Math.PI / innerPetalCount) * i + (rnd.nextDouble() - 0.5) * 0.2;
                double cx = Math.cos(angle) * innerRadius * (0.9 + rnd.nextDouble() * 0.15);
                double cz = Math.sin(angle) * innerRadius * (0.9 + rnd.nextDouble() * 0.15);
                double cy = 0.2 * M + rnd.nextDouble() * 0.25 * M;
                // petals as ellipsoids (longer radially)
                addEllipsoid(parts, cx, cy, cz,
                        Math.max(1.0, innerRadius * 0.18),
                        Math.max(0.6, M * 0.18),
                        Math.max(1.0, innerRadius * 0.10),
                        brightMaterial);
            }

            // Outer mid accents that give depth (midMaterial)
            int accentCount = Math.max(4, (int) Math.round(4 * scale));
            double accentRadius = Math.max(1.2, 1.1 * M);
            for (int i = 0; i < accentCount; i++) {
                double angle = rnd.nextDouble() * 2 * Math.PI;
                double dist = accentRadius * (0.9 + (rnd.nextDouble() - 0.5) * 0.2);
                double cx = Math.cos(angle) * dist;
                double cz = Math.sin(angle) * dist;
                double cy = 0.5 * M + rnd.nextDouble() * 0.35 * M;
                addSphere(parts, cx, cy, cz, Math.max(0.7, M * 0.12), midMaterial);
            }

            // Top bloom highlights (mix bright + mid for variety)
            int crownCount = Math.max(3, (int) Math.round(3 * scale));
            for (int i = 0; i < crownCount; i++) {
                double angle = rnd.nextDouble() * 2 * Math.PI;
                double r = Math.max(0.4, 0.35 * M + rnd.nextDouble() * 0.3 * M);
                double x = Math.cos(angle) * r * 0.6;
                double z = Math.sin(angle) * r * 0.6;
                double y = 0.9 * M + rnd.nextDouble() * 0.5 * M;
                PlatformBlockState<T> pick = rnd.nextDouble() < 0.6 ? brightMaterial : midMaterial;
                addSphere(parts, x, y, z, Math.max(0.6, M * 0.12), pick);
            }

            return parts;
        };
    }

    public static <T> Set<Pair<Vec3, PlatformBlockState<T>>> singlePetal(
            PlatformBlockState<T> material,
            double scale
    ) {
        Set<Pair<Vec3, PlatformBlockState<T>>> parts = new HashSet<>();
        Random rnd = new Random();

        // Basic dimensions (scale=1 -> 10x10 size)
        double width = 4.0 * scale;
        double height = 2.0 * scale;
        double length = 5.0 * scale;

        // Curved petal surface approximation
        for (double t = 0; t <= 1.0; t += 0.05) { // vertical sweep
            for (double a = 0; a < Math.PI * 2; a += Math.PI / 20) {
                double radius = width * (1 - t * 0.8); // taper toward tip
                double x = Math.cos(a) * radius * 0.3;
                double y = Math.sin(a) * radius * 0.05 + t * height; // slightly curved upwards
                double z = t * length;

                // add random flutter
                x += (rnd.nextDouble() - 0.5) * 0.3 * scale;
                y += (rnd.nextDouble() - 0.5) * 0.1 * scale;

                parts.add(new Pair<>(new Vec3(x, y, z), material));
            }
        }
        return parts;
    }

    /**
     * Generates a single rose bloom.
     * Scale = 1 means about 10x10 blocks.
     */
    public static <T> Set<Pair<Vec3, PlatformBlockState<T>>> generateRose(
            double scale,
            PlatformBlockState<T> darkMaterial,
            PlatformBlockState<T> midMaterial,
            PlatformBlockState<T> brightMaterial
    ) {
        Set<Pair<Vec3, PlatformBlockState<T>>> parts = new HashSet<>();

        // 🌹 Define bloom structure
        int layerCount = (int) (4 * scale);
        double baseRadius = 1.5 * scale;
        double layerHeightStep = 0.4 * scale;
        double inwardCurl = 0.7;

        for (int layer = 0; layer < layerCount; layer++) {
            double layerProgress = (double) layer / (layerCount - 1);
            double radius = baseRadius * (1.0 - layerProgress * inwardCurl);
            double height = layer * layerHeightStep;

            // petals per layer (slightly randomized)
            int petalCount = (int) (5 + Math.random() * 2);
            double rotationOffset = Math.random() * Math.PI * 2;

            for (int i = 0; i < petalCount; i++) {
                double angle = (2 * Math.PI / petalCount) * i + rotationOffset;

                // 🌸 generate petal geometry
                Set<Pair<Vec3, PlatformBlockState<T>>> petal =
                        generatePetalMulti(scale, darkMaterial, midMaterial, brightMaterial);

                // 🌸 position the petal in a circle
                for (var p : petal) {
                    Vec3 pos = p.getKey();
                    double x = pos.x;
                    double y = pos.y;
                    double z = pos.z;

                    // rotate petal around Y
                    double rotX = Math.cos(angle) * x - Math.sin(angle) * z;
                    double rotZ = Math.sin(angle) * x + Math.cos(angle) * z;

                    // apply layer offsets
                    Vec3 finalPos = new Vec3(rotX + Math.cos(angle) * radius, y + height, rotZ + Math.sin(angle) * radius);
                    parts.add(new Pair<>(finalPos, p.getValue()));
                }
            }
        }

        // 🌑 add a core center
        parts.add(new Pair<>(new Vec3(0, 0, 0), darkMaterial));

        return parts;
    }

    /**
     * Generates a single curved petal (multi-material version).
     */
    public static <T> Set<Pair<Vec3, PlatformBlockState<T>>> generatePetalMulti(
            double scale,
            PlatformBlockState<T> darkMaterial,
            PlatformBlockState<T> midMaterial,
            PlatformBlockState<T> brightMaterial
    ) {
        Set<Pair<Vec3, PlatformBlockState<T>>> parts = new HashSet<>();

        double petalLength = 2.5 * scale;
        double petalWidth = scale;
        double petalHeight = 0.4 * scale;
        int res = (int) (8 * scale);

        for (int i = 0; i <= res; i++) {
            double t = (double) i / res;
            double y = Math.sin(t * Math.PI) * petalHeight; // gentle curve upward
            double x = (t - 0.5) * petalWidth * 2;
            double z = -t * petalLength;

            PlatformBlockState<T> mat;
            if (t < 0.3) mat = darkMaterial;
            else if (t < 0.7) mat = midMaterial;
            else mat = brightMaterial;

            parts.add(new Pair<>(new Vec3(x, y, z), mat));
        }

        return parts;
    }
}