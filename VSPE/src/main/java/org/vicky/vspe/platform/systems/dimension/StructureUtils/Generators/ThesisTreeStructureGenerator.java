package org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators;

import org.jetbrains.annotations.NotNull;
import org.vicky.platform.utils.Vec3;
import org.vicky.platform.world.PlatformBlockState;
import org.vicky.platform.world.PlatformWorld;
import org.vicky.utilities.Pair;
import org.vicky.vspe.BlockVec3i;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.CurveFunctions;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators.thesis.ThesisBasedTreeGenerator;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.ProceduralStructureGenerator;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.SpiralUtil;
import org.vicky.vspe.platform.systems.dimension.TimeCurve;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.BlockPlacement;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.RandomSource;
import org.vicky.vspe.platform.utilities.Quad;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;

import static org.vicky.vspe.branch.BranchKt.generateLeafBlobWithChildren;

/**
 * A ProceduralStructureGenerator subclass that uses ThesisBasedTreeGenerator to
 * create a tree structure and convert nodes into block placements.
 * <p>
 * The {@link Builder} only accepts primitives, enums and functional interfaces.
 */
public class ThesisTreeStructureGenerator<T> extends ProceduralStructureGenerator<T> {

    private final int treeAge;
    private final Pair<Integer,
            Integer> preTrunkHeight;
    private final Pair<Double,
            Double> preTrunkWidth;
    private final int trunkSegments;
    private final int maxLeavesPerBranch;
    private final double spread; // generic quality/LOD parameter
    private final PlatformBlockState<T> trunkBlockMaterial;
    private final PlatformBlockState<T> leafMaterial;
    private final boolean placeLeaves;
    private final ThesisBasedTreeGenerator.GrowthData growthData;
    private final DoubleUnaryOperator thicknessFunction;
    private final long localSeed;
    private final TimeCurve forTrunk = TimeCurve.INVERTED_QUADRATIC;
    private final LeafDetails leafInformation;
    public ThesisBasedTreeGenerator tb;

    /**
     * Collected terminal points of leafing branches used to build a final
     * realistic canopy dome when {@link LeafDetails#useRealisticType} is true.
     */
    private final List<Pair<Vec3, Integer>> realisticLeafTerminals =
            Collections.synchronizedList(new ArrayList<>());

    /**
     * Ensures the final canopy dome job is only enqueued once.
     */
    private final AtomicBoolean realisticDomeQueued = new AtomicBoolean(false);

    private ThesisTreeStructureGenerator(Builder<T> builder) {
        this.maxLeavesPerBranch = builder.maxLeavesPerBranch;
        this.treeAge = builder.treeAge;
        this.preTrunkHeight = builder.trunkHeight;
        this.preTrunkWidth = builder.trunkWidth;
        this.trunkSegments = builder.trunkSegments;
        this.spread = builder.spread;
        this.trunkBlockMaterial = builder.trunkBlockMaterial;
        this.leafMaterial = builder.leafMaterial;
        this.placeLeaves = builder.placeLeaves;
        this.growthData = builder.growthData;
        this.leafInformation = builder.leafInformation;
        this.thicknessFunction = builder.thicknessFunction;
        this.localSeed = builder.seed;
    }

    @Override
    public BlockVec3i getApproximateSize() {
        // A conservative bounding box estimation (centered on origin).
        int w = (int) Math.ceil(preTrunkWidth.value() * 2) + 4;
        return new BlockVec3i(w, preTrunkHeight.value() + 4, w);
    }

    /**
     * Core generation entrypoint called by the superclass wrapper.
     * Convert a simulated thesis tree into a set of block placements.
     */
    @Override
    protected void performGeneration(RandomSource rnd, Vec3 origin, List<BlockPlacement<T>> outPlacements, Map<Long, BiConsumer<PlatformWorld<T, ?>, Vec3>> outActions) {
        // Build GrowthData for the thesis generator from our configured one
        // generic quality/LOD parameter
        int trunkHeight = rnd.nextInt(preTrunkHeight.key(), preTrunkHeight.value());
        double trunkWidth = rnd.nextDouble(preTrunkWidth.key(), preTrunkWidth.value());
        ThesisBasedTreeGenerator.GrowthData gd =
                this.growthData != null ? this.growthData : new ThesisBasedTreeGenerator.GrowthData(trunkHeight);
        gd.forTrunk = forTrunk;

        // Create the thesis generator (world-agnostic)
        tb = new ThesisBasedTreeGenerator(gd, localSeed ^ rnd.nextLong(), LOGGER, isProduction);

        // init root at origin pointing upwards
        tb.initRoot(origin, new Vec3(0f, 1f, 0f), (float) trunkWidth);

        tb.simulateToAge(treeAge);
        tb.shutdown();

        // Convert the tree nodes into block placements
        // Map primitive trunkBlockId -> PlatformBlockState<T>

        // Compute maximum order so we can normalize thickness functions by depth
        int maxOrder = 1;
        for (ThesisBasedTreeGenerator.TreeNode node : tb.getCachedBranches().values()) {
            if (node != null) maxOrder = Math.max(maxOrder, node.order);
        }

        // Clear any old terminal data before stamping a new tree
        realisticLeafTerminals.clear();
        realisticDomeQueued.set(false);

        submitSubtask((sub)-> stampBranchRecursively(sub, tb.getRoot(), trunkBlockMaterial));

        // If using the realistic system, enqueue a single finalising job that
        // builds a canopy/dome from all collected terminal points.
        if (placeLeaves && leafInformation.useRealisticType) {
            if (realisticDomeQueued.compareAndSet(false, true)) {
                submitFinalisingSubtask(this::buildRealisticLeafCanopy);
            }
        }

        if (flush != null) flush.run();

        // The generate(...) wrapper will handle merging subtasks and returning the final GenerationResult
    }

