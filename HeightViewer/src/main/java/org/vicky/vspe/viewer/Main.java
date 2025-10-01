package org.vicky.vspe.viewer;

import org.vicky.platform.utils.Vec3;
import org.vicky.vspe.Direction;
import org.vicky.vspe.platform.systems.dimension.MushroomCapProfile;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators.NoAIProceduralTreeGenerator;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.SeededRandomSource;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.SimpleBlockState;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.vicky.vspe.viewer.VoxelizerViewer.computeBounds;

public class Main {
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
        var tree = new NoAIProceduralTreeGenerator.NoAIPTGBuilder<String>()
                .trunkWidth(10, 15)
                .trunkHeight(70, 110)
                .trunkType(NoAIProceduralTreeGenerator.TrunkType.CONIFEROUS)
                .branchType(NoAIProceduralTreeGenerator.BranchingType.CONIFEROUS)
                .leafType(NoAIProceduralTreeGenerator.LeafPopulationType.NO_LEAVES)
                .capProfile(MushroomCapProfile.GAUSSIAN)
                .maxMushDang(20)
                .mushroomCapWidth(20, 30)
                .mushroomCapHeight(3, 7)
                .vineSequenceMaterial(List.of(
                        SimpleBlockState.Companion.from("bb00EE", (it) -> it),
                        SimpleBlockState.Companion.from("bb00AA", (it) -> it),
                        SimpleBlockState.Companion.from("88009A", (it) -> it),
                        SimpleBlockState.Companion.from("440055", (it) -> it)
                ))
                .spacing(5)
                .leafPropagationChance(1.0)
                .branchPropagationChance(0.78)
                .windFlowDirection(Direction.NORTHEAST)
                .branchSizeDecay(0.75)
                .branchingPointRange(0.21, 0.90)
                .branchMaxDevianceAngle(5)
                .branchMaxHorizontalDevianceAngle(20)
                .branchDepth(2)
                .maxBranchAmount(5)
                .slantAngleRange(-50, 50)
                .branchVerticalDensity(2)
                .maxVinePerBranch(10)
                .coniferousBranchRange(2, 5)
                .woodMaterial(SimpleBlockState.Companion.from("220022", (it) -> it))
                .leafMaterial(SimpleBlockState.Companion.from("FF00FF", (it) -> it))
                .build()
                .generate(
                        new SeededRandomSource(ByteBuffer.wrap(UUID.randomUUID().toString().getBytes()).getInt()),
                        new Vec3(0, 0, 0)
                );

        int idx = 0;
        for (var pos : tree.placements) {
            idx++;
            list.add(new VoxelizerViewer.BlockPlacement<>(pos.getX(), pos.getY(), pos.getZ(), pos.getState()));
        }
        System.out.println("size: " + idx);
        map.put(new VoxelizerViewer.ChunkCoord(0, 0), list);
        VoxelizerViewer.StructureBox bounds = computeBounds(new VoxelizerViewer.ResolvedStructure<>(map, null));
        VoxelizerViewer.SAMPLE = new VoxelizerViewer.ResolvedStructure<>(map, bounds);
        VoxelizerViewer.main(args);
    }

    // Resize (bilinear) utility - up/down samples cleanly
    private static int[] resizeHeights(int[] src, int srcSize, int dstSize) {
        int[] out = new int[dstSize * dstSize];
        if (srcSize <= 1) {
            // degenerate: just fill
            int v = src.length > 0 ? src[0] : 0;
            for (int i = 0; i < out.length; i++) out[i] = v;
            return out;
        }

        for (int z = 0; z < dstSize; z++) {
            double gz = (z / (double) (dstSize - 1)) * (srcSize - 1);
            int iz = (int) Math.floor(gz);
            double tz = gz - iz;
            iz = Math.min(iz, srcSize - 2);

            for (int x = 0; x < dstSize; x++) {
                double gx = (x / (double) (dstSize - 1)) * (srcSize - 1);
                int ix = (int) Math.floor(gx);
                double tx = gx - ix;
                ix = Math.min(ix, srcSize - 2);

                int a = src[iz * srcSize + ix];
                int b = src[iz * srcSize + (ix + 1)];
                int c = src[(iz + 1) * srcSize + ix];
                int d = src[(iz + 1) * srcSize + (ix + 1)];

                double v = bilerp(a, b, c, d, tx, tz);
                out[z * dstSize + x] = (int) Math.round(v);
            }
        }
        return out;
    }

    private static double bilerp(double a, double b, double c, double d, double tx, double ty) {
        double ab = a + (b - a) * tx;
        double cd = c + (d - c) * tx;
        return ab + (cd - ab) * ty;
    }
}

