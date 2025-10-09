package org.vicky.vspe.viewer;

import org.vicky.platform.utils.Mirror;
import org.vicky.platform.utils.Rotation;
import org.vicky.platform.utils.Vec3;
import org.vicky.vspe.platform.systems.dimension.MushroomCapProfile;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators.NoAIProceduralTreeGenerator;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators.parts.RealisticRose.realisticRoseTipMulti;

public class Main {
    /*
    public static void main(String[] args) {

        // var mapper = new ChunkHeightProvider(BiomeResolvers.BiomeDetailHolder.MAGENTA_FOREST.getHeightSampler());
        // Viewer expects a HeightProvider: (chunkX, chunkZ, size) -> int[]
        // ChunkHeightViewer.PROVIDER = (chunkX, chunkZ, size) -> {
        //     int[] base = mapper.getChunkHeights(chunkX, chunkZ); // e.g. returns 16*16
        //     if (base == null) return null;
        //     int baseSize = (int) Math.round(Math.sqrt(base.length));
        //     if (baseSize == size) return base;
        //     // bilinear up/down-sample
        //     return resizeHeights(base, baseSize, size);
        // };

        // ChunkHeightViewer.main(args);
        ConcurrentHashMap<VoxelizerViewer.ChunkCoord, List<VoxelizerViewer.BlockPlacement<Object>>> map = new ConcurrentHashMap<>();
        List<VoxelizerViewer.BlockPlacement<Object>> list = new ArrayList<>();
        var treeL = new NoAIProceduralTreeGenerator.NoAIPTGBuilder<String>()
                .trunkWidth(10, 15)
                .trunkHeight(70, 110)
                .trunkType(NoAIProceduralTreeGenerator.TrunkType.TAPERED_SPINDLE)
                .branchType(NoAIProceduralTreeGenerator.BranchingType.TAPERED_SPINDLE)
                .leafType(NoAIProceduralTreeGenerator.LeafPopulationType.ON_BRANCH_TIP)
                .randomness(0.8)
                .tipDecoration(realisticRoseTipMulti(
                        SimpleBlockState.Companion.from("990033", (it) -> it),
                        SimpleBlockState.Companion.from("BB0033", (it) -> it),
                        SimpleBlockState.Companion.from("FF0033", (it) -> it),
                        2
                ))
                .spacing(5)
                .vineHeight(0.45)
                .leafPropagationChance(0.67)
                .branchPropagationChance(0.78)
                .branchSizeDecay(0.95)
                .maxBranchAmount(7)
                .branchingPointRange(0.35, 0.80)
                .branchMaxDevianceAngle(7)
                .branchMaxHorizontalDevianceAngle(20)
                .branchDepth(2)
                .slantAngleRange(-50, 50)
                .mushroomCapWidth(17, 20)
                .capProfile(MushroomCapProfile.SHARP_SNOUT)
                .branchVerticalDensity(2)
                .vineSequenceMaterial(List.of(
                        SimpleBlockState.Companion.from("bb00EE", (it) -> it),
                        SimpleBlockState.Companion.from("bb00AA", (it) -> it),
                        SimpleBlockState.Companion.from("88009A", (it) -> it),
                        SimpleBlockState.Companion.from("440055", (it) -> it)
                ))
                .woodMaterial(SimpleBlockState.Companion.from("220022", (it) -> it))
                .leafMaterial(SimpleBlockState.Companion.from("FF00FF", (it) -> it));
        new ProceduralStructure<>(treeL);

        var tree = treeL
                .build()
                .generate(
                        new SeededRandomSource(ByteBuffer.wrap(UUID.randomUUID().toString().getBytes()).getInt()),
                        new Vec3(-700, 0, 700)
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
     */

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        var treeL = new NoAIProceduralTreeGenerator.NoAIPTGBuilder<String>()
                .trunkWidth(10, 15)
                .trunkHeight(70, 110)
                .trunkType(NoAIProceduralTreeGenerator.TrunkType.TAPERED_SPINDLE)
                .branchType(NoAIProceduralTreeGenerator.BranchingType.TAPERED_SPINDLE)
                .leafType(NoAIProceduralTreeGenerator.LeafPopulationType.ON_BRANCH_TIP)
                .randomness(0.8)
                .tipDecoration(realisticRoseTipMulti(
                        SimpleBlockState.Companion.from("990033", (it) -> it),
                        SimpleBlockState.Companion.from("BB0033", (it) -> it),
                        SimpleBlockState.Companion.from("FF0033", (it) -> it),
                        2
                ))
                .spacing(5)
                .vineHeight(0.45)
                .leafPropagationChance(0.67)
                .branchPropagationChance(0.78)
                .branchSizeDecay(0.95)
                .maxBranchAmount(7)
                .branchingPointRange(0.35, 0.80)
                .branchMaxDevianceAngle(7)
                .branchMaxHorizontalDevianceAngle(20)
                .branchDepth(2)
                .slantAngleRange(-50, 50)
                .mushroomCapWidth(17, 20)
                .capProfile(MushroomCapProfile.SHARP_SNOUT)
                .branchVerticalDensity(2)
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

