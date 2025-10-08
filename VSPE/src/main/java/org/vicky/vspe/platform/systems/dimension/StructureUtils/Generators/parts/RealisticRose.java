package org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators.parts;

import org.vicky.platform.utils.Vec3;
import org.vicky.platform.world.PlatformBlockState;
import org.vicky.utilities.Pair;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.CurveFunctions;
import org.vicky.vspe.platform.systems.dimension.TimeCurve;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

// realistic rose tip generator — localized coordinates (x=left, y=up, z=forward)
public class RealisticRose {

    // rotate vector around X (left) axis
    private static Vec3 rotateX(Vec3 v, double angle) {
        double c = Math.cos(angle), s = Math.sin(angle);
        double y = v.y * c - v.z * s;
        double z = v.y * s + v.z * c;
        return new Vec3(v.x, y, z);
    }

    // rotate vector around Y (up) axis
    private static Vec3 rotateY(Vec3 v, double angle) {
        double c = Math.cos(angle), s = Math.sin(angle);
        double x = v.x * c - v.z * s;
        double z = v.x * s + v.z * c;
        return new Vec3(x, v.y, z);
    }

    /**
     * Single-material realistic rose tip.
     *
     * @param material       the block used for all petals
     * @param scale          scale where 1.0 ≈ 10 block diameter
     * @param steps          number of inner steps/layers (outer -> inner)
     * @param basePetalCount number of base petals (outer layer) — should be 7 per your spec
     * @param innerScale     minimum relative petal scale at center (0.1..1.0)
     */
    public static <T> Function<Integer, Set<Pair<Vec3, PlatformBlockState<T>>>> realisticRoseTipSingle(
            PlatformBlockState<T> material,
            double scale,
            int steps,
            int basePetalCount,
            double innerScale
    ) {
        return (branchIndex) -> {
            Set<Pair<Vec3, PlatformBlockState<T>>> parts = new HashSet<>();
            // deterministic per-branch randomness
            Random rnd = new Random(branchIndex * 0x9E3779B97F4A7C15L);

            double M = 5.0 * Math.max(0.0001, (scale / (branchIndex * 2))); // scale mapping so 1 -> radius ~5 -> diameter ~10

            // base petal geometry constants (will be scaled per layer)
            double basePetalLength = 1.8 * M; // outer petal extends ~1.8*M forward
            // lateral half-width
            double basePetalHeight = 0.45 * M; // curvature amplitude
            var petalFun =
                    CurveFunctions.radius(basePetalCount, basePetalCount * 0.5, 0.0, 1.0, TimeCurve.EXPONENTIAL);

            for (int layer = 0; layer < steps; layer++) {
                double progress = steps == 1 ? 1.0 : (double) layer / (double) (steps - 1); // 0 outer, 1 inner
                // tilt grows from 0 (horizontal) -> PI/2 (vertical) as we move inward
                double tilt = progress * (Math.PI / 2.0);
                // petal scale shrinks toward center (linear to innerScale)
                double petalScale = (1.0 - progress) + progress * innerScale;

                double petalLength = basePetalLength * petalScale;
                double petalWidth = M * petalScale;
                double petalHeight = basePetalHeight * petalScale;

                // radius from center where petal base sits (outer -> inner)
                double radius = M * (1.0 - progress * 0.9); // slightly pull inward each layer
                // stagger layer rotation so petals interdigitate
                double layerOffset = (layer % 2 == 0) ? 0.0 : (Math.PI / basePetalCount);

                int petalCount = (int) Math.round(petalFun.apply((double) layer / (steps - 1))); // keep constant per spec

                // sampling resolution (scale with petalScale)
                int resLen = Math.max(6, (int) Math.round(8 * petalScale));
                int resWid = Math.max(3, (int) Math.round(6 * petalScale));

                for (int pIdx = 0; pIdx < petalCount; pIdx++) {
                    double azimuth = 2.0 * Math.PI * pIdx / petalCount + layerOffset;

                    // build a single petal as a set of local points
                    for (int i = 0; i <= resLen; i++) {
                        double t = (double) i / resLen; // 0..1 along length
                        // gentle curve along length (height) — peaks near middle
                        double curveY = Math.sin(Math.PI * t) * petalHeight * (1.0 - 0.5 * t);

                        // width taper: widest near base, narrow at tip
                        double halfWidth = petalWidth * (1.0 - 0.85 * t);

                        for (int j = -resWid; j <= resWid; j++) {
                            double s = (double) j / resWid; // -1..1 across width
                            // lateral falloff to form a petal cross-section (more density near center)
                            double taper = Math.pow(1.0 - Math.abs(s), 1.6);
                            double localX = s * halfWidth * taper;
                            double localY = curveY;
                            double localZ = t * petalLength;

                            // small randomized flutter for natural look
                            double jitterX = (rnd.nextDouble() - 0.5) * 0.06 * M;
                            double jitterY = (rnd.nextDouble() - 0.5) * 0.03 * M;
                            double jitterZ = (rnd.nextDouble() - 0.5) * 0.05 * M;

                            Vec3 local = new Vec3(localX + jitterX, localY + jitterY, localZ + jitterZ);

                            // tilt around local X axis (left) so tip raises as tilt increases
                            Vec3 tilted = rotateX(local, tilt);

                            // rotate to azimuth around Y then translate radially
                            Vec3 rotated = rotateY(tilted, azimuth);
                            Vec3 placed = rotated.add(new Vec3(Math.cos(azimuth) * radius, 0.0, Math.sin(azimuth) * radius));

                            parts.add(new Pair<>(placed, material));
                        }
                    }
                }
            }

            // small dark core at center
            parts.add(new Pair<>(new Vec3(0.0, 0.0, 0.0), material));

            return parts;
        };
    }