    // Recursively traverse starting at node and stamp tubes for main and lateral children
    private void stampBranchRecursively(SubGenerator subGen, ThesisBasedTreeGenerator.TreeNode node, PlatformBlockState<T> state) {
        if (node == null || node.getControlPoints().isEmpty() || node.nodeStatus == ThesisBasedTreeGenerator.NodeStatus.DEAD) return;

        for (ThesisBasedTreeGenerator.TreeNode child : node.getChildren()) {
            submitSubtask((sub) -> stampBranchRecursively(sub, child, state));
        }

        // pick functions depending on order or node type
        Function<Double, Double> radiusFunction = getRadiusFunction(node);

        Function<Double, Double> pitchFunction =
                CurveFunctions.pitch(0.03, 0.1, 0.2, 1.0, TimeCurve.EASE_IN_OUT_CUBIC);

        // Pass your control points straight to the spiral generator
        Set<Vec3> generated = SpiralUtil.generateVineWithSpiralNoBezier(
                new LinkedList<>(node.getAllPoints()),
                7,
                0.8f,
                radiusFunction,
                pitchFunction
        );

        // Stamp them into the world
        for (Vec3 v : generated) {
            subGen.guardAndStore(v, state, false);
        }


        final double nodeLength = node.length();

        if (placeLeaves && leafInformation.useRealisticType) {
            boolean isLeafingOrder = node.order > leafInformation.startIndex;
            if (isLeafingOrder) {
                realisticLeafTerminals.add(new Pair<>(node.tip(), node.order));
            }
        }
        else if (node.order > leafInformation.startIndex && placeLeaves) {
            final double startDist = nodeLength * leafInformation.leafSpawningPoint;
            final double endDist = nodeLength * leafInformation.leafSpawningPointEnd;
            final double leafSpacing = nodeLength * leafInformation.leafSpacing;

            Queue<Quad<Double, Vec3, Double, Vec3>> leafingPoints = new LinkedList<>();
            for (double dist = startDist; dist <= endDist; dist += leafSpacing) {
                double t = dist / nodeLength; // normalized 0–1 position along branch
                Vec3 point = SpiralUtil.findPointOnPathFromLength(dist, node.getAllPoints());

                double totalThickness = radiusFunction.apply(t);
                var pointr = new Quad<>(
                        (double) leafInformation.heightReductionCurve.apply(t),
                        point, totalThickness * 2, SpiralUtil.tangentAtLength(dist, node.getControlPoints()));

                leafingPoints.add(pointr);
                LOGGER.debug("Leafing points for node {}: {}", node.id, pointr);
            }

            double goldenAngle = Math.toRadians(137.5);
            double baseAngle = 0.0;
            int index = 0;

            while (!leafingPoints.isEmpty()) {
                if (index > maxLeavesPerBranch) {
                    break;
                }
                var polled = leafingPoints.poll();
                Vec3 point = polled.second;
                double thickness = polled.third;
                double progress = leafInformation.heightReduction ? polled.first : 0.0;
                Vec3 tangent = polled.fourth;
                switch (leafInformation.leafType) {
                    case ALTERNATE -> {
                        // Rotate by golden angle each step
                        double angle = baseAngle + index * goldenAngle;
                        submitFinalisingSubtask(sub -> placeLeaf(sub, progress, node.order, point, tangent, nodeLength, angle, thickness));
                    }
                    case OPPOSITE -> {
                        // Two opposite leaves (180° apart)
                        double angle1 = baseAngle + index * Math.toRadians(90);
                        double angle2 = angle1 + Math.PI;
                        submitFinalisingSubtask(sub -> placeLeaf(sub, progress, node.order, point, tangent, nodeLength, angle1, thickness));
                        submitFinalisingSubtask(sub -> placeLeaf(sub, progress, node.order, point, tangent, nodeLength, angle2, thickness));
                    }
                    case OPPOSITE_DISTICHOUS -> {
                        // Opposite leaves, all aligned (same angle pair)
                        double angle2 = baseAngle + Math.PI;
                        submitFinalisingSubtask(sub -> placeLeaf(sub, progress, node.order, point, tangent, nodeLength, baseAngle, thickness));
                        submitFinalisingSubtask(sub -> placeLeaf(sub, progress, node.order, point, tangent, nodeLength, angle2, thickness));
                    }
                    case WHORLED -> {
                        // 3–5 leaves around the node evenly spaced
                        int whorlCount = 3 + (int) (Math.random() * 3); // 3–5
                        for (int i = 0; i < whorlCount; i++) {
                            double angle = baseAngle + (i * (2 * Math.PI / whorlCount));
                            submitFinalisingSubtask(sub -> placeLeaf(sub, progress, node.order, point, tangent, nodeLength, angle, thickness));
                        }
                    }
                    case ROSETTE -> {
                        // Flat circular base near ground
                        int count = 8;
                        for (int i = 0; i < count; i++) {
                            double angle = baseAngle + (i * (2 * Math.PI / count));
                            submitFinalisingSubtask(sub -> placeLeaf(sub, progress, node.order, point, tangent, nodeLength, angle, thickness));
                        }
                    }
                    case VERTICILLATE -> {
                        // Like whorled but tighter spacing (closer angles)
                        int ringCount = 5;
                        for (int i = 0; i < ringCount; i++) {
                            double angle = baseAngle + i * (2 * Math.PI / (ringCount * 1.5));
                            submitFinalisingSubtask(sub -> placeLeaf(sub, progress, node.order, point, tangent, nodeLength, angle, thickness));
                        }
                    }
                    case FACISULATE -> {
                        // Needles grouped in bundles (small cluster offset)
                        int bundleSize = 3 + (int) (Math.random() * 3);
                        for (int i = 0; i < bundleSize; i++) {
                            double angle = baseAngle + i * (Math.PI / bundleSize);
                            submitFinalisingSubtask(sub -> placeLeaf(sub, progress, node.order, point, tangent, nodeLength, angle, thickness));
                        }
                    }
                    case TERMINAL -> {
                        // Only place one leaf at the very end
                        if (leafingPoints.isEmpty()) {
                            submitFinalisingSubtask(sub -> placeLeaf(sub, progress, node.order, point, tangent, nodeLength, baseAngle, thickness));
                        }
                    }
                    case PALM -> {
                        if (leafingPoints.isEmpty()) {
                            int layers   = leafInformation.layerCount;
                            int perLayer = leafInformation.leafCountPerLayer;

                            Vec3 axis = tangent.normalize();
                            Vec3 up   = new Vec3(0, 1, 0);

                            // Stable horizontal basis around trunk axis
                            Vec3 right = axis.crossProduct(up);
                            if (right.lengthSq() < 1e-6) {
                                right = axis.crossProduct(new Vec3(1, 0, 0));
                            }
                            right = right.normalize();
                            Vec3 forwardAround = axis.crossProduct(right).normalize();

                            for (int l = 0; l < layers; l++) {
                                double layerNorm = (layers <= 1) ? 0.0 : (double) l / (layers - 1); // 0 bottom, 1 top

                                // Position this layer along the branch
                                Vec3 layerPos = point.subtract(axis.multiply(l * leafInformation.layerSpacing));
                                double layerAngleOffset = Math.toRadians(leafInformation.rotationPerLayer * l);

                                // --- Decide elevation angle for this layer ---
                                // Example mapping:
                                //   bottom layer  -> -45°  (downwards)
                                //   middle layers ->  0°   (flat)
                                //   top layer     -> +30°  (slightly upward)
                                double minDeg = -45.0;  // bottom
                                double maxDeg =  30.0;  // top
                                double elevDeg = minDeg + (maxDeg - minDeg) * layerNorm;
                                double elevRad = Math.toRadians(elevDeg);

                                for (int i = 0; i < perLayer; i++) {
                                    // 1) full 0–2π spread around the trunk
                                    double angle = baseAngle
                                            + layerAngleOffset
                                            + (i * (2.0 * Math.PI / perLayer));

                                    double cosA = Math.cos(angle);
                                    double sinA = Math.sin(angle);

                                    // pure horizontal radial direction
                                    Vec3 radialDir = right.multiply(cosA)
                                            .add(forwardAround.multiply(sinA))
                                            .normalize();

                                    // 2) Build a direction with the desired elevation:
                                    //    project radialDir to XZ for "horiz", then mix with world-up.
                                    Vec3 horiz = new Vec3(radialDir.x, 0.0, radialDir.z);
                                    if (horiz.lengthSq() < 1e-6) horiz = new Vec3(1, 0, 0);
                                    horiz = horiz.normalize();

                                    // Rotate in plane (horiz, up) by elevRad: α<0 = down, α>0 = up
                                    Vec3 forwardDir = horiz.multiply(Math.cos(elevRad))
                                            .add(up.multiply(Math.sin(elevRad)))
                                            .normalize();

                                    submitFinalisingSubtask(sub -> placeLeaf(
                                            sub,
                                            0.0,
                                            node.order,
                                            layerPos,
                                            tangent,
                                            nodeLength,
                                            angle,
                                            thickness,
                                            forwardDir   // "straight" direction for this leaf (used by droop)
                                    ));
                                }
                            }
                        }
                    }
                    case BANANA -> {
                        // existing behaviour (if any) would go here
                    }
                }

                index++;
            }
        }

        LOGGER.ambient("Stamping for node: id {}, length {}, scale {}", node.id, node.getAllPoints().size(), node.length());
    }

