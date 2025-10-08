package org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators;

import org.vicky.platform.utils.Vec3;
import org.vicky.platform.world.PlatformBlockState;
import org.vicky.platform.world.PlatformWorld;
import org.vicky.utilities.ContextLogger.ContextLogger;
import org.vicky.utilities.Pair;
import org.vicky.vspe.BlockVec3i;
import org.vicky.vspe.Direction;
import org.vicky.vspe.branch.*;
import org.vicky.vspe.platform.systems.dimension.MushroomCapProfile;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.*;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.factories.HelixPointFactory;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.factories.StraightPointFactory;
import org.vicky.vspe.platform.systems.dimension.TimeCurve;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.BlockPlacement;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.RandomSource;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class NoAIProceduralTreeGenerator<T> extends ProceduralStructureGenerator<T> {

    private final NoAIPTGBuilder<T> p;
    private final Map<List<Vec3>, Integer> leafingPoints = new HashMap<>();
    private int trunkHeight;

    private static List<Vec3> resampleByArcLength(List<Vec3> pts, int targetCount) {
        if (pts == null || pts.size() < 2 || targetCount < 2) return new ArrayList<>(pts);

        // compute segment lengths
        double[] seg = new double[pts.size() - 1];
        double total = 0.0;
        for (int i = 0; i < pts.size() - 1; i++) {
            Vec3 a = pts.get(i), b = pts.get(i + 1);
            double dx = b.x - a.x;
            double dy = b.y - a.y;
            double dz = b.z - a.z;
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            seg[i] = d;
            total += d;
        }

        // if degenerate length -> just return evenly sampled indices
        if (total <= 1e-9) {
            List<Vec3> out = new ArrayList<>();
            for (int i = 0; i < targetCount; i++) {
                out.add(pts.get(0));
            }
            return out;
        }

        // cumulative lengths
        double[] cum = new double[seg.length + 1];
        cum[0] = 0.0;
        for (int i = 0; i < seg.length; i++) cum[i + 1] = cum[i] + seg[i];

        List<Vec3> out = new ArrayList<>(targetCount);
        for (int s = 0; s < targetCount; s++) {
            double t = (double) s / (targetCount - 1) * total;
            // find segment k where cum[k] <= t <= cum[k+1]
            int k = Arrays.binarySearch(cum, t);
            if (k < 0) k = -k - 2;
            if (k < 0) k = 0;
            if (k >= seg.length) {
                out.add(pts.get(pts.size() - 1));
                continue;
            }
            double local = (t - cum[k]) / (seg[k] <= 1e-9 ? 1.0 : seg[k]); // alpha between k and k+1
            Vec3 a = pts.get(k), b = pts.get(k + 1);
            Vec3 interp = new Vec3(
                    a.x + (b.x - a.x) * local,
                    a.y + (b.y - a.y) * local,
                    a.z + (b.z - a.z) * local
            );
            out.add(interp);
        }
        return out;
    }

    public NoAIProceduralTreeGenerator(NoAIPTGBuilder<T> builder) {
        this.p = builder;
        airFiller = false;
    }

    @Override
    public BlockVec3i getApproximateSize() {
        // Conservative horizontal radius:
        // trunk radius + main branch reach + branch thickness + small margin
        int horizRadius = (int) Math.round(p.trunkWidthRange.value() + (p.trunkWidthRange.value() * (p.branchSizeDecay * p.branchDepth)));

        // Width and depth (square footprint)
        int width = horizRadius * 2 + 1;

        // Vertical extents:
        // upward: main trunk height + branch vertical excursion + a margin
        int up = (int) Math.round(p.trunkHeightRange.value() + (p.trunkHeightRange.value() * (p.branchSizeDecay * p.branchDepth)));

        int height = up + 1; // +1 to be safe (inclusive bounds)

        return new BlockVec3i(width, height, width);
    }

    @Override
    protected void performGeneration(RandomSource rnd, Vec3 origin, List<BlockPlacement<T>> outPlacements, Map<Long, BiConsumer<PlatformWorld<T, ?>, Vec3>> outActions) {
        submitSubtask(sub -> generateTrunk(rnd, origin, sub));

        if (p.leafType != LeafPopulationType.NO_LEAVES) {
            submitFinalisingSubtask(sub -> generateLeaves(sub, rnd.fork(rnd.nextLong())));
        }
    }

    private void generateTrunk(RandomSource rnd, Vec3 origin, SubGenerator generator) {
        trunkHeight = rnd.nextInt(p.trunkHeightRange.key(), p.trunkHeightRange.value());
        int trunkWidth = rnd.nextInt(p.trunkWidthRange.key(), p.trunkWidthRange.value());

        int finalTrunkHeight = trunkHeight;
        Set<Vec3> UnOptimisedMemo = switch (p.trunkType) {
            case BONSAI -> {
                Function<Double, Double> radiusFunction =
                        CurveFunctions.radius((double) trunkWidth / 2, 0.0, 0.1, 1.0, TimeCurve.INVERTED_QUADRATIC);
                Function<Double, Double> thicknessFunction =
                        CurveFunctions.radius(3, 0.0, 0.1, 1.0, TimeCurve.INVERTED_QUADRATIC);
                Function<Double, Double> pitchFunction =
                        CurveFunctions.pitch(0.03, 0.1, 0.2, 1.0, TimeCurve.EASE_IN_OUT_CUBIC);
                var points = CorePointsFactory.generate(CorePointsFactory.Params.builder()
                        .type(
                                new HelixPointFactory(
                                        3,
                                        0.8,
                                        1)
                        )
                        .origin(origin)
                        .divergenceDecay(0.6)
                        .segments((int) (50 * p.quality))
                        .height(trunkHeight)
                        .width(p.bonsaiTreeHelixSpan)
                        .divergenceProbability(0.8)
                        .divergenceStrength(0.5)
                        .noiseSmooth(0.2)
                        .pitchDegrees(-60)
                        .noiseStrength(0.7)
                        .build()
                );
                points.addFirst(new Vec3(5, -3, 5).add(origin));
                points.addFirst(new Vec3(0, -15, 0).add(origin));

                if (p.leafType == LeafPopulationType.HANGING || p.leafType == LeafPopulationType.HANGING_MUSHROOM || p.leafType == LeafPopulationType.HANGING_FUZZY || p.leafType == LeafPopulationType.THICK_HANGING) {
                    var hehe = new ArrayList<Vec3>();
                    int start = (int) Math.round(points.size() * 0.57);
                    for (int i = start; i < points.size(); i++) {
                        hehe.add(points.get(i));
                    }
                    leafingPoints.put(hehe, 1);
                }

                if (p.branchType != BranchingType.NO_BRANCHES)
                    submitSubtask(sub -> generateBranches(sub, rnd.fork(rnd.nextLong()), points, finalTrunkHeight, radiusFunction, thicknessFunction, 1));
                yield SpiralUtil.generateVineWithSpiral(points, thicknessFunction, 7, 0.8f, radiusFunction, pitchFunction);
            }
            case CONIFEROUS -> {
                Function<Double, Double> radiusFunction =
                        CurveFunctions.radius((double) trunkWidth / 2, 0.0, 0.1, 1.0, TimeCurve.INVERTED_QUADRATIC);
                Function<Double, Double> thicknessFunction =
                        CurveFunctions.radius(3, 0.0, 0.1, 1.0, TimeCurve.INVERTED_QUADRATIC);
                Function<Double, Double> pitchFunction =
                        CurveFunctions.pitch(0.03, 0.1, 0.2, 1.0, TimeCurve.EASE_IN_OUT_CUBIC);

                var points = CorePointsFactory.generate(CorePointsFactory.Params.builder()
                        .type(
                                new StraightPointFactory()
                        )
                        .origin(origin)
                        .divergenceDecay(0.3)
                        .segments((int) (20 * p.quality))
                        .height(trunkHeight)
                        .width(trunkWidth)
                        .divergenceProbability(0.4)
                        .divergenceStrength(0.2)
                        .noiseSmooth(0.5)
                        .noiseStrength(0.2)
                        .build()
                );
                points.addFirst(new Vec3(0, -3, 0).add(origin));
                points.addFirst(new Vec3(0, -15, 0).add(origin));

                if (p.branchType != BranchingType.NO_BRANCHES)
                    submitSubtask(sub -> generateBranches(sub, rnd.fork(rnd.nextLong()), points, finalTrunkHeight, radiusFunction, thicknessFunction, 1));
                yield SpiralUtil.generateVineWithSpiral(points, thicknessFunction, 7, 0.8f, radiusFunction, pitchFunction);
            }
            case MULTI_TRUNKED -> {
                Set<Vec3> Unloaded = new HashSet<>();
                Function<Double, Double> radiusFunction =
                        CurveFunctions.radius((double) trunkWidth / 2, (double) trunkWidth / 4, 0.1, 1.0, TimeCurve.QUADRATIC);
                Function<Double, Double> thicknessFunction =
                        CurveFunctions.radius(3, 0.0, 0.1, 1.0, TimeCurve.QUADRATIC);
                Function<Double, Double> pitchFunction =
                        CurveFunctions.pitch(0.03, 0.1, 0.2, 1.0, TimeCurve.EASE_IN_OUT_CUBIC);
                Vec3 stompStop = new Vec3(0, 0, 0);
                {
                    var points = CorePointsFactory.generate(CorePointsFactory.Params.builder()
                            .type(new StraightPointFactory())
                            .origin(origin)
                            .divergenceDecay(0.7)
                            .segments((int) (20 * p.quality))
                            .height(trunkHeight * p.multiTrunkStartPoint)
                            .width(trunkWidth)
                            .divergenceProbability(0.8)
                            .divergenceStrength(0.4)
                            .noiseSmooth(0.1)
                            .noiseStrength(0.6)
                            .yawDegrees(rnd.nextInt(0, p.multiTrunkAngleMax))
                            .pitchDegrees(rnd.nextInt(0, p.multiTrunkAngleMax))
                            .build()
                    );
                    points.addFirst(new Vec3(0, -3, 0).add(origin));
                    points.addFirst(new Vec3(0, -15, 0).add(origin));

                    for (var point : points) {
                        if (point.y > stompStop.y) stompStop = point;
                    }

                    Unloaded.addAll(SpiralUtil.generateVineWithSpiral(points, thicknessFunction, 7, 0.8f, radiusFunction, pitchFunction));
                }

                for (int i = 0; i < p.multiTrunkNumber; i++) {
                    trunkHeight = rnd.nextInt(p.trunkHeightRange.key(), p.trunkHeightRange.value());
                    trunkWidth = rnd.nextInt((int) (p.trunkWidthRange.key() * 0.6), (int) (p.trunkWidthRange.value() * 0.6));
                    radiusFunction =
                            CurveFunctions.radius((trunkWidth * 0.8) / 2, (trunkWidth * 0.8) / 4, 0.0, 1.0, TimeCurve.LINEAR);
                    thicknessFunction =
                            CurveFunctions.radius(3.0, 1.0, 0.0, 1.0, TimeCurve.QUADRATIC);
                    pitchFunction =
                            CurveFunctions.pitch(0.05, 0.1, 0.3, 1.0, TimeCurve.EASE_IN_OUT_CUBIC);
                    double baseAngle = (360.0 / p.multiTrunkNumber) * i;
                    double jitter = rnd.nextInt(0, p.multiTrunkAngleMax);
                    var points = CorePointsFactory.generate(
                            CorePointsFactory.Params.builder()
                                    .origin(origin)
                                    .type(new StraightPointFactory())
                                    .divergenceDecay(0.7)
                                    .segments((int) (20 * p.quality))
                                    .height(trunkHeight)
                                    .width(trunkWidth)
                                    .divergenceProbability(0.8)
                                    .divergenceStrength(0.4)
                                    .noiseSmooth(0.1)
                                    .noiseStrength(0.6)
                                    .yawDegrees((int) (baseAngle + jitter))
                                    .pitchDegrees(Math.abs((int) (jitter)))
                                    .build()
                    );
                    points.addFirst(new Vec3(0, -3, 0).add(origin));
                    points.addFirst(new Vec3(0, -15, 0).add(origin));

                    List<Vec3> points2 = new ArrayList<>();
                    for (var point : points) {
                        points2.add(point.add(stompStop));
                    }

                    Function<Double, Double> finalRadiusFunction = radiusFunction;
                    int finalTrunkHeight1 = trunkHeight;

                    if (p.branchType != BranchingType.NO_BRANCHES) {
                        Function<Double, Double> finalThicknessFunction = thicknessFunction;
                        submitSubtask(sub -> generateBranches(sub, rnd.fork(rnd.nextLong()), points, finalTrunkHeight1, finalRadiusFunction, finalThicknessFunction, 1));
                    }
                    Unloaded.addAll(SpiralUtil.generateVineWithSpiral(points2, thicknessFunction, 7, 0.8f, radiusFunction, pitchFunction));
                }
                yield Unloaded;
            }
            case SLANTED -> {
                Function<Double, Double> radiusFunction =
                        CurveFunctions.radius((double) trunkWidth / 2, 0.0, 0.1, 1.0, TimeCurve.QUADRATIC);
                Function<Double, Double> thicknessFunction =
                        CurveFunctions.radius(3, 0.0, 0.1, 1.0, TimeCurve.QUADRATIC);
                Function<Double, Double> pitchFunction =
                        CurveFunctions.pitch(0.03, 0.1, 0.2, 1.0, TimeCurve.EASE_IN_OUT_CUBIC);

                int angle = rnd.nextInt(p.slantAngleRange.key(), p.slantAngleRange.value());
                var points = CorePointsFactory.generate(
                        CorePointsFactory.Params.builder()
                                .type(new StraightPointFactory())
                                .origin(origin)
                                .divergenceDecay(0.2)
                                .segments((int) (20 * p.quality))
                                .height(trunkHeight)
                                .width(trunkWidth)
                                .divergenceProbability(0.5)
                                .divergenceStrength(0.4)
                                .noiseSmooth(0.1)
                                .noiseStrength(0.3)
                                .yawDegrees((int) ((double) angle * 1.2))
                                .pitchDegrees(Math.abs((int) ((double) angle)))
                                .build()
                );
                points.addFirst(new Vec3(0, -3, 0).add(origin));
                points.addFirst(new Vec3(0, -15, 0).add(origin));

                int finalTrunkHeight1 = trunkHeight;
                if (p.branchType != BranchingType.NO_BRANCHES)
                    submitSubtask(sub -> generateBranches(sub, rnd.fork(rnd.nextLong()), points, finalTrunkHeight1, radiusFunction, thicknessFunction, 1));
                yield SpiralUtil.generateVineWithSpiral(points, thicknessFunction, 7, 0.8f, radiusFunction, pitchFunction);
            }
            case TAPERED_SPINDLE -> {
                Function<Double, Double> radiusFunction =
                        CurveFunctions.radius((double) trunkWidth / 3, 0.0, 0.1, 1.0, TimeCurve.INVERTED_CUBIC);
                Function<Double, Double> thicknessFunction =
                        CurveFunctions.radius(3, 0.0, 0.1, 1.0, TimeCurve.INVERTED_QUADRATIC);
                Function<Double, Double> pitchFunction =
                        CurveFunctions.pitch(0.03, 0.1, 0.2, 1.0, TimeCurve.EASE_IN_OUT_CUBIC);

                var points = CorePointsFactory.generate(
                        CorePointsFactory.Params.builder()
                                .type(new StraightPointFactory())
                                .divergenceDecay(0.2 + p.randomness)
                                .origin(origin)
                                .segments((int) (30 * p.quality))
                                .height(trunkHeight)
                                .width(10)
                                .divergenceProbability(0.5 + p.randomness)
                                .divergenceStrength(0.4 + p.randomness)
                                .noiseSmooth(0.1)
                                .noiseStrength(0.7 * p.randomness)
                                .noiseWidthFactor(3)
                                // .pitchDegrees(-90)
                                .build());

                if (p.leafType == LeafPopulationType.HANGING || p.leafType == LeafPopulationType.HANGING_MUSHROOM || p.leafType == LeafPopulationType.HANGING_FUZZY || p.leafType == LeafPopulationType.THICK_HANGING) {
                    var hehe = new ArrayList<Vec3>();
                    int start = (int) Math.round(points.size() * 0.57);
                    for (int i = start; i < points.size(); i++) {
                        hehe.add(points.get(i));
                    }
                    leafingPoints.put(hehe, 1);
                }

                if (p.taperedFuzz) {
                    int matCount = p.vineSequenceMaterial.size();

                    int lastPlacedIndex = -999;
                    var trunkpath = BezierCurve.generatePoints(points, 200);

                    // iterate along trunk centerline and spawn a vine starting point on trunk surface (outward)
                    for (int i = 0; i < trunkpath.size(); i++) {
                        // random skip
                        if (rnd.nextDouble() > p.leafPropagationChance) continue;

                        // simple enforced spacing between vines
                        if (i - lastPlacedIndex < 10) continue;

                        // only place along lower ~75% of trunkpath (same as original)
                        if (((double) i / (double) trunkpath.size()) > 0.75) break;

                        // fraction along trunk (0..1)
                        double frac = (double) i / Math.max(1, (trunkpath.size() - 1));

                        // radius at this point
                        double localRadius = radiusFunction.apply(1.0 - frac);

                        // compute a smoothed tangent (central difference when possible)
                        Vec3 tangent;
                        if (i == 0) {
                            tangent = trunkpath.get(1).subtract(trunkpath.get(0));
                        } else if (i == trunkpath.size() - 1) {
                            tangent = trunkpath.get(i).subtract(trunkpath.get(i - 1));
                        } else {
                            tangent = trunkpath.get(i + 1).subtract(trunkpath.get(i - 1)).multiply(0.5);
                        }
                        if (tangent.lengthSq() < 1e-6) tangent = new Vec3(1, 0, 0);
                        // project tangent into horizontal plane so outward is horizontal
                        Vec3 tangXZ = new Vec3(tangent.x, 0, tangent.z);
                        if (tangXZ.lengthSq() < 1e-6) tangXZ = new Vec3(1, 0, 0);
                        tangXZ = tangXZ.normalize();

                        // rotate 90 degrees in XZ to get outward direction (out from trunk)
                        Vec3 outward = new Vec3(-tangXZ.z, 0, tangXZ.x).normalize();
                        if (outward.lengthSq() < 1e-6) outward = new Vec3(1, 0, 0);

                        // place the vine base at the trunk surface: center + outward * (radius + padding)
                        // small downward nudge so vine attaches just under the surface (optional)
                        Vec3 surfaceBase = trunkpath.get(i)
                                .add(outward.multiply(localRadius + 0.5))   // push outside the trunk
                                .add(0.0, -0.5, 0.0);                           // tiny downward nudge so vine hangs from surface

                        lastPlacedIndex = i; // record that we placed a vine here

                        // vine length (longer toward base of trunk)
                        int vineHeight = (int) Math.max(3, Math.round(7 * (1 - frac)));

                        var pts = CorePointsFactory.generate(
                                CorePointsFactory.Params.builder()
                                        .noiseStrength(0.7)
                                        .divergenceStrength(0.7)
                                        .noiseSmooth(0.1)
                                        .height(vineHeight)
                                        .segments(Math.min(vineHeight * 8, 200))
                                        .build()
                        );

                        // place vine points downward from the surface base (don't subtract into trunk)
                        for (var vec : pts) {
                            double clampedY = Math.max(0.0, Math.min(vineHeight, vec.y));
                            // vec.x, vec.z are lateral offsets from the vine's local axis; vec.y is distance along vine
                            Vec3 localOffset = new Vec3(vec.x, -clampedY, vec.z); // negative y => hang downward

                            Vec3 world = surfaceBase.add(localOffset);

                            int wx = (int) Math.round(world.x);
                            int wy = (int) Math.round(world.y);
                            int wz = (int) Math.round(world.z);

                            int idx = Math.min(matCount - 1, Math.max(0, (int) Math.floor((clampedY / (double) vineHeight) * matCount)));
                            var material = p.vineSequenceMaterial.get(idx);

                            // optional safety: skip placement if it would write inside trunk center
                            // double distSq = new Vec3(world.x - trunkpath.get(i).x, 0, world.z - trunkpath.get(i).z).lengthSq();
                            // if (distSq < (localRadius * localRadius * 0.25)) continue;

                            generator.guardAndStore(new Vec3(wx, wy, wz), material, false);
                        }
                    }
                }

                points.addFirst(new Vec3(0, -3, 0).add(origin));
                points.addFirst(new Vec3(0, -15, 0).add(origin));

                if (p.branchType != BranchingType.NO_BRANCHES)
                    submitSubtask(sub -> generateBranches(sub, rnd.fork(rnd.nextLong()), points, finalTrunkHeight, radiusFunction, thicknessFunction, 1));

                yield SpiralUtil.generateVineWithSpiral(points, thicknessFunction, 5, 0.8f, radiusFunction, pitchFunction);
            }
        };


        for (var vec : UnOptimisedMemo) {
            generator.guardAndStore(vec, p.woodMaterial, false);
        }
    }

    private void generateBranches(SubGenerator subGenerator, RandomSource rnd, List<Vec3> trunkPath, int startLength, Function<Double, Double> startWidth, Function<Double, Double> thickness, int curr) {
        if (startLength < 1 || trunkPath == null || trunkPath.size() < 3) {
            LOGGER.print("Failed Check: sl: " + startLength + " tp: " + trunkPath);
            return;
        }

        double lastHeight = -Double.MAX_VALUE; // sentinel
        int added = 0;
        boolean useMulti = false;
        trunkPath = BezierCurve.generatePoints(trunkPath, 200);
        TimeCurve spreadType = TimeCurve.EXPONENTIAL_OUT;
        double branchSmalize = 1.0;
        int amount = 0;
        int prevI = 0;

        if (p.branchType == BranchingType.CONIFEROUS)
            amount = p.coniferousBranchRange.value();

        for (int i = 1; i < trunkPath.size() - 1; i++) {
            if (added >= (p.maxBranchAmount / curr)) break;
            if (rnd.nextDouble() > p.branchPropagationChance) continue;
            if (i < prevI + p.spacing) continue;

            double heightFactor = (double) i / (trunkPath.size() - 1.0);
            if (heightFactor < (Math.min(p.branchingPointRange.key() * curr, p.branchingPointRange.value() - 0.3)) || heightFactor > p.branchingPointRange.value())
                continue;

            Pair<BranchGenerator, RingBranchRule> gottens = switch (p.branchType) {
                case WIND_SWEPT -> {
                    spreadType = TimeCurve.INVERTED_QUADRATIC;
                    yield new Pair<>(new DirectableBranch(p.windFlowDirection, 40, 20, 0.7),
                            new RingBranchRule(1, Math.toRadians(10)));
                }
                case BULBED -> null;
                case SQUIGGLY -> null;
                case CONIFEROUS -> {
                    spreadType = TimeCurve.INVERTED_QUADRATIC;
                    useMulti = true;
                    branchSmalize = Math.pow(0.9, amount);
                    yield new Pair<>(new MultiAroundBranch(70 * heightFactor, 0.0),
                            new RingBranchRule(Math.max((int) (amount * 0.5), (int) (amount * ((1 - heightFactor) * 2))), Math.toRadians(0)));
                }
                case TAPERED_SPINDLE -> new Pair<>(new CrookedBranch(0.5, 270, 60),
                        new RingBranchRule(1, Math.toRadians(10)));
                case PALMLIKE -> null;
                case DICHOTOMOUS -> null;
                case BROOMED -> null;
                case OPPOSITE -> null;
                case TRIANGULAR -> null;
                case NO_BRANCHES -> null;
            };

            var cheesy =
                    (int) Math.round(Math.max(2, startLength * p.branchSizeDecay *
                            (curr == 1 ? (1 - heightFactor) * 2 : (1 - heightFactor) / 2) *
                            ((double) curr * 0.8)));

            BranchGenerator gen = gottens.key();
            RingBranchRule rule = gottens.value();
            BranchingEngine engine = new BranchingEngine(gen, rule, 0.0, 0.0, useMulti);

            Vec3 prev = trunkPath.get(i - 1);
            Vec3 cur = trunkPath.get(i);
            Vec3 next = trunkPath.get(i + 1);
            Vec3 tangent = next.subtract(prev).normalize();

            double trunkRadiusAtHeight = startWidth.apply(heightFactor);
            double trunkThicknessAtHeight = thickness.apply(heightFactor);

            // --- spacing rule ---
            // min spacing proportional to parent radius
            double minSpacing = Math.min(0.15, 0.04 / (heightFactor / 2));
            if (Math.abs(heightFactor - lastHeight) < minSpacing) continue;

            var attachment = new AttachmentPoint(cur, tangent,
                    (trunkRadiusAtHeight + trunkThicknessAtHeight),
                    heightFactor);
            lastHeight = heightFactor;
            added++;

            List<List<Vec3>> branches =
                    engine.generateAll(rnd, List.of(attachment), 10 / curr,
                            cheesy);

            for (var branch : branches) {
                var startThickness = CurveFunctions.radius(1, 0, 0.0, 1.0, spreadType);
                double baseBranchRadius = trunkRadiusAtHeight + trunkThicknessAtHeight;
                var startRadius = CurveFunctions.radius(
                        baseBranchRadius * branchSmalize,
                        baseBranchRadius * 0.2 * branchSmalize,
                        0.05, 1.0,
                        spreadType
                );

                if (curr < p.branchDepth) {
                    submitSubtask(subGenerator1 -> generateBranches(
                            subGenerator1, rnd.fork(rnd.nextLong()), branch,
                            cheesy, startRadius, startThickness, curr + 1));
                }

                if (p.leafType == LeafPopulationType.HANGING_FUZZY) {
                    leafingPoints.put(SpiralUtil.generateVineWithSpiral(branch,
                            startThickness, 7, 0.8f, startRadius,
                            CurveFunctions.pitch(0.01, 0.05, 0.2, 1.0, TimeCurve.INVERTED_QUADRATIC)).stream().toList(), curr + 1);
                } else {
                    leafingPoints.put(branch, curr + 1);
                }

                for (var pos : SpiralUtil.generateVineWithSpiral(branch,
                        startThickness, 7, 0.8f, startRadius,
                        CurveFunctions.pitch(0.01, 0.05, 0.2, 1.0, TimeCurve.INVERTED_QUADRATIC))
                ) {
                    subGenerator.guardAndStore(pos, p.woodMaterial, false);
                }
            }
        }
    }

    private void generateLeaves(SubGenerator subGenerator, RandomSource rnd) {
        if (leafingPoints.isEmpty()) return;

        switch (p.leafType) {
            case HANGING -> {
                int matCount = p.vineSequenceMaterial.size();
                if (matCount == 0) return;

                for (var entry : leafingPoints.entrySet()) {
                    var branch = entry.getKey();
                    if (branch == null || branch.isEmpty()) continue;

                    int lastPlacedIndex = -(int) Math.ceil(p.spacing); // ensure first placement allowed
                    for (int i = 0; i < branch.size(); i++) {
                        // random skip still allowed
                        if (rnd.nextDouble() > p.leafPropagationChance) continue;

                        // enforce spacing using lastPlacedIndex (fix mixing i/index semantics)
                        if (i - lastPlacedIndex < p.spacing) continue;

                        if (lastPlacedIndex >= p.maxVinePerBranch) break;

                        int vineHeight = Math.max(1, (int) Math.round(trunkHeight * (rnd.nextDouble() * p.vineHeight)) / entry.getValue());
                        Vec3 base = branch.get(i);
                        lastPlacedIndex++; // count placed vine

                        var pts = CorePointsFactory.generate(
                                CorePointsFactory.Params.builder()
                                        .noiseStrength(0.7)
                                        .divergenceStrength(0.7)
                                        .noiseSmooth(0.1)
                                        .height(vineHeight)
                                        .segments(Math.max(8, Math.min(vineHeight * 8, 200)))
                                        .build()
                        );

                        // smooth + resample by arc length to avoid irregular gaps/jumps
                        pts = BezierCurve.generatePoints(pts, Math.min(200, Math.max(32, vineHeight * 8)));
                        pts = resampleByArcLength(pts, Math.max(Math.min(vineHeight * 4, 200), 8));

                        // clamp y and ensure it ranges 0..vineHeight (prevents weird progress >1 or negative)
                        for (var vec : pts) {
                            double clampedY = Math.max(0.0, Math.min(vineHeight, vec.y));
                            // replace vec.y if needed (if Vec3 is immutable, construct new)
                            vec = new Vec3(vec.x, clampedY, vec.z);
                            double progress = clampedY / (double) vineHeight;
                            int idx = Math.min(matCount - 1, Math.max(0, (int) Math.floor(progress * matCount)));
                            var material = p.vineSequenceMaterial.get(idx);
                            subGenerator.guardAndStore(base.subtract(vec), material, false);
                        }
                    }
                }
            }
            case THICK_HANGING -> {
                int matCount = p.vineSequenceMaterial.size();
                if (matCount == 0) return;

                for (var entry : leafingPoints.entrySet()) {
                    var branch = entry.getKey();
                    if (branch == null || branch.isEmpty()) continue;

                    int lastPlacedIndex = -p.spacing;
                    for (int i = 0; i < branch.size(); i++) {
                        if (rnd.nextDouble() > p.leafPropagationChance) continue;
                        if (i - lastPlacedIndex < p.spacing) continue;
                        if (lastPlacedIndex >= p.maxVinePerBranch) break;

                        int vineHeight = Math.max(2, (int) Math.round(trunkHeight * (rnd.nextDouble() * p.vineHeight))) / entry.getValue();
                        Vec3 base = branch.get(i);
                        lastPlacedIndex++;

                        var pts = CorePointsFactory.generate(
                                CorePointsFactory.Params.builder()
                                        .noiseStrength(0.2)
                                        .divergenceStrength(0.7)
                                        .noiseSmooth(0.1)
                                        .segments(vineHeight)
                                        .height(vineHeight)
                                        .build()
                        );

                        for (int j = 0; j < pts.size(); j++) {
                            var vec = pts.get(j);
                            double pprogress = Math.max(0.0, Math.min(1.0, j / (double) pts.size()));
                            // System.out.println("PROGRESS: " + pprogress);
                            // System.out.println("Y: " + vec.y);
                            int idx = Math.min(matCount - 1, Math.max(0, (int) Math.floor(pprogress * matCount)));
                            var material = p.vineSequenceMaterial.get(idx);

                            double diameter = Math.max(1.0, p.vineThickness.apply(pprogress)) / entry.getValue();
                            double radius = diameter * 0.5;
                            // System.out.println("DIAMETER: " + diameter);

                            Vec3 rawPos = base.subtract(vec);
                            Vec3 center = rawPos;

                            // voxelize sphere
                            double rSq = radius * radius;
                            int rCeil = Math.max(1, (int) Math.ceil(radius));

                            int baseX = (int) Math.floor(center.x);
                            int baseY = (int) Math.floor(center.y);
                            int baseZ = (int) Math.floor(center.z);

                            for (int dx = -rCeil; dx <= rCeil; dx++) {
                                for (int dy = -rCeil; dy <= rCeil; dy++) {
                                    for (int dz = -rCeil; dz <= rCeil; dz++) {
                                        double bx = baseX + dx + 0.5;
                                        double by = baseY + dy + 0.5;
                                        double bz = baseZ + dz + 0.5;
                                        double ddx = bx - center.x;
                                        double ddy = by - center.y;
                                        double ddz = bz - center.z;
                                        double distSq = ddx * ddx + ddy * ddy + ddz * ddz;
                                        if (distSq <= rSq + 1e-9) {
                                            int wx = baseX + dx;
                                            int wy = baseY + dy;
                                            int wz = baseZ + dz;
                                            // System.out.println("X: " + wx + " Y: " + wy + " Z: " + wz);
                                            subGenerator.guardAndStore(wx, wy, wz, material, false, 1);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            case MUSHROOMCAP -> {
                Map<Vec3, Integer> capCenters = new HashMap<>();
                for (var entry : leafingPoints.entrySet()) {
                    var branch = entry.getKey();
                    if (branch == null || branch.isEmpty()) continue;
                    branch.stream()
                            .max(Comparator.comparingDouble(v -> v.y))
                            .ifPresent(it -> capCenters.put(it, entry.getValue()));
                }

                for (var entry : capCenters.entrySet()) {
                    var center = entry.getKey();
                    int capWidth = rnd.nextInt(p.mushroomCapWidth.key(), p.mushroomCapWidth.value()) / entry.getValue();
                    int capHeight = rnd.nextInt(p.mushroomCapHeight.key(), p.mushroomCapHeight.value()) / entry.getValue();
                    if (capWidth < 1) capWidth = 1;
                    if (capHeight < 1) capHeight = 3;

                    final double maxRadius = capWidth / 2.0; // treat capWidth as diameter
                    final double rimThicknessFrac = 0.32; // fraction of capWidth giving vertical rim thickness
                    final int rimThicknessBlocks = Math.max(1, (int) Math.round(capWidth * rimThicknessFrac));
                    final double deformationScale = 0.18; // how dramatic the wobbles are (multiples of radius)
                    // hemisphere vs parabolic: "hemisphere" gives rounded dome, "parabola" gives shallower cap
                    final String profile = "hemisphere"; // options: "hemisphere" | "parabola" | "gaussian"
                    // ====================

                    final int cx = (int) Math.round(center.x);
                    final int cz = (int) Math.round(center.z);
                    final int baseY = (int) Math.round(center.y);

                    int bound = (int) Math.ceil(maxRadius) + 1;

                    for (int dz = -bound; dz <= bound; dz++) {
                        for (int dx = -bound; dx <= bound; dx++) {
                            double r = Math.sqrt(dx * dx + dz * dz);
                            if (r > maxRadius + 0.5) continue; // outside cap footprint

                            // deterministic per-position deformation so mushrooms are stable and natural-looking
                            long hash = ((cx + dx) * 73856093L ^ (cz + dz) * 19349663L ^ ((long) capWidth << 16));
                            Random localRnd = new Random(hash);
                            double deform = (localRnd.nextDouble() - 0.5) * 2.0 * (Math.max(0.4, maxRadius * deformationScale));

                            // apply deformation to the *effective radius* of this cell (so bumps change height)
                            double rEff = Math.max(0.0, r + deform * 0.35); // deform less on radius to avoid huge clipping
                            if (rEff > maxRadius + 1.0) continue;

                            // normalized radius 0..1
                            double rn = Math.max(0.0, Math.min(1.0, rEff / maxRadius));
                            double profileHeight = p.capProfile.height(rn, capHeight);

                            // top Y coordinate for this column (cap top surface)
                            int topY = baseY + (int) Math.round(profileHeight);

                            // rim: we make a thin vertical shell rather than a full solid cap
                            // bottom of the rim shell (cap underside) (do not go below baseY)
                            int bottomY = Math.max(baseY, topY - rimThicknessBlocks + 1);

                            // optional: ensure the very top block is always placed (keeps the dome silhouette)
                            subGenerator.guardAndStore(cx + dx, topY, cz + dz, p.leafMaterial, false, 1);

                            // place vertical rim shell between bottomY..topY
                            for (int y = bottomY; y <= topY; y++) {
                                // to keep interior hollow, only place blocks near the shell edge:
                                // compute a small inner cutoff radius based on rn so inner cells are fewer placed
                                double innerCut = maxRadius * 0.6; // inside this radius we'll thin the shell
                                if (r <= innerCut) {
                                    // For inner region, place fewer blocks (only near the top) to keep a thin cap
                                    if (y >= topY - 1) {
                                        subGenerator.guardAndStore(cx + dx, y, cz + dz, p.leafMaterial, false, 1);
                                    }
                                } else {
                                    // outer region (near rim): fill full vertical rim
                                    subGenerator.guardAndStore(cx + dx, y, cz + dz, p.leafMaterial, false, 1);
                                }
                            }

                            // optionally add an underlap to ensure no visible air under shallow parts:
                            if (bottomY > baseY) {
                                subGenerator.guardAndStore(cx + dx, bottomY - 1, cz + dz, p.leafMaterial, false, 1);
                            }
                        }
                    }
                }
            }
            case ON_BRANCH_TIP -> {
                for (var entry : leafingPoints.entrySet()) {
                    var branch = entry.getKey();
                    if (branch == null || branch.isEmpty()) continue;

                    Vec3 tip = branch.getLast();

                    // === Compute local orientation based on last few points ===
                    int sampleCount = Math.min(5, branch.size());

                    // 1. Average forward direction (smooth tangent)
                    Vec3 avgForward = new Vec3(0, 0, 0);
                    for (int i = branch.size() - sampleCount; i < branch.size() - 1; i++) {
                        if (i < 0) continue;
                        Vec3 a = branch.get(i);
                        Vec3 b = branch.get(i + 1);
                        avgForward = avgForward.add(b.subtract(a));
                    }
                    if (avgForward.lengthSq() < 1e-6) avgForward = new Vec3(0, 0, 1);
                    avgForward = avgForward.normalize();

                    // 2. Average curvature normal (defines "up")
                    Vec3 avgNormal = new Vec3(0, 0, 0);
                    for (int i = branch.size() - sampleCount; i < branch.size() - 2; i++) {
                        if (i < 0) continue;
                        Vec3 a = branch.get(i);
                        Vec3 b = branch.get(i + 1);
                        Vec3 c = branch.get(i + 2);
                        Vec3 ab = b.subtract(a);
                        Vec3 bc = c.subtract(b);
                        Vec3 normal = ab.crossProduct(bc);
                        if (normal.lengthSq() > 1e-6)
                            avgNormal = avgNormal.add(normal.normalize());
                    }

                    if (avgNormal.lengthSq() < 1e-6) {
                        // fallback: choose something perpendicular to forward
                        avgNormal = Math.abs(avgForward.y) < 0.99
                                ? new Vec3(0, 1, 0)
                                : new Vec3(1, 0, 0);
                    }
                    avgNormal = avgNormal.normalize();

                    // 3. Construct orthonormal basis (left, up, forward)
                    Vec3 left = avgNormal.crossProduct(avgForward);
                    if (left.lengthSq() < 1e-6) left = new Vec3(1, 0, 0);
                    left = left.normalize();
                    Vec3 up = avgForward.crossProduct(left).normalize();

                    // === Place decorations using new basis ===
                    Set<Pair<Vec3, PlatformBlockState<T>>> decos = p.onTipDecorations.apply(entry.getValue());
                    if (decos == null || decos.isEmpty()) {
                        // default behavior: single leaf at tip
                        subGenerator.guardAndStore(tip, p.leafMaterial, false);
                        continue;
                    }

                    for (Pair<Vec3, PlatformBlockState<T>> pair : decos) {
                        Vec3 local = pair.key(); // author-local coordinates (x=left, y=up, z=forward)
                        PlatformBlockState<T> state = pair.value();

                        // transform from local -> world
                        Vec3 world = tip
                                .add(left.multiply(local.x))
                                .add(up.multiply(local.y))
                                .add(avgForward.multiply(local.z));

                        int wx = (int) Math.round(world.x);
                        int wy = (int) Math.round(world.y);
                        int wz = (int) Math.round(world.z);

                        // optionally orient block to match forward/up if supported
                        // e.g. state = state.withFacingDirection(avgForward);

                        subGenerator.guardAndStore(new Vec3(wx, wy, wz), state, false);
                    }
                }
            }
            case AROUND_ALL_BRANCHES -> {
                for (var entry : leafingPoints.entrySet()) {
                    var branch = entry.getKey();
                    if (branch == null || branch.isEmpty()) continue;
                    for (Vec3 pos : branch) {
                        if (rnd.nextDouble() <= p.leafPropagationChance) {
                            subGenerator.guardAndStore(pos.add(1, 0, 0), p.leafMaterial, false);
                            subGenerator.guardAndStore(pos.add(1, 1, 0), p.leafMaterial, false);
                            subGenerator.guardAndStore(pos.add(1, 1, 1), p.leafMaterial, false);
                            subGenerator.guardAndStore(pos.add(0, 1, 0), p.leafMaterial, false);
                            subGenerator.guardAndStore(pos.add(0, 1, 1), p.leafMaterial, false);
                            subGenerator.guardAndStore(pos.add(0, 0, 1), p.leafMaterial, false);
                            subGenerator.guardAndStore(pos.add(1, 0, 1), p.leafMaterial, false);
                            subGenerator.guardAndStore(pos.add(-1, 0, 0), p.leafMaterial, false);
                            subGenerator.guardAndStore(pos.add(-1, -1, 0), p.leafMaterial, false);
                            subGenerator.guardAndStore(pos.add(-1, -1, -1), p.leafMaterial, false);
                            subGenerator.guardAndStore(pos.add(0, -1, 0), p.leafMaterial, false);
                            subGenerator.guardAndStore(pos.add(0, -1, -1), p.leafMaterial, false);
                            subGenerator.guardAndStore(pos.add(0, 0, -1), p.leafMaterial, false);
                            subGenerator.guardAndStore(pos.add(-1, 0, -1), p.leafMaterial, false);
                        }
                    }
                }
            }
            case HANGING_FUZZY -> {
                int matCount = p.vineSequenceMaterial.size();
                if (matCount == 0) return;

                for (var entry : leafingPoints.entrySet()) {
                    var branch = entry.getKey();
                    if (branch == null || branch.isEmpty()) continue;
                    for (Vec3 pos : branch) {
                        subGenerator.guardAndStore(pos.add(1, 0, 0), p.leafMaterial, false);
                        subGenerator.guardAndStore(pos.add(1, 1, 0), p.leafMaterial, false);
                        subGenerator.guardAndStore(pos.add(1, 1, 1), p.leafMaterial, false);
                        subGenerator.guardAndStore(pos.add(0, 1, 0), p.leafMaterial, false);
                        subGenerator.guardAndStore(pos.add(0, 1, 1), p.leafMaterial, false);
                        subGenerator.guardAndStore(pos.add(0, 0, 1), p.leafMaterial, false);
                        subGenerator.guardAndStore(pos.add(1, 0, 1), p.leafMaterial, false);
                    }

                    // For each point along this branch consider spawning a hanging vine
                    int index = 0;
                    for (int i = 0; i < branch.size(); i++) {
                        if (rnd.nextDouble() > p.leafPropagationChance) continue;
                        if (index > p.maxVinePerBranch) break;
                        if (i < index + (p.spacing / 2)) continue;
                        int vineHeight = Math.max(1, (int) Math.round(trunkHeight * (rnd.nextDouble() * p.vineHeight)));

                        Vec3 base = branch.get(i);
                        index++;

                        var points = CorePointsFactory.generate(
                                CorePointsFactory.Params.builder()
                                        .noiseStrength(0.7)
                                        .divergenceStrength(0.7)
                                        .noiseSmooth(0.1)
                                        .height(vineHeight)
                                        .segments(Math.min(50, Math.max(8, vineHeight * 4))) // choose sensible segment count
                                        .build()
                        );
                        points = BezierCurve.generatePoints(points, 200);

                        for (var vec : points) {
                            // progress 0..1 down the vine (vec.y goes 0..vineHeight in generated profile)
                            double progress = vec.y / (double) vineHeight;
                            progress = Math.max(0.0, Math.min(1.0, progress));
                            int idx = (int) Math.floor(progress * matCount);
                            if (idx >= matCount) idx = matCount - 1;
                            if (idx < 0) idx = 0;
                            var material = p.vineSequenceMaterial.get(idx);

                            // place at base - vec so vine descends from base
                            subGenerator.guardAndStore(base.subtract(vec), material, false);
                        }
                    }
                }
            }
            case CONIFEROUS -> {
                // --- DEBUG / Tunables ---
                final double perPointChance = p.leafPropagationChance;
                final int armsMin = 2;
                final int armsMax = Math.max(armsMin, p.coniferousBranchRange.value()); // safety clamp
                final double extrusionLengthFactor = 0.05; // use 1.0 to derive more obvious lengths (was 10)
                final double forwardBiasPerStep = 0.15;
                final double angleJitterRadians = Math.toRadians(4.0);
                final double radialJitter = 0.25;
                final int distance = 3;

                // Set true while debugging to force block placement (may overwrite). Set false for normal behavior.
                final boolean FORCE_PLACE = true;

                for (var entry : leafingPoints.entrySet()) {
                    var branchUnprocessed = entry.getKey();
                    if (branchUnprocessed == null || branchUnprocessed.isEmpty()) continue;
                    final int extrusionMin = 2;
                    final int extrusionMax = (int) Math.round(trunkHeight * 0.09) / Math.max(1, entry.getValue() - 1);

                    // reset per-branch spacing counter (important!)
                    int curr = -distance; // allow the first spawn if pi >= 0
                    // optionally densify path for smoother tangent decisions
                    branchUnprocessed = branchUnprocessed.stream()
                            .sorted(Comparator.comparingDouble(v -> v.x))
                            .sorted(Comparator.comparingDouble(v -> v.z))
                            .sorted(Comparator.comparingDouble(v -> v.y)).toList();
                    double length = branchUnprocessed.getFirst().distance(branchUnprocessed.getLast());
                    if (length == 0.0) continue;
                    if (length > 0.0) length = 100 * Math.min(1, length / 10);
                    else if (length < 0.0) length = 100 * length;
                    else length = 50;
                    List<Vec3> branch = BezierCurve.generatePoints(branchUnprocessed, (int) length);
                    int branchSize = branch.size();
                    int start = (int) Math.round(p.branchingPointRange.key() * branchSize);

                    for (int pi = start; pi < branchSize; pi++) {
                        if (rnd.nextDouble() > perPointChance) continue;
                        if (pi < curr + distance) continue;
                        curr = pi;

                        Vec3 point = branch.get(pi);

                        // tangent / forward
                        Vec3 forward;
                        if (pi + 1 < branchSize) {
                            forward = branch.get(pi + 1).subtract(point);
                        } else if (pi - 1 >= 0) {
                            forward = point.subtract(branch.get(pi - 1));
                        } else {
                            forward = new Vec3(0, 1, 0);
                        }
                        if (forward.lengthSq() < 1e-8) forward = new Vec3(0, 1, 0);
                        forward = forward.normalize();

                        // orthonormal basis
                        Vec3 worldUp = new Vec3(0, 1, 0);
                        if (Math.abs(forward.dot(worldUp)) > 0.999) worldUp = new Vec3(1, 0, 0);
                        Vec3 u = worldUp.crossProduct(forward).normalize();
                        Vec3 v = forward.crossProduct(u).normalize();

                        // decide arms
                        int armsRange = armsMax - armsMin + 1;
                        int armCount = armsMin + rnd.nextInt(armsRange);

                        // extrusion length derived from remaining points (scaled)
                        int remainingPoints = Math.max(1, branchSize - pi);
                        int baseExtrusion = (int) Math.round(remainingPoints * extrusionLengthFactor);
                        int extrusionLength = Math.max(extrusionMin, Math.min(extrusionMax, baseExtrusion));

                        // LOGGER.print("[CONIFEROUS] spawn at pi=" + pi + " remaining=" + remainingPoints +
                        //         " baseExtr=" + baseExtrusion + " len=" + extrusionLength + " arms=" + armCount);

                        for (int a = 0; a < armCount; a++) {
                            double baseAngle = (2.0 * Math.PI * a) / Math.max(1, armCount);
                            double angle = baseAngle + (rnd.nextDouble() * 2.0 - 1.0) * angleJitterRadians;
                            Vec3 radialDir = u.multiply(Math.cos(angle)).add(v.multiply(Math.sin(angle))).normalize();

                            double startRadialOffset = rnd.nextDouble() * 0.6;

                            for (int step = 1; step <= extrusionLength; step++) {
                                double radialMag = startRadialOffset + step + (rnd.nextDouble() * radialJitter - radialJitter * 0.5);
                                double forwardMag = step * forwardBiasPerStep;

                                Vec3 worldPos = point
                                        .add(radialDir.multiply(radialMag))
                                        .add(forward.multiply(forwardMag));

                                // round to blocks
                                int wx = (int) Math.round(worldPos.x);
                                int wy = (int) Math.round(worldPos.y);
                                int wz = (int) Math.round(worldPos.z);

                                // Debug log each placement attempt (will be noisy — helpful for visibility)

                                // Use Vec3 overload to keep APIs consistent; force placement if debugging
                                if (FORCE_PLACE) {
                                    // true => allow overwrite (debugging)
                                    subGenerator.guardAndStore(new Vec3(wx, wy, wz), p.leafMaterial, true, 1);
                                } else {
                                    subGenerator.guardAndStore(new Vec3(wx, wy, wz), p.leafMaterial, false);
                                }
                            }
                        }
                    }
                }
            }
            case HANGING_MUSHROOM -> {
                // --- find cap centers ---
                Map<Vec3, Integer> capCenters = new HashMap<>();
                for (var entry : leafingPoints.entrySet()) {
                    var branch = entry.getKey();
                    if (branch == null || branch.isEmpty()) continue;
                    branch.stream().max(Comparator.comparingDouble(v -> v.y)).ifPresent(it -> capCenters.put(it, entry.getValue()));
                }

                for (var entry : capCenters.entrySet()) {
                    var center = entry.getKey();
                    int capWidth = rnd.nextInt(p.mushroomCapWidth.key(), p.mushroomCapWidth.value()) / entry.getValue();
                    int capHeight = rnd.nextInt(p.mushroomCapHeight.key(), p.mushroomCapHeight.value()) / entry.getValue();
                    if (capWidth < 1) capWidth = 3;
                    if (capHeight < 1) capHeight = 3;

                    final double maxRadius = capWidth / 2.0;
                    final double rimThicknessFrac = 0.32;
                    final int rimThicknessBlocks = Math.max(1, (int) Math.round(capWidth * rimThicknessFrac));
                    final double deformationScale = 0.18;
                    // =================================================

                    final int cx = (int) Math.round(center.x);
                    final int cz = (int) Math.round(center.z);
                    final int baseY = (int) Math.round(center.y);

                    // collect visible placed cap positions so we can hang from them
                    List<Vec3> capPlaced = new ArrayList<>();

                    int bound = (int) Math.ceil(maxRadius) + 1;
                    for (int dz = -bound; dz <= bound; dz++) {
                        for (int dx = -bound; dx <= bound; dx++) {
                            double r = Math.sqrt(dx * dx + dz * dz);
                            if (r > maxRadius + 0.5) continue;

                            long hash = ((cx + dx) * 73856093L ^ (cz + dz) * 19349663L ^ ((long) capWidth << 16));
                            Random localRnd = new Random(hash);
                            double deform = (localRnd.nextDouble() - 0.5) * 2.0 * (Math.max(0.4, maxRadius * deformationScale));
                            double rEff = Math.max(0.0, r + deform * 0.35);
                            if (rEff > maxRadius + 1.0) continue;

                            double rn = Math.max(0.0, Math.min(1.0, rEff / maxRadius));
                            double profileHeight = p.capProfile.height(rn, capHeight);

                            int topY = baseY + (int) Math.round(profileHeight);
                            int bottomY = Math.max(baseY, topY - rimThicknessBlocks + 1);

                            // place top block (silhouette) and record it as a potential hanging site
                            subGenerator.guardAndStore(cx + dx, topY, cz + dz, p.leafMaterial, false, 1);
                            capPlaced.add(new Vec3(cx + dx, topY, cz + dz));

                            // place vertical rim shell between bottomY..topY (thin interior)
                            for (int y = bottomY; y <= topY; y++) {
                                double innerCut = maxRadius * 0.6;
                                if (r <= innerCut) {
                                    if (y >= topY - 1) {
                                        subGenerator.guardAndStore(cx + dx, y, cz + dz, p.leafMaterial, false, 1);
                                    }
                                } else {
                                    subGenerator.guardAndStore(cx + dx, y, cz + dz, p.leafMaterial, false, 1);
                                }
                            }

                            // underlap guard to avoid visible air where underside is high
                            if (bottomY > baseY) {
                                subGenerator.guardAndStore(cx + dx, bottomY - 1, cz + dz, p.leafMaterial, false, 1);
                            }
                        }
                    } // done placing cap

                    // --- spawn danglings from selected cap positions (bias towards rim if desired) ---
                    if (capPlaced.isEmpty() || p.maxMushDang <= 0) continue;

                    // Optional: bias candidates toward rim by filtering distances near maxRadius
                    List<Vec3> rimCandidates = new ArrayList<>();
                    List<Vec3> innerCandidates = new ArrayList<>();
                    for (Vec3 pos : capPlaced) {
                        double dx = pos.x - cx;
                        double dz = pos.z - cz;
                        double dist = Math.sqrt(dx * dx + dz * dz);
                        if (dist >= maxRadius * 0.65) rimCandidates.add(pos);
                        else innerCandidates.add(pos);
                    }

                    // prefer rim candidates but fall back to inner if not enough
                    List<Vec3> pickPool = new ArrayList<>(rimCandidates);
                    pickPool.addAll(innerCandidates);

                    Collections.shuffle(pickPool, rnd);
                    int maxDang = Math.min(p.maxMushDang, pickPool.size());

                    int matCount = p.vineSequenceMaterial.size();
                    if (matCount == 0) continue; // nothing to hang

                    for (int i = 0; i < maxDang; i++) {
                        Vec3 base = pickPool.get(i);

                        // optional per-dangling spawn chance
                        if (rnd.nextDouble() > p.leafPropagationChance / 2) continue;

                        int vineHeight = Math.max(1, (int) Math.round(trunkHeight * (rnd.nextDouble() * p.vineHeight)) / entry.getValue());
                        var points = CorePointsFactory.generate(
                                CorePointsFactory.Params.builder()
                                        .noiseStrength(0.7)
                                        .divergenceStrength(0.7)
                                        .noiseSmooth(0.1)
                                        .height(vineHeight)
                                        .segments(Math.min(50, Math.max(8, vineHeight * 4)))
                                        .build()
                        );
                        points = BezierCurve.generatePoints(points, 200);

                        for (var vec : points) {
                            double progress = vec.y / (double) vineHeight;
                            progress = Math.max(0.0, Math.min(1.0, progress));
                            int idx = (int) Math.floor(progress * matCount);
                            if (idx >= matCount) idx = matCount - 1;
                            if (idx < 0) idx = 0;
                            var material = p.vineSequenceMaterial.get(idx);

                            // hang downward from the cap block
                            subGenerator.guardAndStore(base.subtract(vec), material, false);
                        }
                    }
                } // end each cap center
            }

            default -> {
                // fallback: treat like AROUND_ALL_BRANCHES
                for (var entry : leafingPoints.entrySet()) {
                    var branch = entry.getKey();
                    if (branch == null || branch.isEmpty()) continue;
                    for (Vec3 pos : branch) {
                        if (rnd.nextDouble() <= p.leafPropagationChance) {
                            subGenerator.guardAndStore(pos.add(0, 1, 0), p.leafMaterial, false);
                        }
                    }
                }
            }
        }
    }


    public enum TrunkType {
        BONSAI(0.4, BranchingType.WIND_SWEPT, 15),
        CONIFEROUS(0.6, BranchingType.CONIFEROUS, 3),
        MULTI_TRUNKED(0.2, BranchingType.DICHOTOMOUS, 32),
        SLANTED(0.8, BranchingType.PALMLIKE, 7),
        TAPERED_SPINDLE(0.3, BranchingType.TAPERED_SPINDLE, 20);

        private final double branchingPoint;
        private final BranchingType branchingType;
        private final int deviance;

        TrunkType(double defaultPoint, BranchingType branchingType, int deviance) {
            this.branchingPoint = defaultPoint;
            this.branchingType = branchingType;
            this.deviance = deviance;
        }
    }

    public enum BranchingType {
        WIND_SWEPT,
        BULBED,
        SQUIGGLY,
        CONIFEROUS,
        TAPERED_SPINDLE,
        PALMLIKE,
        DICHOTOMOUS,
        BROOMED,
        OPPOSITE,
        TRIANGULAR,
        NO_BRANCHES
    }

    public enum LeafPopulationType {
        PERPENDICULAR_TO_BRANCH,
        /**/ ON_BRANCH_TIP,
        AROUND_LAST_BRANCH,
        /**/ HANGING,
        /**/ THICK_HANGING,
        /**/ AROUND_ALL_BRANCHES,
        CONIFEROUS,
        ROSETTE,
        /**/ HANGING_MUSHROOM,
        /**/ HANGING_FUZZY,
        /**/ MUSHROOMCAP,
        /**/ NO_LEAVES
    }

    public static class NoAIPTGBuilder<T> extends BaseBuilder<T, NoAIProceduralTreeGenerator<T>> {
        public boolean taperedFuzz = false;
        protected TrunkType trunkType = TrunkType.TAPERED_SPINDLE;
        protected BranchingType branchType = trunkType.branchingType;
        protected LeafPopulationType leafType = LeafPopulationType.ON_BRANCH_TIP;
        protected MushroomCapProfile capProfile = MushroomCapProfile.HEMISPHERE;
        protected Pair<Double, Double>
                branchingPointRange = new Pair<>(trunkType.branchingPoint, 0.8);
        protected Pair<Integer, Integer>
                mushroomCapWidth = new Pair<>(30, 40),
                mushroomCapHeight = new Pair<>(10, 20),
                trunkHeightRange = new Pair<>(30, 40),
                trunkWidthRange = new Pair<>(15, 18),
                coniferousBranchRange = new Pair<>(4, 7);
        protected int branchDepth = 4, branchMaxDevianceAngle = trunkType.deviance,
                branchMaxHorizontalDevianceAngle = 10, maxBranchAmount = 7, maxVinePerBranch = 4, spacing = 2,
                maxMushDang = 30;
        protected double quality = 1.0, vineHeight = 0.3,
                twistiness = 0.4, randomness = 0.2, leafPropagationChance = 1.0, branchPropagationChance = 0.5,
                branchSizeDecay = 0.4, treeTrunkMaxShrink = 0.3, branchVerticalDensity = 1.0;

        protected Direction windFlowDirection = Direction.EAST;
        protected PlatformBlockState<T> woodMaterial, leafMaterial;
        protected List<PlatformBlockState<T>> vineSequenceMaterial = new ArrayList<>();
        protected Function<Double, Double> vineThickness =
                CurveFunctions.radius(2.0, 1.0, 0.0, 1.0, TimeCurve.EXPONENTIAL_OUT, true);

        protected double bonsaiTreeHelixSpan = 30;
        protected int multiTrunkAngleMax = 30;
        protected int multiTrunkNumber = 3;
        protected double multiTrunkStartPoint = 0.2;
        protected Function<Integer, Set<Pair<Vec3, PlatformBlockState<T>>>> onTipDecorations = (x) -> new HashSet<>();

        protected Pair<Integer, Integer> slantAngleRange = new Pair<>(10, 20);

        public NoAIPTGBuilder<T> trunkType(TrunkType type) {
            this.trunkType = type;
            this.branchType = type.branchingType; // sync default
            this.branchingPointRange = new Pair<>(type.branchingPoint, 0.8);
            this.branchMaxDevianceAngle = type.deviance;
            return this;
        }

        public NoAIPTGBuilder<T> branchType(BranchingType type) {
            this.branchType = type;
            return this;
        }

        /**
         * These should be generated with an up of Vec(0, 1, 0) and left of (1, 0, 1). The class will automatically make it face the right direction
         *
         * @return The Builder
         */
        public NoAIPTGBuilder<T> tipDecoration(Function<Integer, Set<Pair<Vec3, PlatformBlockState<T>>>> onTipDecorations) {
            this.onTipDecorations = onTipDecorations;
            return this;
        }

        public NoAIPTGBuilder<T> maxBranchAmount(int maxBranchAmount) {
            this.maxBranchAmount = maxBranchAmount;
            return this;
        }

        public NoAIPTGBuilder<T> maxMushDang(int maxMushDang) {
            this.maxMushDang = maxMushDang;
            return this;
        }

        public NoAIPTGBuilder<T> taperedFuzz(boolean taperedFuzz) {
            this.taperedFuzz = taperedFuzz;
            return this;
        }

        public NoAIPTGBuilder<T> multiTrunkAngleMax(int multiTrunkAngleMax) {
            this.multiTrunkAngleMax = multiTrunkAngleMax;
            return this;
        }

        public NoAIPTGBuilder<T> spacing(int spacing) {
            this.spacing = spacing;
            return this;
        }

        public NoAIPTGBuilder<T> multiTrunkNumber(int multiTrunkNumber) {
            this.multiTrunkNumber = multiTrunkNumber;
            return this;
        }

        public NoAIPTGBuilder<T> maxVinePerBranch(int maxVinePerBranch) {
            this.maxVinePerBranch = maxVinePerBranch;
            return this;
        }

        public NoAIPTGBuilder<T> coniferousBranchRange(int coniferousMinBranches, int coniferousMaxBranches) {
            this.coniferousBranchRange = new Pair<>(coniferousMinBranches, coniferousMaxBranches);
            return this;
        }

        public NoAIPTGBuilder<T> bonsaiTreeHelixSpan(double bonsaiTreeHelixSpan) {
            this.bonsaiTreeHelixSpan = bonsaiTreeHelixSpan;
            return this;
        }

        public NoAIPTGBuilder<T> vineHeight(double vineHeight) {
            this.vineHeight = vineHeight;
            return this;
        }

        public NoAIPTGBuilder<T> treeTrunkMaxShrink(double treeTrunkMaxShrink) {
            this.treeTrunkMaxShrink = treeTrunkMaxShrink;
            return this;
        }

        public NoAIPTGBuilder<T> branchVerticalDensity(double branchVerticalDensity) {
            this.branchVerticalDensity = branchVerticalDensity;
            return this;
        }

        public NoAIPTGBuilder<T> multiTrunkStartPoint(double multiTrunkStartPoint) {
            this.multiTrunkStartPoint = multiTrunkStartPoint;
            return this;
        }

        public NoAIPTGBuilder<T> quality(double quality) {
            this.quality = quality;
            return this;
        }

        public NoAIPTGBuilder<T> leafType(LeafPopulationType type) {
            this.leafType = type;
            return this;
        }

        public NoAIPTGBuilder<T> capProfile(MushroomCapProfile capProfile) {
            this.capProfile = capProfile;
            return this;
        }

        public NoAIPTGBuilder<T> slantAngleRange(int minAngle, int maxAngle) {
            this.slantAngleRange = new Pair<>(minAngle, maxAngle);
            return this;
        }

        public NoAIPTGBuilder<T> trunkHeight(int minHeight, int maxHeight) {
            this.trunkHeightRange = new Pair<>(minHeight, maxHeight);
            return this;
        }

        public NoAIPTGBuilder<T> vineThickness(Function<Double, Double> it) {
            this.vineThickness = it;
            return this;
        }

        public NoAIPTGBuilder<T> mushroomCapWidth(int minHeight, int maxHeight) {
            this.mushroomCapWidth = new Pair<>(minHeight, maxHeight);
            return this;
        }

        public NoAIPTGBuilder<T> mushroomCapHeight(int minHeight, int maxHeight) {
            this.mushroomCapHeight = new Pair<>(minHeight, maxHeight);
            return this;
        }

        public NoAIPTGBuilder<T> trunkWidth(int minWidth, int maxWidth) {
            this.trunkWidthRange = new Pair<>(minWidth, maxWidth);
            return this;
        }

        public NoAIPTGBuilder<T> branchingPointRange(double value, double valueMax) {
            this.branchingPointRange = new Pair<>(value, valueMax);
            return this;
        }

        public NoAIPTGBuilder<T> twistiness(double value) {
            this.twistiness = value;
            return this;
        }

        public NoAIPTGBuilder<T> randomness(double value) {
            this.randomness = value;
            return this;
        }

        public NoAIPTGBuilder<T> leafPropagationChance(double value) {
            this.leafPropagationChance = value;
            return this;
        }

        public NoAIPTGBuilder<T> branchPropagationChance(double value) {
            this.branchPropagationChance = value;
            return this;
        }

        public NoAIPTGBuilder<T> branchDepth(int value) {
            this.branchDepth = value;
            return this;
        }

        public NoAIPTGBuilder<T> branchSizeDecay(double value) {
            this.branchSizeDecay = value;
            return this;
        }

        public NoAIPTGBuilder<T> branchMaxDevianceAngle(int value) {
            this.branchMaxDevianceAngle = value;
            return this;
        }

        public NoAIPTGBuilder<T> branchMaxHorizontalDevianceAngle(int branchMaxHorizontalDevianceAngle) {
            this.branchMaxHorizontalDevianceAngle = branchMaxHorizontalDevianceAngle;
            return this;
        }

        public NoAIPTGBuilder<T> windFlowDirection(Direction dir) {
            this.windFlowDirection = dir;
            return this;
        }

        public NoAIPTGBuilder<T> woodMaterial(PlatformBlockState<T> material) {
            this.woodMaterial = material;
            return this;
        }

        public NoAIPTGBuilder<T> leafMaterial(PlatformBlockState<T> material) {
            this.leafMaterial = material;
            return this;
        }

        public NoAIPTGBuilder<T> vineSequenceMaterial(List<PlatformBlockState<T>> materials) {
            materials.forEach(mat -> this.vineSequenceMaterial.add(mat));
            return this;
        }

        public NoAIPTGBuilder<T> addVineSequenceMaterial(PlatformBlockState<T> material) {
            this.vineSequenceMaterial.add(material);
            return this;
        }

        @Override
        public void validate() {
            if (quality < 0.1)
                throw new IllegalArgumentException("Quality Factor CANNOT be less than 0.1... THAT'S EVEN TOO LOW :SOB:");
            if (leafType != LeafPopulationType.NO_LEAVES &&
                    leafMaterial == null)
                throw new IllegalArgumentException("Leaf Material cannot be null when using leaves");
            if (woodMaterial == null) throw new IllegalArgumentException("Wood Material cannot be null");
            if (leafType == LeafPopulationType.HANGING && vineSequenceMaterial.isEmpty())
                throw new IllegalArgumentException("When using hanging leaf type, vineMaterialSequence cannot be null or empty");
            if (branchType == BranchingType.WIND_SWEPT && windFlowDirection == null)
                throw new IllegalArgumentException("When using WIND_SWEPT branch type, windFlowDirection cannot be null");
            if (leafPropagationChance <= 0.0)
                LOGGER.print("When setting leafPropagationChance to <= 0.0 it would be better to use LeafPopulationType#NO_LEAVES to avoid unnecessary calculations", ContextLogger.LogType.WARNING);
            if (branchPropagationChance <= 0.0)
                LOGGER.print("When setting branchPropagationChance to <= 0.0 it would be better to use BranchingType#NO_BRANCHES to avoid unnecessary calculations", ContextLogger.LogType.WARNING);
        }

        @Override
        protected NoAIProceduralTreeGenerator<T> create() {
            return new NoAIProceduralTreeGenerator<>(this);
        }
    }
}
