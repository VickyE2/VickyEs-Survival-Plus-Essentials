package org.vicky.vspe.viewer;

import org.vicky.platform.utils.Mirror;
import org.vicky.platform.utils.Rotation;
import org.vicky.platform.utils.Vec3;
import org.vicky.vspe.platform.systems.dimension.MushroomCapProfile;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.BezierCurve;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.CorePointsFactory;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.CurveFunctions;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators.Debug_PointTesterGenerator;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators.NoAIProceduralTreeGenerator;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators.ThesisTreeStructureGenerator;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators.thesis.ThesisBasedTreeGenerator;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.SpiralUtil;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.factories.HelixPointFactory;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.factories.LCurveFactory;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.factories.SCurveFactory;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.spiralutilsdecorators.NoodleSpline;
import org.vicky.vspe.platform.systems.dimension.TimeCurve;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators.parts.RealisticRose.realisticRoseTipMulti;
import static org.vicky.vspe.viewer.VoxelizerViewer.computeBounds;

public class Main {
    public static void main(String[] args) {
        ConcurrentHashMap<VoxelizerViewer.ChunkCoord, List<VoxelizerViewer.BlockPlacement<Object>>> map = new ConcurrentHashMap<>();
        List<VoxelizerViewer.BlockPlacement<Object>> list = new ArrayList<>();
        var treeL = new NoAIProceduralTreeGenerator.NoAIPTGBuilder<String>()
                .trunkWidth(10, 15)
                .trunkHeight(70, 120)
                .trunkType(NoAIProceduralTreeGenerator.TrunkType.TAPERED_SPINDLE)
                .branchType(NoAIProceduralTreeGenerator.BranchingType.UMBERELLA)
                .leafType(NoAIProceduralTreeGenerator.LeafPopulationType.HANGING)
                .realismLevel(0.7)
                .randomness(0.7)
                .spacing(2)
                .coniferousBranchRange(0, 10)
                .branchingPointRange(0.56, 1.0)
                .branchMaxDevianceAngle(7)
                .branchDepth(1)
                .leafPropagationChance(0.67)
                .vinePropagationChance(0.54)
                .branchPropagationChance(1.92)
                .branchSizeDecay(0.64)
                .maxBranchAmount(7)
                .maxVinePerBranch(30)
                .vineHeight(0.37)
                .tipDecoration(realisticRoseTipMulti(
                        SimpleBlockState.Companion.from("990033", (it) -> it),
                        SimpleBlockState.Companion.from("BB0033", (it) -> it),
                        SimpleBlockState.Companion.from("FF0033", (it) -> it),
                        2
                ))
                .vineSequenceMaterial(List.of(
                        SimpleBlockState.Companion.from("bb00EE", (it) -> it),
                        SimpleBlockState.Companion.from("bb00AA", (it) -> it),
                        SimpleBlockState.Companion.from("88009A", (it) -> it),
                        SimpleBlockState.Companion.from("440055", (it) -> it)
                ))
                .woodMaterial(SimpleBlockState.Companion.from("AA5500", (it) -> it))
                .leafMaterial(SimpleBlockState.Companion.from("44BB00", (it) -> it));
        var treeTh = new ThesisTreeStructureGenerator.Builder<String>()
                .trunkRadius(10, 19)
                .trunkHeight(7, 8)
                .treeAge(50)
                .placeLeaves(false)
                .growthData(new ThesisBasedTreeGenerator.GrowthData.Builder(6)
                        .senescenceAffectsChildren(true)
                        .distanceBetweenChildren(5)
                        .maxDepth(2)
                        .multiTrunkismMaxAmount(8)
                        .multiTrunkismAge(15)
                        .apicalControl(0.9f)
                        .addOverrides(
                                ThesisBasedTreeGenerator.Overrides.TrunkOverrides.MULTI_TRUNKISM
                        )
                        .build())
                .leafDetails(ThesisTreeStructureGenerator.LeafDetails.newBuilder()
                        .startIndex(1)
                        // .leafBreath(1.0f)
                        // .leafLength(1.5f)
                        // .leafSpawningPoint(0.5f)
                        .useRealisticType(true)
                        .realismPow(0.77)
                        .build())
                .seed(ByteBuffer.wrap(UUID.randomUUID().toString().getBytes()).getInt())
                .trunkMaterial(SimpleBlockState.Companion.from("AA5500", (it) -> it))
                .leafMaterial(SimpleBlockState.Companion.from("44BB00", (it) -> it));

        var tree = treeTh
                .build()
                .generate(
                        new SeededRandomSource(ByteBuffer.wrap(UUID.randomUUID().toString().getBytes()).getInt()),
                        new Vec3(0, 0, 0),
                        true
                );

        var debug = new Debug_PointTesterGenerator<>(
                () -> SpiralUtil.generateHelixAroundCurve(
                        BezierCurve.generatePoints(
                            CorePointsFactory.generate(
                                CorePointsFactory.Params.builder()
                                    .type(new HelixPointFactory(1, 1.0, 1.0))
                                    .height(160)
                                    .width(40)
                                    .build()
                            ), 200
                        ),
                        CurveFunctions.radius(1.0, 0.5, 0.0, 1.0, TimeCurve.QUADRATIC),
                        CurveFunctions.pitch(0.01, 0.05, 0.0, 1.0, TimeCurve.INVERTED_QUADRATIC),
                        (x) -> 0.0, 1, 0.8f,
                        NoodleSpline.newBuilder()
                                .setNoiseJitter(8)
                                .setNoiseSmoothness(8)
                                .setNoiseFreq(0.5)
                                .setMajorCircleRadius(50)
                                .setStrandRadius(3)
                                .setBundleSize(1)
                                .setMaxCircleCount(20)
                                .setMode(NoodleSpline.Mode.BRAID)
                                .setNoiseSeed(652035148)
                                .setBraidPhaseShift(0.0)
                                .setBraidPitch(0.0)
                                .setTwistsPerUnit(0.0)
                                .build(),
                        true, true
                ),
                SimpleBlockState.Companion.from("004466", (it) -> it)
        ).generate(
                new SeededRandomSource(ByteBuffer.wrap(UUID.randomUUID().toString().getBytes()).getInt()),
                new Vec3(0, 0, 0),
                false
        );

        int idx = 0;
        var highX = 0;
        var highY = 0;
        var highZ = 0;
        var lowX = 0;
        var lowY = 0;
        var lowZ = 0;
        for (var pos : tree.placements) {
            idx++;
            list.add(new VoxelizerViewer.BlockPlacement<>(pos.getX(), pos.getY(), pos.getZ(), pos.getState()));
            if (highX < pos.getX()) {
                highX = pos.getX();
            }
            if (highY < pos.getY()) {
                highY = pos.getY();
            }
            if (highZ < pos.getZ()) {
                highZ = pos.getZ();
            }

            if (lowX > pos.getX()) {
                lowX = pos.getX();
            }
            if (lowY > pos.getY()) {
                lowY = pos.getY();
            }
            if (lowZ > pos.getZ()) {
                lowZ = pos.getZ();
            }
        }
        System.out.println("size: " + idx);
        System.out.printf("%nlowX: %s lowY: %s lowZ: %s, highX: %s, highY %s, highZ: %s%n", lowX, lowY, lowZ, highX, highY, highZ);

        map.put(new VoxelizerViewer.ChunkCoord(0, 0), list);
        VoxelizerViewer.StructureBox bounds = computeBounds(new VoxelizerViewer.ResolvedStructure<>(map, null));
        VoxelizerViewer.SAMPLE = new VoxelizerViewer.ResolvedStructure<>(map, bounds);
        VoxelizerViewer.main(args);
    }