    private record CanopySample(Vec3 pos, double horizNorm, double heightNorm) { }

    /**
     * Finalising task for realistic leaves.
     *
     * For EACH terminal point we:
     *   1) Place a base blob sized ~[0.4, 0.6] * branchLengthFromCenter.
     *   2) Add a few smaller blobs around that base blob.
     *   3) Add tiny blobs in the vicinity of the same terminal/base.
     *   4) Hollowing + air gaps are handled by generateLeafBlob (via hollowFactor & thinning).
     *   5) Each blob is copy‑pasted with a diagonal +1Y offset so the canopy feels layered.
     */
    private void buildRealisticLeafCanopy(SubGenerator subGen) {
        List<Pair<Vec3, Integer>> terminals;
        synchronized (realisticLeafTerminals) {
            if (realisticLeafTerminals.isEmpty()) return;
            terminals = new ArrayList<>(realisticLeafTerminals);
        }

        double rootLength = 0.0;
        if (tb != null && tb.getRoot() != null) {
            rootLength = tb.getRoot().length();
        }

        final double referenceLength = treeAge * 1.55; // tune to your species
        double scale = (referenceLength > 0.0 && rootLength > 0.0)
                ? rootLength / referenceLength
                : 1.0;

        scale = Math.max(0.4, Math.min(2.5, scale));
        System.out.println("Scale: " + scale);

        for (int i = 0; i < terminals.size(); i++) {
            Vec3 terminal = terminals.get(i).getKey();
            final int termIndex = i;
            if (terminal == null) return;
            double finalScale = scale * ((double) 1 / Math.max(1, terminals.get(i).getValue()));
            submitFinalisingSubtask((subGenerator) -> {
                int baseMinX = 25;
                int baseMaxX = 30;
                int baseMinY = 10;
                int baseMaxY = 15;
                int baseMinZ = 12;
                int baseMaxZ = 16;

                int minX = Math.max(1, (int) Math.round(baseMinX * finalScale));
                int maxX = Math.max(minX + 1, (int) Math.round(baseMaxX * finalScale));

                int minY = Math.max(1, (int) Math.round(baseMinY * finalScale));
                int maxY = Math.max(minY + 1, (int) Math.round(baseMaxY * finalScale));

                int minZ = Math.max(1, (int) Math.round(baseMinZ * finalScale));
                int maxZ = Math.max(minZ + 1, (int) Math.round(baseMaxZ * finalScale));

                double sizeX = rnd.nextInt(minX, maxX);
                double sizeY = rnd.nextInt(minY, maxY);
                double sizeZ = rnd.nextInt(minZ, maxZ);

                var blob = generateLeafBlobWithChildren(
                        terminal,
                        sizeX,
                        sizeY,
                        sizeZ,
                        1.0,
                        rnd
                );

                for (var vec : blob) {
                    subGenerator.guardAndStore(vec, leafMaterial, false);
                }
            });
        }
    }