    /**
     * Multi-material realistic rose tip (dark core -> mid -> bright tip).
     */
    public static <T> Function<Integer, Set<Pair<Vec3, PlatformBlockState<T>>>> realisticRoseTipMulti(
            PlatformBlockState<T> darkMaterial,
            PlatformBlockState<T> midMaterial,
            PlatformBlockState<T> brightMaterial,
            double scale,
            int steps,
            int basePetalCount,
            double innerScale
    ) {
        return (branchIndex) -> {
            Set<Pair<Vec3, PlatformBlockState<T>>> parts = new HashSet<>();
            Random rnd = new Random(branchIndex * 0xC13FA9A902A6328FL);

            double M = 5.0 * Math.max(0.0001, ((int) Math.round(scale / branchIndex)));

            double basePetalLength = 1.8 * M;
            double basePetalWidth = M;
            double basePetalHeight = 0.45 * M;

            for (int layer = 0; layer < steps; layer++) {
                double progress = steps == 1 ? 1.0 : (double) layer / (double) (steps - 1);
                double tilt = progress * (Math.PI / 2.0);
                double petalScale = (1.0 - progress) + progress * innerScale;

                double petalLength = basePetalLength * petalScale;
                double petalWidth = basePetalWidth * petalScale;
                double petalHeight = basePetalHeight * petalScale;

                double radius = M * (1.0 - progress * 0.9);
                double layerOffset = (layer % 2 == 0) ? 0.0 : (Math.PI / basePetalCount);

                int petalCount = basePetalCount;
                int resLen = Math.max(6, (int) Math.round(8 * petalScale));
                int resWid = Math.max(3, (int) Math.round(6 * petalScale));

                for (int pIdx = 0; pIdx < petalCount; pIdx++) {
                    double azimuth = 2.0 * Math.PI * pIdx / petalCount + layerOffset;
                    for (int i = 0; i <= resLen; i++) {
                        double t = (double) i / resLen;
                        double curveY = Math.sin(Math.PI * t) * petalHeight * (1.0 - 0.5 * t);
                        double halfWidth = petalWidth * (1.0 - 0.85 * t);

                        for (int j = -resWid; j <= resWid; j++) {
                            double s = (double) j / resWid;
                            double taper = Math.pow(1.0 - Math.abs(s), 1.6);
                            double localX = s * halfWidth * taper;
                            double localY = curveY;
                            double localZ = t * petalLength;

                            double jitterX = (rnd.nextDouble() - 0.5) * 0.06 * M;
                            double jitterY = (rnd.nextDouble() - 0.5) * 0.03 * M;
                            double jitterZ = (rnd.nextDouble() - 0.5) * 0.05 * M;

                            Vec3 local = new Vec3(localX + jitterX, localY + jitterY, localZ + jitterZ);

                            Vec3 tilted = rotateX(local, tilt);
                            Vec3 rotated = rotateY(tilted, azimuth);
                            Vec3 placed = rotated.add(new Vec3(Math.cos(azimuth) * radius, 0.0, Math.sin(azimuth) * radius));

                            // material gradient along t (0 base -> 1 tip)
                            PlatformBlockState<T> mat;
                            if (t < 0.30) mat = darkMaterial;
                            else if (t < 0.70) mat = midMaterial;
                            else mat = brightMaterial;

                            parts.add(new Pair<>(placed, mat));
                        }
                    }
                }
            }

            // center core - small ball of darkMaterial
            double coreRadius = Math.max(1.0, 0.3 * M);
            int coreRange = (int) Math.ceil(coreRadius);
            for (int x = -coreRange; x <= coreRange; x++) {
                for (int y = -coreRange; y <= coreRange; y++) {
                    for (int z = -coreRange; z <= coreRange; z++) {
                        double dx = x + 0.5;
                        double dy = y + 0.5;
                        double dz = z + 0.5;
                        if (dx * dx + dy * dy + dz * dz <= coreRadius * coreRadius) {
                            parts.add(new Pair<>(new Vec3(x, y, z), darkMaterial));
                        }
                    }
                }
            }

            return parts;
        };
    }

    // Convenience overloads with sensible defaults
    public static <T> Function<Integer, Set<Pair<Vec3, PlatformBlockState<T>>>> realisticRoseTipMulti(
            PlatformBlockState<T> darkMaterial,
            PlatformBlockState<T> midMaterial,
            PlatformBlockState<T> brightMaterial,
            double scale
    ) {
        return realisticRoseTipMulti(darkMaterial, midMaterial, brightMaterial, scale, /*steps=*/5, /*petals=*/7, /*innerScale=*/0.25);
    }

    public static <T> Function<Integer, Set<Pair<Vec3, PlatformBlockState<T>>>> realisticRoseTipSingle(
            PlatformBlockState<T> mat,
            double scale
    ) {
        return realisticRoseTipSingle(mat, scale, /*steps=*/5, /*petals=*/7, /*innerScale=*/0.25);
    }
}