    public static void wasMain(String[] args) throws ExecutionException, InterruptedException {
        var treeL = new NoAIProceduralTreeGenerator.NoAIPTGBuilder<String>()
                .trunkWidth(10, 15)
                .trunkHeight(70, 110)
                .trunkType(NoAIProceduralTreeGenerator.TrunkType.TAPERED_SPINDLE)
                .branchType(NoAIProceduralTreeGenerator.BranchingType.UMBERELLA)
                .leafType(NoAIProceduralTreeGenerator.LeafPopulationType.NO_LEAVES)
                .randomness(0.8)
                .tipDecoration(realisticRoseTipMulti(
                        SimpleBlockState.Companion.from("990033", (it) -> it),
                        SimpleBlockState.Companion.from("BB0033", (it) -> it),
                        SimpleBlockState.Companion.from("FF0033", (it) -> it),
                        2
                ))
                .spacing(2)
                .vineHeight(0.45)
                .leafPropagationChance(0.67)
                .branchPropagationChance(0.95)
                .branchSizeDecay(0.95)
                .maxBranchAmount(7)
                .branchingPointRange(0.15, 0.95)
                .branchMaxDevianceAngle(7)
                .branchDepth(1)
                .slantAngleRange(-50, 50)
                .mushroomCapWidth(17, 20)
                .capProfile(MushroomCapProfile.SHARP_SNOUT)
                .vineSequenceMaterial(List.of(
                        SimpleBlockState.Companion.from("bb00EE", (it) -> it),
                        SimpleBlockState.Companion.from("bb00AA", (it) -> it),
                        SimpleBlockState.Companion.from("88009A", (it) -> it),
                        SimpleBlockState.Companion.from("440055", (it) -> it)
                ))
                .woodMaterial(SimpleBlockState.Companion.from("220022", (it) -> it))
                .leafMaterial(SimpleBlockState.Companion.from("FF00FF", (it) -> it));
        var structure = new ProceduralStructure<>(treeL);
        StructurePlacementContext context = new StructurePlacementContext(
                new SeededRandomSource(1234L),
                Rotation.NONE,
                Mirror.NONE
        );

        int threads = 8;
        int totalStructures = 4;
        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            List<Future<ResolvedStructure<String>>> tasks = new ArrayList<>();

            // Generate 1000 unique origins
            List<Vec3> origins = List.of(
                    Vec3.of(0, 0, 0),
                    Vec3.of(0, 0, 16),
                    Vec3.of(0, 0, 32),
                    Vec3.of(16, 0, 0),
                    Vec3.of(32, 0, 0),
                    Vec3.of(16, 0, 16),
                    Vec3.of(32, 0, 32),
                    Vec3.of(120, 0, 0),
                    Vec3.of(120, 0, 16),
                    Vec3.of(120, 0, 32)
            );

            long start = System.currentTimeMillis();

            for (Vec3 origin : origins) {
                tasks.add(executor.submit(() -> structure.resolve(origin, context)));
            }

            List<ResolvedStructure<String>> taskedes = new ArrayList<>();
            for (Future<ResolvedStructure<String>> task : tasks) {
                var tasked = task.get(); // wait for all to complete
                taskedes.add(tasked);
            }

            for (var tasked : taskedes) {
                System.out.printf(
                        """
                                Bounds:
                                    %s
                                """,
                        tasked.getBounds(),
                        tasked.getPlacementsByChunk().keySet()
                        //,tasked.getPlacementsByChunk().keySet()
                );
            }

            long elapsed = System.currentTimeMillis() - start;

            System.out.println("\n Resolved " + tasks.size() + " structures in " + elapsed + "ms");
            System.out.println("Cache size = " + structure.cacheSize());

            // Test many threads resolving the same origin to ensure single generation
            System.out.println("\nTesting repeated concurrent resolves of the same key...");
            Vec3 sameOrigin = new Vec3(100.0, 64.0, 100.0);
            AtomicInteger generateCount = structure.resetGenerationCounter();
            start = System.currentTimeMillis();

            tasks.clear();
            for (int i = 0; i < 100; i++) {
                tasks.add(executor.submit(() -> structure.resolve(sameOrigin, context)));
            }
            for (Future<ResolvedStructure<String>> task : tasks) task.get();
            elapsed = System.currentTimeMillis() - start;

            System.out.println("GenerateResolved() called " + generateCount.get() + " time(s) for same key, elapsed(ms): " + elapsed);
            executor.shutdown();
        }
    }
}