    private @NotNull Function<Double, Double> getRadiusFunction(ThesisBasedTreeGenerator.TreeNode node) {
        CurveFunctions.Segment begHeight = new CurveFunctions.Segment(Math.max(1, node.baseRadius / 2.0), Math.max(1, node.baseRadius / 4.5), 0.0, 0.24, TimeCurve.INVERTED_QUADRATIC);
        CurveFunctions.Segment height = new CurveFunctions.Segment(Math.max(1, node.baseRadius / 4.5), node.canGrowTaller ? 0.0 : Math.max(1, node.baseRadius / 5.5), 0.24, 1.0, TimeCurve.TRUNK_TAPER);
        if (growthData.overrides.contains(ThesisBasedTreeGenerator.Overrides.TrunkOverrides.MULTI_TRUNKISM) && (node.order == 0)) {
            begHeight = new CurveFunctions.Segment(Math.max(1, node.baseRadius / 2.0), Math.max(1, node.baseRadius / 2.5), 0.0, 0.3, TimeCurve.INVERTED_QUADRATIC);
            height = new CurveFunctions.Segment(Math.max(1, node.baseRadius / 2.5), Math.max(1, node.baseRadius / 2.7), 0.3, 1.0, TimeCurve.INVERTED_QUADRATIC);
        }
        return CurveFunctions.multiFade(List.of(
                begHeight,
                height
        ));
    }

    public static Vec3 getHorizontalNormal(Vec3 tangent) {
        // Project tangent onto horizontal (XZ) plane
        Vec3 horiz = tangent.normalize();

        // If tangent was vertical, fallback to a default horizontal direction
        if (horiz.lengthSq() < 1e-6) {
            horiz = new Vec3(1, 0, 0); // arbitrary fallback
        }

        // Rotate 90° around Y to get horizontal normal (perpendicular in XZ)
        // (x, z) → (-z, x) for +90° rotation
        return new Vec3(-horiz.z, 0, horiz.x).normalize();
    }

    /**
     * Computes a position offset around a branch’s circular cross-section.
     *
     * @param tangent       The branch axis (direction vector).
     * @param angleInRads   The rotation angle around the circle (0–2π).
     * @param radius        The radius of the circle.
     * @param horizontalBias Optional flattening (0 = normal, 1 = fully horizontal bias).
     * @return Vec3 offset from the branch center, lying on the circle surface.
     */
    public static Vec3 getPointAroundBranchCircle(Vec3 tangent, double angleInRads, double radius, double horizontalBias) {
        tangent = tangent.normalize();

        // Step 1: Find a stable horizontal right vector
        Vec3 right = getHorizontalNormal(tangent);

        // Step 2: Build perpendicular vector in the plane
        Vec3 forward = tangent.crossProduct(right).normalize();

        // Step 3: Compute position on circle
        double cos = Math.cos(angleInRads);
        double sin = Math.sin(angleInRads);
        Vec3 circleOffset = right.multiply(cos).add(forward.multiply(sin)).normalize();

        // Step 4: Apply horizontal bias (flatten vertically)
        circleOffset = new Vec3(circleOffset.x, circleOffset.y * (1.0 - horizontalBias), circleOffset.z).normalize();

        // Step 5: Scale by radius
        return circleOffset.multiply(radius);
    }

    /**
     * Computes leaf facing direction around the branch, with optional per-leaf
     * vertical tilt provided by leafDirection (progress 0..1 -> angle -90..90).
     */
    public Vec3 getLeafFacingDirection(
            Vec3 tangent,
            double angleInRads,
            double thicknessAtPoint,
            double horizontalBias,
            DoubleUnaryOperator leafDirection,   // may be null
            double progress                      // 0..1 along branch/node
    ) {
        Vec3 forward = tangent.normalize();

        // 1. Build stable basis
        Vec3 worldUp = new Vec3(0, 1, 0);
        Vec3 right = worldUp.crossProduct(forward);
        if (right.lengthSq() < 1e-6) right = new Vec3(1, 0, 0);
        right = right.normalize();
        Vec3 adjustedUp = forward.crossProduct(right).normalize();

        // 2. Initial position around the branch
        double cosA = Math.cos(angleInRads);
        double sinA = Math.sin(angleInRads);
        // This is the vector pointing "out" from the branch surface
        Vec3 outDir = right.multiply(cosA).add(adjustedUp.multiply(sinA)).normalize();

        // 3. Apply Vertical Tilt (relative to the branch, not the world)
        if (leafDirection != null) {
            double deg = leafDirection.applyAsDouble(progress);
            double rad = Math.toRadians(Math.max(-90.0, Math.min(90.0, deg)));

            // TILT LOGIC:
            // We rotate 'outDir' toward the branch 'forward' direction.
            // This makes the leaf point "up" or "down" along the branch.
            outDir = outDir.multiply(Math.cos(rad))
                    .add(forward.multiply(Math.sin(rad)))
                    .normalize();
        }

        // 4. Global Horizontal Bias (if you want gravity/sun effects)
        // We only nudge the final result toward the world horizon
        Vec3 finalDir = new Vec3(outDir.x, outDir.y * (1.0 - horizontalBias), outDir.z);

        return finalDir.normalize();
    }

    int index = 0;

    private void placeLeaf(SubGenerator subGen, double progress, int order, Vec3 start, Vec3 tangent, double nodeLength, double angleInRads, double thicknessAtPoint) {
        var direction = getLeafFacingDirection(tangent, angleInRads, thicknessAtPoint, leafInformation.horizontalBias, leafInformation.leafDirection, progress);
        placeLeaf(subGen, progress, order, start, tangent, nodeLength, angleInRads, thicknessAtPoint, direction);
    }

