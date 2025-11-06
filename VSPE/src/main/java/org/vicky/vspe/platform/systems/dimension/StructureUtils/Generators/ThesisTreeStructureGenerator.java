package org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators;

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

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;

/**
 * A ProceduralStructureGenerator subclass that uses ThesisBasedTreeGenerator to
 * create a tree structure and convert nodes into block placements.
 * <p>
 * The {@link Builder} only accepts primitives, enums and functional interfaces.
 */
public class ThesisTreeStructureGenerator<T> extends ProceduralStructureGenerator<T> {

    private final int treeAge;
    private final Pair<java.lang.Integer,
            java.lang.Integer> preTrunkHeight;
    private final Pair<java.lang.Double,
            java.lang.Double> preTrunkWidth;
    private final int trunkSegments;
    private final double spread; // generic quality/LOD parameter
    private final PlatformBlockState<T> trunkBlockMaterial;
    private final boolean placeLeaves;
    private final ThesisBasedTreeGenerator.GrowthData growthData;
    private final DoubleUnaryOperator thicknessFunction; // maps t in [0..1] to radius multiplier
    private final ThesisBasedTreeGenerator.GrowthEnvironment env;
    private final long localSeed;
    private final TimeCurve forTrunk = TimeCurve.INVERTED_QUADRATIC;
    private final TimeCurve forThickness = TimeCurve.INVERTED_QUADRATIC;
    private final TimeCurve forPitch = TimeCurve.EASE_IN_OUT_CUBIC;

    private ThesisTreeStructureGenerator(Builder<T> builder) {
        this.treeAge = builder.treeAge;
        this.preTrunkHeight = builder.trunkHeight;
        this.preTrunkWidth = builder.trunkWidth;
        this.trunkSegments = builder.trunkSegments;
        this.spread = builder.spread;
        this.trunkBlockMaterial = builder.trunkBlockMaterial;
        this.placeLeaves = builder.placeLeaves;
        this.growthData = builder.growthData;
        this.thicknessFunction = builder.thicknessFunction;
        this.env = builder.env != null ? builder.env : new ThesisBasedTreeGenerator.DefaultEnvironment();
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
        ThesisBasedTreeGenerator tb = new ThesisBasedTreeGenerator(gd, localSeed ^ rnd.nextLong());

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
        stampBranchRecursively(tb.getRoot(), trunkBlockMaterial);

        // Flush buffered placements into outPlacements/map by running the configured flush
        if (flush != null) flush.run();

        // The generate(...) wrapper will handle merging subtasks and returning the final GenerationResult
    }

    private int radiusForNode(ThesisBasedTreeGenerator.TreeNode node, int maxOrder, double baseWidth) {
        if (node == null) return 1;
        double t = (maxOrder <= 0) ? 0.0 : (node.order / (double) maxOrder);
        double multiplier = (thicknessFunction != null) ? thicknessFunction.applyAsDouble(t) : 1.0;
        double radius = baseWidth * multiplier;
        int r = Math.max(1, (int) Math.round(radius));
        return r;
    }

    // Stamp a 'fat' segment between p0 and p1 by placing overlapping spheres along the line.
    // stepSize controls spacing of spheres (smaller -> less gaps, but more placements).
    private void stampSegmentAsSpheres(Vec3 p0, Vec3 p1, int rStart, int rEnd, PlatformBlockState<T> state, double stepSize) {
        double dx = p1.x - p0.x;
        double dy = p1.y - p0.y;
        double dz = p1.z - p0.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist <= 0.0) {
            // fallback: single sphere at p0
            guardAndStore(p0.round().getIntX(), p0.round().getIntY(), p0.round().getIntZ(), Math.max(rStart, rEnd), state, true);
            return;
        }

        int steps = Math.max(1, (int) Math.ceil(dist / stepSize));
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3 p = p0.lerp(p1, t);
            // interpolate radius along the segment to avoid sudden jumps
            int r = Math.max(1, (int) Math.round(rStart * (1.0 - t) + rEnd * t));
            guardAndStore(p.round().getIntX(), p.round().getIntY(), p.round().getIntZ(), r, state, true);
        }
    }

    // Recursively traverse starting at node and stamp tubes for main and lateral children
    private void stampBranchRecursively(ThesisBasedTreeGenerator.TreeNode node, PlatformBlockState<T> state) {
        if (node == null || node.getControlPoints().size() < 2) return;

        // pick functions depending on order or node type
        Function<Double, Double> radiusFunction =
                CurveFunctions.multiFade(List.of(
                        new CurveFunctions.Segment(Math.max(1, node.baseRadius / 2.0), Math.max(1, node.baseRadius / 3.5), 0.0, 0.3, TimeCurve.INVERTED_QUADRATIC),
                        new CurveFunctions.Segment(Math.max(1, node.baseRadius / 3.5), node.baseRadius / 8.0, 0.3, 1.0, TimeCurve.QUADRATIC)
                ));

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
            guardAndStore(v, state, true);
        }

        // Recurse into children
        for (ThesisBasedTreeGenerator.TreeNode child : node.getChildren()) {
            stampBranchRecursively(child, state);
        }
    }

    // ---------------- Builder ----------------

    public static class Builder<T> extends BaseBuilder<T, ThesisTreeStructureGenerator<T>> {
        // required primitives/enums/functions only
        private int treeAge = 8;
        private Pair<Integer, Integer> trunkHeight = new Pair<>(8, 15);
        private Pair<Double, Double> trunkWidth = new Pair<>(1.5, 6.0);
        private int trunkSegments = 16;
        private double spread = 1.0;
        private PlatformBlockState<T> trunkBlockMaterial;
        private boolean placeLeaves = false;
        private ThesisBasedTreeGenerator.GrowthData growthData = null;
        private DoubleUnaryOperator thicknessFunction = t -> 1.0 - 0.6 * t; // default taper
        private ThesisBasedTreeGenerator.GrowthEnvironment env = null;
        private long seed = new Random().nextLong();
        protected LeafPopulationType leafType = LeafPopulationType.ON_BRANCH_TIP;

        public Builder<T> treeAge(int a) {
            this.treeAge = Math.max(1, a);
            return this;
        }

        public Builder<T> leafType(LeafPopulationType type) {
            this.leafType = type;
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

        public Builder<T> spread(double q) {
            this.spread = Math.max(0.1, q);
            return this;
        }

        public Builder<T> trunkMaterial(PlatformBlockState<T> id) {
            this.trunkBlockMaterial = Objects.requireNonNull(id);
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

        public Builder<T> environment(ThesisBasedTreeGenerator.GrowthEnvironment env) {
            this.env = env;
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
            if (thicknessFunction == null) throw new IllegalArgumentException("thicknessFunction required");
        }

        @Override
        protected ThesisTreeStructureGenerator<T> create() {
            // Build a generator instance with these primitive/config items
            return new ThesisTreeStructureGenerator<>(this);
        }
    }
}
