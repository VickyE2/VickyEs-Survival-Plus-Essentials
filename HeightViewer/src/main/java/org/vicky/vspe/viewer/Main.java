package org.vicky.vspe.viewer;

import org.vicky.platform.utils.Vec3;
import org.vicky.vspe.platform.systems.dimension.MushroomCapProfile;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators.NoAIProceduralTreeGenerator;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.SeededRandomSource;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.SimpleBlockState;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators.parts.RealisticRose.realisticRoseTipMulti;
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
                .leafMaterial(SimpleBlockState.Companion.from("FF00FF", (it) -> it))
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