    private void placeLeaf(SubGenerator subGen, double progress, int order, Vec3 start, Vec3 tangent, double nodeLength, double angleInRads, double thicknessAtPoint, Vec3 direction) {

        var leafWidth = thicknessAtPoint * leafInformation.leafBreadth * Math.max(0.1, 1.0 - progress);
        var leafLength = nodeLength * leafInformation.leafLength * Math.max(0.1, 1.0 - progress);
        var leafThickness = leafWidth * leafInformation.leafThickness;
        var startingPoint =
                start.add(getPointAroundBranchCircle(tangent, angleInRads,
                        thicknessAtPoint, leafInformation.horizontalBias));
        ;

        var breadthFunction =
                CurveFunctions.multiFade(
                        new CurveFunctions.Segment(0.0, leafWidth, 0.0, leafInformation.maxBreadthPoint, leafInformation.shrinkFactor),
                        new CurveFunctions.Segment(leafWidth, 0.0, leafInformation.maxBreadthPoint, 1.0, leafInformation.shrinkFactor)
                );

        if (leafInformation.leafStalk) {
            var stalkLength = leafLength * leafInformation.leafStalkLength;
            startingPoint = start.add(direction.multiply(stalkLength));
            Set<Vec3> generated = SpiralUtil.generateVineWithSpiralNoBezier(
                    new LinkedList<>(List.of(start, startingPoint)),
                    7,
                    0.8f,
                    CurveFunctions.radius(thicknessAtPoint * 0.75, leafThickness * 0.8, 0.0, 1.0, TimeCurve.INVERTED_QUADRATIC),
                    CurveFunctions.pitch(0.0, 0.05, 0.0, 1.0, TimeCurve.INVERTED_QUADRATIC)
            );
            generated.forEach(it -> subGen.guardAndStore(it, leafMaterial, false));
        }


        if (!leafInformation.compound) {
            Vec3 tip = startingPoint.add(direction.multiply(leafLength));
            var baseDroop = Math.pow(Math.max(1, order), 0.67) *
                    (-1.0 * leafInformation.droopFactor);
            List<Vec3> path;
            if (leafInformation.droopMode == LeafDroopMode.CURVED) {
                // existing Bézier "n" shape
                path = generateDroopCurve(
                        startingPoint,
                        tip,
                        baseDroop,
                        Math.max(7, (int) leafLength / 2)
                );
            }
            else if (leafInformation.droopMode == LeafDroopMode.STRAIGHT_DOWN) {
                // STRAIGHT_DOWN: bend from a configured forward direction toward gravity
                // For palms, you can choose forwardDir to be "outward" or "upward" per layer when you call placeLeaf.
                Vec3 forwardDir = direction.normalize();

                path = generateStraightDownDroop(
                        startingPoint,
                        forwardDir,
                        leafLength,
                        leafInformation.droopFactor,
                        leafInformation.droopStart,
                        leafInformation.droopCurve,
                        Math.max(7, (int) leafLength / 2)
                );
            }
            else path = List.of(startingPoint, tip);

            Set<Vec3> blade = SpiralUtil.generateWallPath(
                    path,
                    breadthFunction,
                    CurveFunctions.fade(leafThickness, 0.0, 0.0, 1.0, TimeCurve.QUADRATIC),
                    0.8f, 1.0f
            );
            blade.forEach(it -> subGen.guardAndStore(it, leafMaterial, false));
        }
    }

    /**
     * Monotonic droop: start along a "forward" direction, then smoothly bend toward
     * gravity (down or up) starting at droopStart (0..1 of blade length).
     *
     * @param start       base point of the leaf
     * @param forwardDir  initial straight direction of the leaf (normalized)
     * @param length      total arc length to approximate along the blade
     * @param droopFactor magnitude and sign of droop. >0 => toward -Y, <0 => toward +Y
     * @param droopStart  normalized [0..1] param where drooping begins along the leaf
     * @param droopCurve  curve defining the intensity of the droop transition
     * @param segments    number of segments to sample along the blade
     */
    private List<Vec3> generateStraightDownDroop(
            Vec3 start,
            Vec3 forwardDir,
            double length,
            double droopFactor,
            double droopStart,
            TimeCurve droopCurve,
            int segments
    ) {
        List<Vec3> pts = new ArrayList<>(segments + 1);

        // Sanitize inputs
        if (segments < 2 || length <= 0.0) {
            pts.add(start);
            pts.add(start.add(forwardDir.normalize().multiply(length)));
            return pts;
        }

        forwardDir = forwardDir.normalize();

        // Determine "gravity" direction from sign of droopFactor
        Vec3 gravityDir = droopFactor >= 0.0 ? new Vec3(0, -1, 0) : new Vec3(0, 1, 0);
        double droopMag = Math.abs(droopFactor); // cap to 1 for blending

        double stepLen = length / segments;
        double clampedStart = Math.max(0.0, Math.min(1.0, droopStart));

        Vec3 pos = start;
        pts.add(pos);

        for (int i = 1; i <= segments; i++) {
            double t = (double) i / segments; // 0..1 along arc

            double droopStrength;
            float transitionWidth = 0.2f;

            if (t <= clampedStart || droopMag <= 0.0) {
                droopStrength = 0.0;
            }
            else if (t < clampedStart + transitionWidth) {
                // Normalize tau so it goes 0.0 -> 1.0 exactly within the transitionWidth
                float tau = (float)((t - clampedStart) / transitionWidth);

                // Apply your curve (Ease-in, etc.)
                float eased = droopCurve.apply(tau);
                droopStrength = eased * droopMag;
            }
            else {
                // Beyond the transition window, we stay at full magnitude
                droopStrength = droopMag;
            }

            double lerpT = Math.min(1.0, droopStrength);

            Vec3 dir = forwardDir.lerp(gravityDir, lerpT).normalize();
            pos = pos.add(dir.multiply(stepLen));
            pts.add(pos);
        }

        return pts;
    }

    private List<Vec3> generateDroopCurve(Vec3 start, Vec3 end, double droopFactor, int segments) {
        // Direction from base to tip
        Vec3 dir = end.subtract(start).normalize();
        double length = start.distance(end);

        // Control point closer to the tip (e.g. 70–85% along the path)
        double controlBias = 0.8;

        // Start -> tip partial point
        Vec3 preTip = start.add(dir.multiply(length * controlBias));

        // Droop direction — downward relative to world Y
        Vec3 droopDir = new Vec3(0, -1, 0);

        // Control point — pull downward near the tip to cause droop
        Vec3 control = preTip.add(droopDir.multiply(length * droopFactor));

        List<Vec3> points = new ArrayList<>();
        for (int i = 0; i <= segments; i++) {
            double t = (double) i / segments;
            // Quadratic Bézier interpolation
            Vec3 point = start.multiply(Math.pow(1 - t, 2))
                    .add(control.multiply(2 * (1 - t) * t))
                    .add(end.multiply(t * t));
            points.add(point);
        }

        return points;
    }


