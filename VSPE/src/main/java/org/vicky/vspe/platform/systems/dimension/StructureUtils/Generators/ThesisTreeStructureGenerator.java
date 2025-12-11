package org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators;

import kotlin.Triple;
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
import java.util.function.BiConsumer;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;

import static org.vicky.vspe.branch.BranchKt.generateLeafBlob;

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
                this.growthData != null ? this.growthData : new ThesisBasedTreeGenerator.GrowthData((float) (spread), trunkHeight);
        gd.forTrunk = forTrunk;

        // Create the thesis generator (world-agnostic)
        ThesisBasedTreeGenerator tb = new ThesisBasedTreeGenerator(gd, localSeed ^ rnd.nextLong(), LOGGER);

        // init root at origin pointing upwards
        tb.initRoot(origin, new Vec3(0f, 1f, 0f), (float) trunkWidth);

        tb.simulateToAge(treeAge);

        // Convert the tree nodes into block placements
        // Map primitive trunkBlockId -> PlatformBlockState<T>

        // Compute maximum order so we can normalize thickness functions by depth
        int maxOrder = 1;
        for (ThesisBasedTreeGenerator.TreeNode node : tb.getCachedBranches().values()) {
            if (node != null) maxOrder = Math.max(maxOrder, node.order);
        }
        submitSubtask((sub)-> stampBranchRecursively(sub, tb.getRoot(), trunkBlockMaterial));

        // Flush buffered placements into outPlacements/map by running the configured flush
        if (flush != null) flush.run();

        // The generate(...) wrapper will handle merging subtasks and returning the final GenerationResult
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


    // Recursively traverse starting at node and stamp tubes for main and lateral children
    private void stampBranchRecursively(SubGenerator subGen, ThesisBasedTreeGenerator.TreeNode node, PlatformBlockState<T> state) {
        if (node == null || node.getControlPoints().size() < 2) return;

        // pick functions depending on order or node type
        Function<Double, Double> radiusFunction = getRadiusFunction(node);

        double branchScale = Math.pow(node.baseRadius, 0.75) * 0.5;
        Function<Double, Double> thicknessFunction =
                CurveFunctions.radius(branchScale, 0.0, 0.0, 1.0, TimeCurve.INVERTED_QUADRATIC);

        Function<Double, Double> pitchFunction =
                CurveFunctions.pitch(0.03, 0.1, 0.2, 1.0, TimeCurve.EASE_IN_OUT_CUBIC);

        // Pass your control points straight to the spiral generator
        Set<Vec3> generated = SpiralUtil.generateVineWithSpiralNoBezier(
                new LinkedList<>(node.getControlPoints()),
                thicknessFunction,
                7,
                0.8f,
                radiusFunction,
                pitchFunction
        );

        // Stamp them into the world
        for (Vec3 v : generated) {
            subGen.guardAndStore(v, state, true);
        }


        final double nodeLength = SpiralUtil.findLengthOfPath(new ArrayList<>(node.getControlPoints()));

        if (node.order > leafInformation.startIndex && placeLeaves &&
                leafInformation.useRealisticType) {
            submitSubtask(sub -> placeLeaf(sub, 0.0, node.order,
                    node.getControlPoints().getLast(), null, nodeLength, 0, 0));
        }
        else if (node.order > leafInformation.startIndex && placeLeaves) {
            final double startDist = nodeLength * leafInformation.leafSpawningPoint;
            final double endDist = nodeLength * leafInformation.leafSpawningPointEnd;
            final double leafSpacing = node.baseRadius * leafInformation.leafSpacing;

            Queue<Quad<Double, Vec3, Double, Vec3>> leafingPoints = new LinkedList<>();
            for (double dist = startDist; dist <= endDist; dist += leafSpacing) {
                double t = dist / nodeLength; // normalized 0–1 position along branch
                Vec3 point = SpiralUtil.findPointOnPathFromLength(dist, node.getControlPoints());

                // Evaluate both curves at t
                double radius = radiusFunction.apply(t);
                double thickness = thicknessFunction.apply(t);

                // The combined value you want
                double totalThickness = radius + thickness;

                leafingPoints.add(new Quad<>(t, point, totalThickness, SpiralUtil.tangentAtLength(dist, node.getControlPoints())));
                // LOGGER.print("Leafing points for node {}: {}", node.id, leafingPoints);
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
                double progress = polled.first;
                Vec3 tangent = polled.fourth;
                switch (leafInformation.leafType) {
                    case ALTERNATE -> {
                        // Rotate by golden angle each step
                        double angle = baseAngle + index * goldenAngle;
                        submitSubtask(sub -> placeLeaf(sub, progress, node.order, point, tangent, nodeLength, angle, thickness));
                    }
                    case OPPOSITE -> {
                        // Two opposite leaves (180° apart)
                        double angle1 = baseAngle + index * Math.toRadians(90);
                        double angle2 = angle1 + Math.PI;
                        submitSubtask(sub -> placeLeaf(sub, progress, node.order, point, tangent, nodeLength, angle1, thickness));
                        submitSubtask(sub -> placeLeaf(sub, progress, node.order, point, tangent, nodeLength, angle2, thickness));
                    }
                    case OPPOSITE_DISTICHOUS -> {
                        // Opposite leaves, all aligned (same angle pair)
                        double angle2 = baseAngle + Math.PI;
                        submitSubtask(sub -> placeLeaf(sub, progress, node.order, point, tangent, nodeLength, baseAngle, thickness));
                        submitSubtask(sub -> placeLeaf(sub, progress, node.order, point, tangent, nodeLength, angle2, thickness));
                    }
                    case WHORLED -> {
                        // 3–5 leaves around the node evenly spaced
                        int whorlCount = 3 + (int) (Math.random() * 3); // 3–5
                        for (int i = 0; i < whorlCount; i++) {
                            double angle = baseAngle + (i * (2 * Math.PI / whorlCount));
                            submitSubtask(sub -> placeLeaf(sub, progress, node.order, point, tangent, nodeLength, angle, thickness));
                        }
                    }
                    case ROSETTE -> {
                        // Flat circular base near ground
                        int count = 8;
                        for (int i = 0; i < count; i++) {
                            double angle = baseAngle + (i * (2 * Math.PI / count));
                            submitSubtask(sub -> placeLeaf(sub, progress, node.order, point, tangent, nodeLength, angle, thickness));
                        }
                    }
                    case VERTICILLATE -> {
                        // Like whorled but tighter spacing (closer angles)
                        int ringCount = 5;
                        for (int i = 0; i < ringCount; i++) {
                            double angle = baseAngle + i * (2 * Math.PI / (ringCount * 1.5));
                            submitSubtask(sub -> placeLeaf(sub, progress, node.order, point, tangent, nodeLength, angle, thickness));
                        }
                    }
                    case FACISULATE -> {
                        // Needles grouped in bundles (small cluster offset)
                        int bundleSize = 3 + (int) (Math.random() * 3);
                        for (int i = 0; i < bundleSize; i++) {
                            double angle = baseAngle + i * (Math.PI / bundleSize);
                            submitSubtask(sub -> placeLeaf(sub, progress, node.order, point, tangent, nodeLength, angle, thickness));
                        }
                    }
                    case TERMINAL -> {
                        // Only place one leaf at the very end
                        if (leafingPoints.isEmpty()) {
                            submitSubtask(sub -> placeLeaf(sub, progress, node.order, point, tangent, nodeLength, baseAngle, thickness));
                        }
                    }
                }

                index++;
            }
        }

        // Recurse into children
        for (ThesisBasedTreeGenerator.TreeNode child : node.getChildren()) {
            submitSubtask((sub) -> stampBranchRecursively(sub, child, state));
        }
    }

    private @NotNull Function<Double, Double> getRadiusFunction(ThesisBasedTreeGenerator.TreeNode node) {
        CurveFunctions.Segment begHeight = new CurveFunctions.Segment(Math.max(1, node.baseRadius / 2.0), Math.max(1, node.baseRadius / 3.5), 0.0, 0.3, TimeCurve.INVERTED_QUADRATIC);
        CurveFunctions.Segment height = new CurveFunctions.Segment(Math.max(1, node.baseRadius / 3.5), node.baseRadius / 8.0, 0.3, 1.0, TimeCurve.QUADRATIC);
        if (growthData.overrides.contains(ThesisBasedTreeGenerator.Overrides.TrunkOverrides.MULTI_TRUNKISM) && (node.order == 0)) {
            begHeight = new CurveFunctions.Segment(Math.max(1, node.baseRadius / 2.0), Math.max(1, node.baseRadius / 2.5), 0.0, 0.3, TimeCurve.INVERTED_QUADRATIC);
            height = new CurveFunctions.Segment(Math.max(1, node.baseRadius / 2.5), Math.max(1, node.baseRadius / 2.7), 0.3, 1.0, TimeCurve.INVERTED_QUADRATIC);
        }
        return CurveFunctions.multiFade(List.of(
                begHeight,
                height
        ));
    }

    public static Vec3 getLeafFacingDirection(
            Vec3 tangent, double angleInRads, double thicknessAtPoint, double horizontalBias
    ) {
        // 1. Normalize tangent
        Vec3 forward = tangent.normalize();

        // 2. Choose an arbitrary "up" vector that isn’t parallel to tangent
        Vec3 up = Math.abs(forward.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);

        // 3. Create orthogonal basis
        Vec3 right = up.crossProduct(forward).normalize();
        Vec3 adjustedUp = forward.crossProduct(right).normalize();

        // 4. Rotate around the tangent by `angleInRads`
        double cos = Math.cos(angleInRads);
        double sin = Math.sin(angleInRads);

        Vec3 circleDir = right.multiply(cos).add(adjustedUp.multiply(sin)).normalize();

        // 5. Blend toward horizontal
        // Reduce Y component influence, creating a "horizontal" tendency.
        Vec3 horizontalBiasDir = new Vec3(circleDir.x, circleDir.y * (1.0 - horizontalBias), circleDir.z);
        horizontalBiasDir = horizontalBiasDir.normalize();

        // 6. Offset by thicknessAtPoint (optional — for leaf position placement)

        // Return direction or position offset depending on use
        return horizontalBiasDir.multiply(thicknessAtPoint);
    }

    int index = 0;

    private void placeLeaf(SubGenerator subGen, double progress, int order, Vec3 start, Vec3 tangent, double nodeLength, double angleInRads, double thicknessAtPoint) {
        if (leafInformation.useRealisticType) {
            double realism = 0.7;
            double decay = 0.7;
            int width = (int) Math.round((Math.pow(nodeLength, leafInformation.realismPow) * 2) * Math.pow(decay, order - 1));
            double density = 0.4 + realism * 0.5;           // 0.4 → 0.9
            int refinement = 2 + (int) Math.round(realism * 3);  // 2 → 5
            double hollow = 0.4 - realism * 0.3;            // 0.4 → 0.1
            double fluff = 0.8 - realism * 0.6;             // 0.8 → 0.2
            double leafScale = 1.4 - realism * 0.7;

            var points = generateLeafBlob(
                    start,
                    (width * 1.2 * leafScale) / 2,
                    (width * 0.7 * leafScale) / 2,
                    (width * leafScale) / 2,
                    density,
                    refinement,
                    hollow,
                    fluff
            );
            for (var point : points) {
                subGen.guardAndStore(point, leafMaterial, false);
            }
        }
        else {
            var direction = getLeafFacingDirection(tangent, angleInRads, thicknessAtPoint, leafInformation.horizontalBias);
            LOGGER.print("Direction {}, tangent {}", direction, tangent);
            var leafWidth = thicknessAtPoint * leafInformation.leafBreadth * Math.max(0.1, 1.0 - progress);
            var leafLength = nodeLength * leafInformation.leafLength * Math.max(0.1, 1.0 - progress);
            var leafThickness = leafWidth * leafInformation.leafThickness;
            var startingPoint =
                    start.add(getPointAroundBranchCircle(tangent, angleInRads,
                            thicknessAtPoint, leafInformation.horizontalBias));;

            LOGGER.print("placing leaf {}, width: {} height: {}", index++, leafWidth, leafLength);
            var breadthFunction =
                    CurveFunctions.multiFade(
                            new CurveFunctions.Segment(0.0, leafWidth, 0.0, leafInformation.maxBreadthPoint, leafInformation.shrinkFactor),
                            new CurveFunctions.Segment(leafWidth, 0.0, leafInformation.maxBreadthPoint, 1.0, leafInformation.shrinkFactor)
                    );

            if (leafInformation.leafStalk) {
                var stalkLength = leafLength * leafInformation.leafStalkLength;
                startingPoint = start.add(direction.multiply(stalkLength));
                double branchScale = Math.pow(leafWidth * 0.34, 0.75) * 0.5;
                Set<Vec3> generated = SpiralUtil.generateVineWithSpiralNoBezier(
                        new LinkedList<>(List.of(start, startingPoint)),
                        CurveFunctions.radius(branchScale, branchScale * 0.3, 0.0, 1.0, TimeCurve.INVERTED_QUADRATIC),
                        7,
                        0.8f,
                        CurveFunctions.radius(leafWidth * 0.34, leafWidth * 0.34 * 0.3, 0.0, 1.0, TimeCurve.INVERTED_QUADRATIC),
                        CurveFunctions.pitch(0.0, 0.05, 0.0, 1.0, TimeCurve.INVERTED_QUADRATIC)
                );
                generated.forEach(it -> subGen.guardAndStore(it, leafMaterial, false));
            }


            if (!leafInformation.compound) {
                Vec3 tip = startingPoint.add(direction.multiply(leafLength));
                var path = generateDroopCurve(startingPoint, tip, Math.pow(order, 0.67) * (-1.0 * leafInformation.droopFactor), Math.max(7, (int) leafLength / 2));
                // LOGGER.print("Start: {}, End: {}, Path: {}", startingPoint, tip, path);
                Set<Vec3> blade = SpiralUtil.generateWallPath(
                        path,
                        breadthFunction,
                        CurveFunctions.fade(leafThickness, 0.0, 0.0, 1.0, TimeCurve.QUADRATIC),
                        0.8f, 0.8f
                );
                blade.forEach(it -> subGen.guardAndStore(it, leafMaterial, false));
            }
        }
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
    }

    public static class LeafDetails {
        /**
         * The tendency for the leaf to tend horizontally
         */
        public float horizontalBias = 1.0f;
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
        /**
         * This is relative to {@link LeafDetails#leafBreadth}
         */
        private float leafThickness = 0.12f;
        /**
         * Makes the leaf start drooping starting from the {@link LeafDetails#maxBreadthPoint} * 1.134
         */
        private float droopFactor = 0.67f;
        /**
         * This makes the leaf exhibit "philodendronism". Basically splitting
         */
        private boolean compound = false;
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
            maxBreadthPoint = builder.maxBreadthPoint;
            shrinkFactor = builder.shrinkFactor;
            leafThickness = builder.leafThickness;
            droopFactor = builder.droopFactor;
            compound = builder.compound;
            useRealisticType = builder.useRealisticType;
            realismPow = builder.realismPow;
            initialBackwardPointing = builder.initialBackwardPointing;
            leafStalk = builder.leafStalk;
            compoundSplit = builder.compoundSplit;
            decoration = builder.decoration;
            leafType = builder.leafType;
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public static final class Builder {
            /**
             * The tendency for the leaf to tend horizontally
             */
            public float horizontalBias = 1.0f;
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
            /**
             * This is relative to {@link LeafDetails#leafBreadth}
             */
            private float leafThickness = 0.12f;
            /**
             * Makes the leaf start drooping starting from the {@link LeafDetails#maxBreadthPoint} * 1.134
             */
            private float droopFactor = 0.67f;
            /**
             * This makes the leaf exhibit "philodendronism". Basically splitting
             */
            private boolean compound = false;
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
            /**
             * This is relative to the final {@link LeafDetails#leafLength}
             */
            private float leafStalkLength = 0.24f;
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

            public Builder leafBreath(float val) {
                leafBreath = val;
                return this;
            }

            public Builder leafSpawningPoint(float val) {
                leafSpawningPoint = Math.max(0.0f, val);
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

            public Builder leafThickness(float val) {
                leafThickness = val;
                return this;
            }

            public Builder droopFactor(float val) {
                droopFactor = val;
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
