package org.vicky.vspe.viewer;

import org.vicky.platform.utils.Mirror;
import org.vicky.platform.utils.Rotation;
import org.vicky.platform.utils.Vec3;
import org.vicky.vspe.platform.systems.dimension.MushroomCapProfile;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.CurveFunctions;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators.NoAIProceduralTreeGenerator;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators.ThesisTreeStructureGenerator;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators.thesis.ThesisBasedTreeGenerator;
import org.vicky.vspe.platform.systems.dimension.TimeCurve;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.*;
import org.vicky.vspe.platform.utilities.FunctionCurveConverter;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.vicky.vspe.branch.BranchKt.generateLeafBlob;
import static org.vicky.vspe.branch.BranchKt.generateLeafBlobWithChildren;
import static org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators.parts.RealisticRose.realisticRoseTipMulti;
import static org.vicky.vspe.viewer.VoxelizerViewer.computeBounds;

public class Main {

    private static String formatDuration(long nanos) {
        double seconds = (double) nanos / 1_000_000_000.0;
        return String.format("%.6f s", seconds);
    }

    public static void main(String[] args) {
        var fade = CurveFunctions
                .multiFade(
                        new CurveFunctions.Segment(
                                1.0, 0.8, 0.0, 0.42, TimeCurve.INVERTED_CUBIC
                        ),
                        new CurveFunctions.Segment(
                                0.8, 0.5, 0.42, 0.83, TimeCurve.QUADRATIC
                        ),
                        new CurveFunctions.Segment(
                                0.5, 0.4, 0.83, 1.0, TimeCurve.EXPONENTIAL_OUT
                        )
                );

        ConcurrentHashMap<VoxelizerViewer.ChunkCoord, List<VoxelizerViewer.BlockPlacement<Object>>> map = new ConcurrentHashMap<>();
        List<VoxelizerViewer.BlockPlacement<Object>> list = new ArrayList<>();
        var treeTh = new ThesisTreeStructureGenerator.Builder<String>()
                .trunkRadius(4, 6)
                .trunkHeight(70, 120)
                .treeAge(170)
                .placeLeaves(true)
                .growthData(new ThesisBasedTreeGenerator.GrowthData.Builder(2.1f)
                        .senescenceAffectsChildren(true)
                        .senescenceStartPercentage(0.68)
                        .senescenceBudPenalty(0.7)   // or 1.2–1.5 for very strong droop
                        .senescenceDecayRate(0.03)
                        .senescenceVigorPenalty(0.55)
                        .senescenceGravBias(1.8)
                        .maxDepth(1)
                        .maxKids(-1)
                        .spread(1.3f)
                        .influenceRadius(-1)
                        .killRadius(30)
                        .trunkGrowthMaxAge(80)
                        .attractorClumping(1.0)
                        .distanceBetweenChildren(10)
                        .pruningHeight(0.5f)
                        .addOverrides(ThesisBasedTreeGenerator.Overrides.BranchOverrides.MIRROR_BRANCHES)
                        .build())
                .leafDetails(ThesisTreeStructureGenerator.LeafDetails.newBuilder()
                        .realismPow(0.54)
                        .startIndex(0)
                        .leafBreath(0.0f)
                        .leafLength(0.34f)
                        .leafSpawningPoint(0.2f)
                        .leafSpawningPointEnd(1.0f)
                        .leafThickness(0.0f)
                        .leafSpacing(0.005f)
                        .droopFactor(1.0f)
                        .droopStart(0.0f)
                        .droopMode(ThesisTreeStructureGenerator.LeafDroopMode.STRAIGHT_DOWN)
                        .layerCount(3)
                        .heightReduction(false)
                        .heightReductionCurve(TimeCurve.EXPONENTIAL_OUT)
                        .leafType(ThesisTreeStructureGenerator.NodeLeafingType.OPPOSITE)
                        .shrinkFactor(TimeCurve.LINEAR)
                        .leafDirection(t -> -10)
                        .build())
                .trunkMaterial(SimpleBlockState.Companion.from("AA5500", (it) -> it))
                .leafMaterial(SimpleBlockState.Companion.from("44BB00", (it) -> it));

        var tree = treeTh
                .build();

        // Use same seed for both runs
        int seed = ByteBuffer.wrap(UUID.randomUUID().toString().getBytes()).getInt();
        Vec3 origin = new Vec3(0, 0, 0);

        // Timing for dry run (true)
        long startTimeDryRun = System.nanoTime();
        long dryRunTime = System.nanoTime() - startTimeDryRun;

        // Timing for actual generation (false)
        long startTimeActual = System.nanoTime();
        var generated = tree.generate(
                new SeededRandomSource(seed),
                origin.add(100, 0, 100),
                false
        );
        long actualTime = System.nanoTime() - startTimeActual;

        // Output formatted results
        System.out.printf("┌─────────────────────────────────┐%n");
        System.out.printf("│  Generation Timing Results     │%n");
        System.out.printf("├─────────────────────────────────┤%n");
        System.out.printf("│ Dry run (false):   %13s │%n", formatDuration(dryRunTime));
        System.out.printf("│ Actual (true):   %13s │%n", formatDuration(actualTime));
        System.out.printf("│ Difference:       %13s │%n", formatDuration(Math.abs(actualTime - dryRunTime)));
        System.out.printf("│ Blocks generated: %13d │%n", generated.placements.size());
        System.out.printf("└─────────────────────────────────┘%n");

        int idx = 0;
        var highX = 0;
        var highY = 0;
        var highZ = 0;
        var lowX = 0;
        var lowY = 0;
        var lowZ = 0;

        /*
        var points = FunctionCurveConverter.toCurve(fade, 0.0, 100.0, 100, 0.0);
        var rnd = new Random(seed);
        var blob = generateLeafBlobWithChildren(
                origin,
                rnd.nextInt(25, 30),
                rnd.nextInt(10, 15),
                rnd.nextInt(12, 16),
                1.0,
                rnd
        );

        for (var attr : blob) {
            idx++;
            var pos = attr;
            list.add(new VoxelizerViewer.BlockPlacement<>(
                    (int) pos.getX(), (int) pos.getY(), (int) pos.getZ(),
                    SimpleBlockState.Companion.from("AA00AA", (it) -> it)));
            if (highX < pos.getX()) {
                highX = (int) pos.getX();
            }
            if (highY < pos.getY()) {
                highY = (int) pos.getY();
            }
            if (highZ < pos.getZ()) {
                highZ = (int) pos.getZ();
            }

            if (lowX > pos.getX()) {
                lowX = (int) pos.getX();
            }
            if (lowY > pos.getY()) {
                lowY = (int) pos.getY();
            }
            if (lowZ > pos.getZ()) {
                lowZ = (int) pos.getZ();
            }
        }*/
        // new ArrayList<BlockPlacement<String>>()

        for (var pos : generated.placements) {
            idx++;
            list.add(new VoxelizerViewer.BlockPlacement<>(
                    pos.getX(), pos.getY(), pos.getZ(),
                    pos.getState()));
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

        /*for(var pos : points) {
            idx++;
            list.add(new VoxelizerViewer.BlockPlacement<>(
                    (int) pos.getX(), (int) pos.getY(), (int) pos.getZ(),
                    SimpleBlockState.Companion.from("AA00AA", (it) -> it)));
        }*/

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