    public enum NodeLeafingType {
        /**
         * <b>Structure:</b> One leaf per node, each leaf grows at a fixed rotation angle from the previous one.
         * <br><b>Common angle:</b> ~137.5° (the golden angle).
         * <br><b>Pattern:</b> Spiral up the stem — you never get perfect vertical alignment.
         * <br><b>Examples:</b> Oak, sunflower, elm.
         */
        ALTERNATE,
        /**
         * <b>Structure:</b> Two leaves per node, directly opposite each other. Each subsequent pair may rotate 90° or 180°.
         * <br><b>Examples:</b> Maple (decussate), grass and bamboo (distichous).
         */
        OPPOSITE,
        /**
         * <b>Structure:</b> Two leaves per node, directly opposite each other. Each subsequent pair keeps the rotation of the previous.
         * <br><b>Examples:</b> Maple (decussate), grass and bamboo (distichous).
         */
        OPPOSITE_DISTICHOUS,
        /**
         * <b>Structure:</b> Three or more leaves per node arranged in a circle around the stem.
         * <br><b>Examples:</b> Oleander, Alstonia, or horsetail.
         */
        WHORLED,
        /**
         * <b>Structure:</b> All leaves emerge at ground level, forming a circular “rosette.”
         * <br><b>Examples:</b> Dandelion, cabbage, agave.
         */
        ROSETTE,
        /**
         * <b>Structure:</b> Appears whorled but actually spiral with closely spaced internodes — leaves look like clusters.
         * <br><b>Examples:</b> Some pines and conifers.
         */
        VERTICILLATE,
        /**
         * <b>Structure:</b> Needles grouped in bundles (“fascicles”).
         * <br><b>Examples:</b> Pines (2–5 needles per fascicle).
         */
        FACISULATE,
        /**
         * <b>Structure:</b> Leaves (or leaf clusters) grow only at the distal end of a branch or shoot, not along its length.
         * <br><b>Examples:</b> Pines (2–5 needles per fascicle).
         */
        TERMINAL,
        /**
         * Represents a leafing pattern in which leaves (or leaf clusters) are fan-shaped and grow
         * radially from a central point, resembling the structure of a palm tree.
         */
        PALM,

        /**
         * Represents a leafing pattern associated with banana plants.
         * The leaves are large, elongated, and grow in a spiral arrangement
         * around the pseudostem, which is formed from overlapping leaf bases.
         */
        BANANA
    }

    public enum LeafDroopMode {
        /**
         * Current behavior: quadratic Bézier with a control point pulled downward near the tip.
         * Produces an arch / "n" shaped droop.
         */
        CURVED,
        /**
         * Simple, monotonic droop: leaf direction bent downward along its length,
         * closer to "straight then downwards".
         */
        STRAIGHT_DOWN
    }

    public static class LeafDetails {
        /**
         * The tendency for the leaf to tend horizontally
         */
        public float horizontalBias = 0.0f;
        /**
         * The branch node point from which leaves begin, The actual value is 1 as it is computed as:
         * <br> x > startIndex "not" x >= startIndex
         */
        public int startIndex = 2;
        /**
         * This is used for non-TERMINAL leaf type
         */
        private float leafSpawningPoint = 0.0f;
        /**
         * This is used for non-TERMINAL leaf type
         */
        private float leafSpawningPointEnd = 1.0f;
        /**
         * This is relative to the branch's size where 1.0 is a whole thickness and 0.0 is not possible... we set a minimum of 0.1
         */
        private float leafSpacing = 2.0f;
        private NodeLeafingType leafType = NodeLeafingType.ALTERNATE;
        private boolean heightReduction = true;
        /**
         * This is relative to the branch's thickness
         */
        private float leafBreadth = 4.45f;
        /**
         * This is relative to the branch's size
         */
        private float leafLength = 0.24f;
        /**
         * The point on the leaf at which the breath becomes its widest
         */
        private float maxBreadthPoint = 0.3f;
        /**
         * The formula for shrinking the leaf to and from the {@link LeafDetails#maxBreadthPoint}
         */
        private TimeCurve shrinkFactor = TimeCurve.LINEAR;

        private TimeCurve heightReductionCurve = TimeCurve.LINEAR;
        /**
         * This is relative to {@link LeafDetails#leafBreadth}
         */
        private float leafThickness = 0.12f;
        /**
         * Makes the leaf start drooping starting from the {@link LeafDetails#maxBreadthPoint} * 1.134
         */
        private float droopFactor = 0.67f;
        /**
         * Normalized point along the leaf length [0..1] where drooping begins for STRAIGHT_DOWN mode.
         * 0.0 = start drooping immediately, 0.5 = middle of the blade, 1.0 = no droop.
         */
        private float droopStart = 0.0f;
        /**
         * The curve used for STRAIGHT_DOWN mode to interpolate the droop strength after droopStart.
         * Default is QUADRATIC (standard ease-in).
         */
        private TimeCurve droopCurve = TimeCurve.QUADRATIC;
        /**
         * How to compute droop along the blade.
         */
        private LeafDroopMode droopMode = LeafDroopMode.CURVED;
        /**
         * This makes the leaf exhibit "philodendronism". Basically splitting
         */
        private boolean compound = false;
        /**
         * Function mapping 0.0–1.0 progress -> vertical angle in degrees [-90, 90].
         * Applied in getLeafFacingDirection to adjust Y before horizontalBias is applied.
         * Null = use default behavior.
         */
        private DoubleUnaryOperator leafDirection = null;
        /**
         * This makes the leaf generation use the beta realistic leaf system
         */
        private boolean useRealisticType = false;
        /**
         * This is the amount of Math.pow for node length to use for leaf size
         */
        public double realismPow = 0.65;
        /**
         * Makes the leaf have exhibit splitting from behind instead of only in-front
         */
        private boolean initialBackwardPointing = false;
        private int compoundSplit = 2;
        /**
         * Number of leaves per layer (for ROSETTE, PALM, WHORLED).
         */
        private int leafCountPerLayer = 8;
        /**
         * Number of layers (for PALM).
         */
        private int layerCount = 1;
        /**
         * Spacing between layers in blocks/units (for PALM).
         */
        private float layerSpacing = 0.2f;
        /**
         * Rotation offset per layer in degrees (for PALM).
         */
        private float rotationPerLayer = 15.0f;
        /**
         * Makes the leaf have a "realistic" stalk before it grows
         */
        private boolean leafStalk = false;
        /**
         * This is relative to the final {@link LeafDetails#leafLength}
         */
        private float leafStalkLength = 0.24f;
        /**
         * If the value of this is set it is used instead of the above parameters to generate leaves.
         * <br> Its is basically a supplier for the leaf structure taking a scale parameter for the function.
         */
        private Function<Double, List<Vec3>> decoration = null;

