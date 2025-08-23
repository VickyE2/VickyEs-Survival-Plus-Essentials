package org.vicky.vspe.viewer;

import org.vicky.vspe.platform.systems.dimension.globalDimensions.BiomeResolvers;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.ChunkHeightProvider;

public class Main {
    public static void main(String[] args) {

        var mapper = new ChunkHeightProvider(BiomeResolvers.BiomeDetailHolder.MAGENTA_FOREST.getHeightSampler());
        // Viewer expects a HeightProvider: (chunkX, chunkZ, size) -> int[]
        ChunkHeightViewer.PROVIDER = (chunkX, chunkZ, size) -> {
            int[] base = mapper.getChunkHeights(chunkX, chunkZ); // e.g. returns 16*16
            if (base == null) return null;

            int baseSize = (int) Math.round(Math.sqrt(base.length));
            if (baseSize == size) return base;

            // bilinear up/down-sample
            return resizeHeights(base, baseSize, size);
        };

        ChunkHeightViewer.main(args);
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