        private LeafDetails() {}

        private LeafDetails(Builder builder) {
            startIndex = builder.startIndex;
            leafBreadth = builder.leafBreath;
            horizontalBias = builder.horizontalBias;
            leafSpawningPointEnd = builder.leafSpawningPointEnd;
            leafSpawningPoint = builder.leafSpawningPoint;
            leafSpacing = builder.leafSpacing;
            leafLength = builder.leafLength;
            leafStalkLength = builder.leafStalkLength;
            leafDirection = builder.leafDirection;
            maxBreadthPoint = builder.maxBreadthPoint;
            shrinkFactor = builder.shrinkFactor;
            heightReductionCurve = builder.heightReductionCurve;
            leafThickness = builder.leafThickness;
            droopFactor = builder.droopFactor;
            droopStart = builder.droopStart;
            droopCurve = builder.droopCurve;
            compound = builder.compound;
            useRealisticType = builder.useRealisticType;
            realismPow = builder.realismPow;
            initialBackwardPointing = builder.initialBackwardPointing;
            leafStalk = builder.leafStalk;
            heightReduction = builder.heightReduction;
            compoundSplit = builder.compoundSplit;
            decoration = builder.decoration;
            leafType = builder.leafType;
            leafCountPerLayer = builder.leafCountPerLayer;
            layerCount = builder.layerCount;
            layerSpacing = builder.layerSpacing;
            rotationPerLayer = builder.rotationPerLayer;
            droopMode = builder.droopMode;
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public static final class Builder {
            /**
             * The tendency for the leaf to tend horizontally
             */
            public float horizontalBias = 0.0f;
            /**
             * The branch node point from which leaves begin, The actual value is 1 as it is computed as:
             * <br> x > startIndex "not" x >= startIndex
             */
            public int startIndex = 2;
            public double realismPow = 0.65;
            /**
             * This is used for non-TERMINAL leaf type
             */
            private float leafSpawningPoint = 0.0f;
            /**
             * This is used for non-TERMINAL leaf type
             */
            private float leafSpawningPointEnd = 1.0f;
            /**
             * This is relative to the branch's size where 1.0 is a whole thickness and 0.0 is not possible... we set a minimum of 0.1
             */
            private float leafSpacing = 2.0f;
            private NodeLeafingType leafType = NodeLeafingType.ALTERNATE;
            /**
             * This is relative to the branch's thickness
             */
            private float leafBreath = 4.45f;
            /**
             * This is relative to the branch's size
             */
            private float leafLength = 0.24f;
            /**
             * The point on the leaf at which the breath becomes its widest
             */
            private float maxBreadthPoint = 0.3f;
            /**
             * The formula for shrinking the leaf to and from the {@link LeafDetails#maxBreadthPoint}
             */
            private TimeCurve shrinkFactor = TimeCurve.LINEAR;

            private TimeCurve heightReductionCurve = TimeCurve.LINEAR;
            /**
             * This is relative to {@link LeafDetails#leafBreadth}
             */
            private float leafThickness = 0.12f;
            /**
             * Makes the leaf start drooping starting from the {@link LeafDetails#maxBreadthPoint} * 1.134
             */
            private float droopFactor = 0.67f;
            /**
             * Normalized point along the leaf length [0..1] where drooping begins for STRAIGHT_DOWN mode.
             * 0.0 = start drooping immediately, 0.5 = middle of the blade, 1.0 = no droop.
             */
            private float droopStart = 0.0f;
            /**
             * The curve used for STRAIGHT_DOWN mode to interpolate the droop strength after droopStart.
             * Default is QUADRATIC (standard ease-in).
             */
            private TimeCurve droopCurve = TimeCurve.QUADRATIC;
            /**
             * How to compute droop along the blade.
             */
            private LeafDroopMode droopMode = LeafDroopMode.CURVED;
            /**
             * This makes the leaf exhibit "philodendronism". Basically splitting
             */
            private boolean compound = false;
            /**
             * Number of leaves per layer (for ROSETTE, PALM, WHORLED).
             */
            private int leafCountPerLayer = 8;
            /**
             * Number of layers (for PALM).
             */
            private int layerCount = 1;
            /**
             * Spacing between layers in blocks/units (for PALM).
             */
            private float layerSpacing = 0.2f;
            /**
             * Rotation offset per layer in degrees (for PALM).
             */
            private float rotationPerLayer = 15.0f;
            /**
             * This makes the leaf generation use the new beta realistic leaves
             */
            private boolean useRealisticType = false;
            /**
             * Makes the leaf have exhibit splitting from behind instead of only in-front
             */
            private boolean initialBackwardPointing = false;
            private int compoundSplit = 2;
            /**
             * Makes the leaf have a "realistic" stalk before it grows
             */
            private boolean leafStalk = false;
            private boolean heightReduction = true;
            /**
             * This is relative to the final {@link LeafDetails#leafLength}
             */
            private float leafStalkLength = 0.24f;
            /**
             * Function mapping 0.0–1.0 progress -> vertical angle in degrees [-90, 90].
             * Applied in getLeafFacingDirection to adjust Y before horizontalBias is applied.
             * Null = use default behavior.
             */
            private DoubleUnaryOperator leafDirection = null;
            /**
             * If the value of this is set it is used instead of the above parameters to generate leaves.
             * <br> Its is basically a supplier for the leaf structure taking a scale parameter for the function.
             */
            private Function<Double, List<Vec3>> decoration = null;

            private Builder() {}

            public Builder horizontalBias(float val) {
                horizontalBias = val;
                return this;
            }

            public Builder startIndex(int val) {
                startIndex = val;
                return this;
            }

            public Builder droopMode(LeafDroopMode val) {
                droopMode = val;
                return this;
            }

            public Builder leafBreath(float val) {
                leafBreath = val;
                return this;
            }

            public Builder leafSpawningPoint(float val) {
                leafSpawningPoint = Math.max(0.0f, val);
                return this;
            }

            public Builder layerCount(int val) {
                layerCount = val;
                return this;
            }

            public Builder leafCountPerLayer(int val) {
                leafCountPerLayer = val;
                return this;
            }

            public Builder layerSpacing(float val) {
                layerSpacing = val;
                return this;
            }

            public Builder rotationPerLayer(float val) {
                rotationPerLayer = val;
                return this;
            }

            public Builder leafSpawningPointEnd(float val) {
                leafSpawningPointEnd = Math.min(1.0f, val);
                return this;
            }

            public Builder leafSpawningPointRange(float min, float max) {
                leafSpawningPoint = Math.max(0.0f, min);
                leafSpawningPointEnd = Math.min(1.0f, max);
                return this;
            }

            public Builder leafSpacing(float val) {
                leafSpacing = Math.max(0.1f, val);
                return this;
            }

            public Builder realismPow(double val) {
                realismPow = Math.max(0.1f, val);
                return this;
            }

            public Builder leafLength(float val) {
                leafLength = val;
                return this;
            }

            public Builder leafStalkLength(float val) {
                leafStalkLength = val;
                return this;
            }

            public Builder leafDirection(DoubleUnaryOperator val) {
                leafDirection = val;
                return this;
            }

            public Builder maxBreadthPoint(float val) {
                maxBreadthPoint = val;
                return this;
            }

            public Builder leafType(NodeLeafingType leafType) {
                this.leafType = leafType;
                return this;
            }

            public Builder shrinkFactor(TimeCurve val) {
                shrinkFactor = val;
                return this;
            }

            public Builder heightReductionCurve(TimeCurve val) {
                heightReductionCurve = val;
                return this;
            }

            public Builder leafThickness(float val) {
                leafThickness = val;
                return this;
            }

            public Builder droopFactor(float val) {
                droopFactor = val;
                return this;
            }

            public Builder droopStart(float val) {
                droopStart = val;
                return this;
            }

            public Builder droopCurve(TimeCurve val) {
                droopCurve = val;
                return this;
            }

            public Builder compound(boolean val) {
                compound = val;
                return this;
            }

            public Builder useRealisticType(boolean val) {
                useRealisticType = val;
                return this;
            }

            public Builder leafStalk(boolean val) {
                leafStalk = val;
                return this;
            }

            public Builder heightReduction(boolean val) {
                heightReduction = val;
                return this;
            }

            public Builder initialBackwardPointing(boolean val) {
                initialBackwardPointing = val;
                return this;
            }

            public Builder compoundSplit(int val) {
                compoundSplit = val;
                return this;
            }

            public Builder decoration(Function<Double, List<Vec3>> val) {
                decoration = val;
                return this;
            }

            public LeafDetails build() {
                return new LeafDetails(this);
            }
        }
    }

    // ---------------- Builder ----------------

    public static class Builder<T> extends BaseBuilder<T, ThesisTreeStructureGenerator<T>> {
        // required primitives/enums/functions only
        private int treeAge = 8;
        private Pair<Integer, Integer> trunkHeight = new Pair<>(8, 15);
        private Pair<Double, Double> trunkWidth = new Pair<>(1.5, 6.0);
        private int trunkSegments = 16;
        private int maxLeavesPerBranch = 30;
        private double spread = 1.0;
        private PlatformBlockState<T> trunkBlockMaterial;
        private PlatformBlockState<T> leafMaterial;
        private boolean placeLeaves = true;
        private ThesisBasedTreeGenerator.GrowthData growthData = null;
        private DoubleUnaryOperator thicknessFunction = t -> 1.0 - 0.6 * t;
        private long seed = new Random().nextLong();
        protected LeafDetails leafInformation = new LeafDetails();

        public Builder<T> treeAge(int a) {
            this.treeAge = Math.max(1, a);
            return this;
        }

        public Builder<T> leafDetails(LeafDetails leafInformation) {
            this.leafInformation = leafInformation;
            return this;
        }

        public Builder<T> trunkHeight(Pair<Integer, Integer> h) {
            this.trunkHeight = h;
            return this;
        }

        public Builder<T> trunkRadius(Pair<Double, Double> w) {
            this.trunkWidth = w;
            return this;
        }

        public Builder<T> trunkHeight(int min, int max) {
            this.trunkHeight = new Pair<>(min, max);
            return this;
        }

        public Builder<T> trunkRadius(double min, double max) {
            this.trunkWidth = new Pair<>(min, max);
            return this;
        }

        public Builder<T> trunkSegments(int s) {
            this.trunkSegments = Math.max(3, s);
            return this;
        }

        public Builder<T> maxLeavesPerBranch(int s) {
            this.maxLeavesPerBranch = Math.max(1, s);
            return this;
        }

        public Builder<T> spread(double q) {
            this.spread = Math.max(0.1, q);
            return this;
        }

        public Builder<T> trunkMaterial(PlatformBlockState<T> id) {
            this.trunkBlockMaterial = Objects.requireNonNull(id);
            return this;
        }

        public Builder<T> leafMaterial(PlatformBlockState<T> id) {
            this.leafMaterial = Objects.requireNonNull(id);
            return this;
        }

        public Builder<T> placeLeaves(boolean v) {
            this.placeLeaves = v;
            return this;
        }

        public Builder<T> growthData(ThesisBasedTreeGenerator.GrowthData gd) {
            this.growthData = gd;
            return this;
        }

        public Builder<T> thicknessFunction(DoubleUnaryOperator fn) {
            this.thicknessFunction = fn;
            return this;
        }

        public Builder<T> seed(long s) {
            this.seed = s;
            return this;
        }

        @Override
        public void validate() {
            if (Objects.equals(trunkHeight.key(), trunkHeight.value()))
                throw new IllegalArgumentException("trunkHeight range must be >= 1 and not equal");
            if (Objects.equals(trunkWidth.key(), trunkWidth.value()))
                throw new IllegalArgumentException("trunkWidth range must be > 0 and not equal");
            if (trunkBlockMaterial == null) throw new IllegalArgumentException("trunkBlockMaterial required");
            if (leafMaterial == null && placeLeaves) throw new IllegalArgumentException("trunkBlockMaterial required");
            if (thicknessFunction == null) throw new IllegalArgumentException("thicknessFunction required");
        }

        @Override
        protected ThesisTreeStructureGenerator<T> create() {
            // Build a generator instance with these primitive/config items
            return new ThesisTreeStructureGenerator<>(this);
        }
    }
}
