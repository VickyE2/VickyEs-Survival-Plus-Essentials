package org.vicky.vspe.forge.dimension;

import com.mojang.serialization.Codec;
import de.pauleff.api.ICompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vicky.platform.utils.ResourceLocation;
import org.vicky.platform.utils.Vec3;
import org.vicky.platform.world.PlatformBlockState;
import org.vicky.vspe.platform.VSPEPlatformPlugin;
import org.vicky.vspe.platform.systems.dimension.DimensionDescriptor;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class UnImpressedChunkGenerator extends ChunkGenerator {
    private static final WeightedStructurePlacer<BlockState> structurePlacer = new WeightedStructurePlacer<>();
    private static final Map<String, ChunkHeightProvider> heightProviderCache = new ConcurrentHashMap<>();
    private final long seed;
    private final DimensionDescriptor descriptor;

    public UnImpressedChunkGenerator(UnImpressedBiomeSource biomeProvider, long seed, DimensionDescriptor descriptor) {
        super(biomeProvider);
        this.seed = seed;
        this.descriptor = descriptor;
    }

    @Override
    protected @NotNull Codec<? extends ChunkGenerator> codec() {
        return ChunkGenerator.CODEC;
    }

    // We intentionally don't run vanilla carvers/surface building — we use our own pipeline
    @Override
    public void applyCarvers(@NotNull WorldGenRegion chunkRegion, long seed, @NotNull RandomState noiseConfig, @NotNull BiomeManager world,
                             @NotNull StructureManager structureAccessor, @NotNull ChunkAccess chunk, @NotNull GenerationStep.Carving carverStep) {
        // no-op (carving can be implemented later if desired)
    }

    @Override
    public void buildSurface(@NotNull WorldGenRegion region, @NotNull StructureManager structures, @NotNull RandomState noiseConfig,
                             @NotNull ChunkAccess chunk) {
        // no-op (we paint surface in fillFromNoise)
    }

    @Override
    public void applyBiomeDecoration(@NotNull WorldGenLevel world, @NotNull ChunkAccess chunk,
                                     @NotNull StructureManager structureAccessor) {
        // no-op - decoration may be handled by your feature placement pipeline
    }

    @Override
    public void spawnOriginalMobs(@NotNull WorldGenRegion region) {
        // no-op - spawn rules can be applied elsewhere if needed
    }

    @Override
    public int getGenDepth() {
        // sensible default (seaLevel - minY). Adjust if you prefer a different value.
        return getSeaLevel() - getMinY();
    }

    @NotNull
    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(@NotNull Executor executor, @NotNull Blender blender,
                                                        @NotNull RandomState noiseConfig,
                                                        @NotNull StructureManager structureAccessor, @NotNull ChunkAccess chunk) {
        return CompletableFuture.supplyAsync(() -> {
            ChunkPos pos = chunk.getPos();
            int startX = pos.getMinBlockX();
            int startZ = pos.getMinBlockZ();
            int minY = getMinY();
            int maxY = chunk.getHeight();

            // step 1: resolve biomes for each column and gather providers
            ForgeBiome[] biomeForColumn = new ForgeBiome[16 * 16];
            SeededRandomSource randomSource = new SeededRandomSource(seed);

            for (int lz = 0; lz < 16; lz++) {
                for (int lx = 0; lx < 16; lx++) {
                    int worldX = startX + lx;
                    int worldZ = startZ + lz;

                    ForgeBiome platformBiome = ((UnImpressedBiomeSource) biomeSource).getBiomeProvider().resolveBiome(worldX, 64, worldZ, seed);
                    biomeForColumn[lx + lz * 16] = platformBiome;
                }
            }

            // helper: chunk seed function
            java.util.function.LongFunction<Long> seedForChunk = (salt) -> {
                long h = seed;
                h ^= (pos.x * 0x9E3779B97F4A7C15L);
                h ^= (pos.z * 0xC6BC279692B5CC83L);
                h = Long.rotateLeft(h, 31);
                h ^= salt;
                return h;
            };

            int radius = 3; // tune (3..5)
            int paddedW = 16 + 2 * radius;
            ForgeBiome[] paddedBiomeGrid = precomputePaddedBiomeGrid(startX, startZ, seed, radius);

            int[] blendedHeights = computeBiomeAwareBlendedHeightsOptimized(pos.x, pos.z, startX, startZ, paddedBiomeGrid, radius);
            // Optional slope limiting
            blendedHeights = applySlopeLimit(blendedHeights, 2); // max vertical step 3

            // step 3: fill blocks & apply NMS biome holders
            for (int lx = 0; lx < 16; lx++) {
                for (int lz = 0; lz < 16; lz++) {
                    int worldX = startX + lx;
                    int worldZ = startZ + lz;
                    int idx = lx + lz * 16;

                    ForgeBiome biome = paddedBiomeGrid[(lx + radius) + (lz + radius) * paddedW];
                    if (biome == null) continue;
                    // Then in the per-column loop, instead of querying provider/ heights[idx], do:
                    int topY = blendedHeights[idx];
                    if (topY < minY) topY = minY;
                    if (topY >= maxY) topY = maxY - 1;

                    // set bedrock at minY
                    chunk.setBlockState(new BlockPos(worldX, minY, worldZ), Blocks.BEDROCK.defaultBlockState(), false);

                    // fill column from minY+1..topY using palette
                    for (int y = minY + 1; y <= topY; y++) {
                        PlatformBlockState<BlockState> platformBlock = biome.getDistributionPalette().getFor(y);
                        BlockState forgeData = platformBlock.getNative();
                        chunk.setBlockState(new BlockPos(worldX, y, worldZ), forgeData, false);
                    }

                    // set biome for each y slice (some MC versions require this)
                    for (int y = minY; y < maxY; y++) {
                        // chunk#setBiome expects local coords 0..15 for x/z
                        chunk.fillBiomesFromNoise(getBiomeSource(), Climate.empty());
                    }

                    int seaLevel = getSeaLevel();
                    if (topY < seaLevel - 1) {
                        for (int y = topY + 1; y < seaLevel; y++) {
                            BlockPos p = new BlockPos(worldX, y, worldZ);
                            if (chunk.getBlockState(p).isAir()) {
                                PlatformBlockState<?> water = descriptor.water();
                                if (water.getNative() instanceof BlockState forgeData) {
                                    chunk.setBlockState(p, forgeData, false);
                                }
                            }
                        }
                    }
                }
            }

            var placer = new BlockPlacer<BlockState>() {
                @Override
                public int getHighestBlockAt(int x, int z) {
                    int highest = minY;
                    for (int y = maxY - 1; y >= minY; y--) {
                        if (!chunk.getBlockState(new BlockPos(x, y, z)).isAir()) {
                            return y;
                        }
                    }
                    return highest;
                }

                @Override
                public void placeBlock(int x, int y, int z, @Nullable PlatformBlockState<BlockState> platformBlockState) {
                    if (platformBlockState == null) return;
                    chunk.setBlockState(new BlockPos(x, y, z), platformBlockState.getNative(), false);
                }

                @Override
                public void placeBlock(@NotNull Vec3 vec3, @Nullable PlatformBlockState<BlockState> platformBlockState) {
                    placeBlock((int) vec3.x, (int) vec3.y, (int) vec3.z, platformBlockState);
                }

                @Override
                public void placeBlock(@NotNull Vec3 vec3, @Nullable PlatformBlockState<BlockState> platformBlockState, @NotNull ICompoundTag tag) {
                    placeBlock(vec3, platformBlockState);
                }

                @Override
                public void placeBlock(int x, int y, int z, @Nullable PlatformBlockState<BlockState> platformBlockState, @NotNull ICompoundTag tag) {
                    placeBlock(x, y, z, platformBlockState);
                }
            };

            // step 4: per-column features
            for (int lz = 0; lz < 16; lz++) {
                for (int lx = 0; lx < 16; lx++) {
                    int idx = lx + lz * 16;
                    ForgeBiome platformBiome = biomeForColumn[idx];
                    if (platformBiome == null) continue;
                    int topY = blendedHeights[idx];

                    FeatureContext<BlockState> ctx = new FeatureContext<>(
                            seed,
                            pos.x,
                            pos.z,
                            randomSource.fork(seedForChunk.apply(0xF00D_1L)),
                            placer,
                            new NoiseSamplerProvider() {
                                @NotNull
                                @Override
                                public NoiseSampler getSampler(@NotNull ResourceLocation resourceLocation) {
                                    return new FBMGenerator(seed, 2, 0.03f, 0.003f, 0.7f, 2);
                                }
                            }
                    );

                    for (BiomeFeature<?> feature : platformBiome.getFeatures()) {
                        if (feature.getPlacement() == FeaturePlacement.PER_COLUMN) {
                            if (((BiomeFeature<BlockState>) feature).shouldPlace(
                                    pos.x * 16 + lx,
                                    topY,
                                    pos.z * 16 + lz,
                                    ctx
                            )) {
                                try {
                                    ((BiomeFeature<BlockState>) feature).place(
                                            pos.x * 16 + lx,
                                            topY,
                                            pos.z * 16 + lz,
                                            ctx
                                    );
                                } catch (Exception ex) {
                                    VSPEPlatformPlugin.platformLogger().error("Feature placement error at " + pos.x + "," + pos.z + ": " + ex.getMessage(), ex);
                                }
                            }
                        }
                    }
                }
            }

            // Now create the chunk generate context using the simpleDimension and pass the adapter
            ChunkGenerateContext<BlockState, ForgeBiome> chunkGenerateContext =
                    new ChunkGenerateContext<>(
                            pos.x, pos.z,
                            ((UnImpressedBiomeSource) biomeSource).getBiomeProvider(),
                            randomSource, placer,
                            Vec3::new
                    );

            try {
                structurePlacer.placeStructuresInChunk(pos.x, pos.z, chunkGenerateContext, new ChunkData<BlockState, ForgeBiome>() {
                    @Override
                    public void setBiome(int x, int y, int z, @NotNull ForgeBiome forgeBiome) {
                        chunk.fillBiomesFromNoise(biomeSource, Climate.empty());
                    }

                    @Override
                    public void setBlock(int x, int y, int z, @NotNull PlatformBlockState<BlockState> platformBlockState) {
                        chunk.setBlockState(new BlockPos(x, y, z), platformBlockState.getNative(), false);
                    }
                });
            } catch (Exception ex) {
                VSPEPlatformPlugin.platformLogger().error("Structure placement failed for chunk " + pos.x + "," + pos.z + ": " + ex.getMessage(), ex);
            }

            return chunk;
        }, executor);
    }

    @Override
    public int getSeaLevel() {
        if (((UnImpressedBiomeSource) biomeSource).getBiomeProvider() instanceof MultiParameterBiomeResolver<ForgeBiome> resolver)
            return (int) (resolver.getSeaLevel() * 319);
        return descriptor.oceanLevel();
    }

    @Override
    public int getMinY() {
        // fixed to -64 as you had previously
        return -64;
    }

    @Override
    public int getBaseHeight(int x, int z, @NotNull Heightmap.Types heightmap,
                             @NotNull LevelHeightAccessor world, @NotNull RandomState noiseConfig) {

        // determine chunk & index
        int chunkX = Math.floorDiv(x, 16);
        int chunkZ = Math.floorDiv(z, 16);
        int localX = Math.floorMod(x, 16);
        int localZ = Math.floorMod(z, 16);
        int idx = localX + localZ * 16;

        // Find the biome from our biome source
        ForgeBiome biome = ((UnImpressedBiomeSource) biomeSource).getBiomeProvider().resolveBiome(x, 0, z, seed);
        ChunkHeightProvider provider = getOrCreateProviderForBiome(biome);
        int[] heights = computeSmoothedHeights(provider, chunkX, chunkZ);

        int minY = getMinY();
        int maxY = getSeaLevel();
        int topY = minY;

        if (heights != null && idx >= 0 && idx < heights.length) {
            int candidate = heights[idx];
            if (candidate >= minY && candidate < maxY) {
                BlockState forgeData = biome.getDistributionPalette().getFor(localX, candidate, localZ, topY, getSeaLevel()).getNative();
                if (heightmap.isOpaque().test(forgeData)) {
                    topY = candidate;
                }
            }
        }

        return topY;
    }

    @Override
    public @NotNull NoiseColumn getBaseColumn(int x, int z, @NotNull LevelHeightAccessor world, @NotNull RandomState noiseConfig) {
        int minY = getMinY();
        int total = world.getHeight();
        BlockState[] array = new BlockState[total];
        Arrays.fill(array, Blocks.AIR.defaultBlockState());

        // compute topY from provider/palette
        ForgeBiome biome = ((UnImpressedBiomeSource) biomeSource).getBiomeProvider().resolveBiome(x, 0, z, seed);
        int chunkX = Math.floorDiv(x, 16);
        int chunkZ = Math.floorDiv(z, 16);
        ChunkHeightProvider provider = getOrCreateProviderForBiome(biome);
        int[] heights = provider.getChunkHeights(chunkX, chunkZ);
        int localX = Math.floorMod(x, 16);
        int localZ = Math.floorMod(z, 16);
        int idx = localX + localZ * 16;

        if (heights != null && idx >= 0 && idx < heights.length) {
            int topY = heights[idx];
            for (int y = minY; y <= topY && y < minY + total; y++) {
                PlatformBlockState<BlockState> p = biome.getDistributionPalette().getFor(localX, y, localZ, topY, getSeaLevel());
                if (p != null) {
                    BlockState bd = p.getNative();
                    array[y - minY] = bd;
                }
            }
        }

        return new NoiseColumn(minY, array);
    }

    @Override
    public void addDebugScreenInfo(@NotNull List<String> text, @NotNull RandomState noiseConfig, @NotNull BlockPos pos) {
        // optional debug info; implement as needed
    }

    /**
     * Return cached provider for this biome or create it lazily.
     * Keying here uses the biome's registry key (string) — replace with a better stable key if you have one.
     */
    private ChunkHeightProvider getOrCreateProviderForBiome(ForgeBiome biome) {
        String key = biome.getIdentifier();
        return heightProviderCache.computeIfAbsent(key, k -> createProviderFromBiome(biome));
    }

    /**
     * Construct a ChunkHeightProvider for the given biome.
     * Implement this to build the provider using the biome's noise layers / parameters.
     */
    private ChunkHeightProvider createProviderFromBiome(ForgeBiome biome) {
        return new ChunkHeightProvider(biome.getHeightSampler());
    }

    /**
     * Smooth a chunk's 16x16 height array by sampling a 3x3 neighborhood of chunks
     * and applying a small weighted kernel. Returns a new int[256].
     * <p>
     * provider.getChunkHeights(chunkX, chunkZ) must return int[256] (localX + localZ*16).
     */
    private int[] computeSmoothedHeights(ChunkHeightProvider provider, int chunkX, int chunkZ) {
        // kernel weights:
        // [1 2 1]
        // [2 4 2]
        // [1 2 1]  -> sum = 16
        final int[] weights = new int[]{
                1, 2, 1,
                2, 4, 2,
                1, 2, 1
        };

        // gather 3x3 neighbor heights (if provider returns null, substitute an all-minY array)
        int[][] neigh = new int[9][];
        int idx = 0;
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                int cx = chunkX + dx;
                int cz = chunkZ + dz;
                int[] arr = provider.getChunkHeights(cx, cz);
                if (arr == null) {
                    // fallback: fill with minY so missing neighbor won't produce extreme jumps
                    arr = new int[16 * 16];
                    Arrays.fill(arr, getMinY());
                }
                neigh[idx++] = arr;
            }
        }

        int[] out = new int[16 * 16];

        // for each local cell, combine same local coords from neighbors
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int cellIndex = lx + lz * 16;
                int sum = 0;
                for (int k = 0; k < 9; k++) {
                    int w = weights[k];
                    int[] a = neigh[k];
                    // using same local index in neighbor chunk (no sub-chunk offset)
                    sum += a[cellIndex] * w;
                }
                out[cellIndex] = Math.floorDiv(sum, 16); // normalize kernel sum
            }
        }

        return out;
    }

    /**
     * A simple heuristic mapping two biomes -> similarity weight [0..1].
     * Replace with a real tag/category check if you have one.
     */
    private double biomeSimilarityWeight(ForgeBiome a, ForgeBiome b) {
        if (a == null || b == null) return 0.0;

        // identical -> full weight
        if (a.getIdentifier().equals(b.getIdentifier())) return 1.0;

        String ida = a.getIdentifier().toLowerCase(Locale.ROOT);
        String idb = b.getIdentifier().toLowerCase(Locale.ROOT);

        // treat oceans / deep as very dissimilar to land (lower weight).
        boolean aOcean = ida.contains("ocean") || ida.contains("deep_ocean");
        boolean bOcean = idb.contains("ocean") || idb.contains("deep_ocean");

        if (aOcean && bOcean) return 1.0;       // both oceans -> blend well
        if (aOcean ^ bOcean) return 0.15;       // ocean <-> land -> low blend (allows beaches if combined with slope)
        // treat beaches/shores as a bridge
        if (ida.contains("beach") || idb.contains("beach")) return 0.9;

        // fallback: same family (e.g., "forest", "jungle") boost (substring heuristic)
        if (ida.split("/")[0].equals(idb.split("/")[0])) return 0.9;

        // default moderate blend
        return 0.6;
    }

    /**
     * Enforce a per-adjacent-column step limit to avoid single-column cliffs.
     * maxStep e.g. 3 or 4.
     */
    private int[] applySlopeLimit(int[] heights, int maxStep) {
        int W = 16, H = 16;
        int[] out = Arrays.copyOf(heights, heights.length);
        boolean changed = true;
        int iter = 0;
        int MAX_ITERS = 4;

        while (changed && iter++ < MAX_ITERS) {
            changed = false;
            for (int z = 0; z < H; z++) {
                for (int x = 0; x < W; x++) {
                    int idx = x + z * W;
                    int h = out[idx];

                    // 4-neighbors
                    if (x > 0) {
                        int nidx = (x - 1) + z * W;
                        int nh = out[nidx];
                        if (Math.abs(nh - h) > maxStep) {
                            if (nh > h) out[nidx] = h + maxStep;
                            else out[nidx] = h - maxStep;
                            changed = true;
                        }
                    }
                    if (x < W - 1) {
                        int nidx = (x + 1) + z * W;
                        int nh = out[nidx];
                        if (Math.abs(nh - h) > maxStep) {
                            if (nh > h) out[nidx] = h + maxStep;
                            else out[nidx] = h - maxStep;
                            changed = true;
                        }
                    }
                    if (z > 0) {
                        int nidx = x + (z - 1) * W;
                        int nh = out[nidx];
                        if (Math.abs(nh - h) > maxStep) {
                            if (nh > h) out[nidx] = h + maxStep;
                            else out[nidx] = h - maxStep;
                            changed = true;
                        }
                    }
                    if (z < H - 1) {
                        int nidx = x + (z + 1) * W;
                        int nh = out[nidx];
                        if (Math.abs(nh - h) > maxStep) {
                            if (nh > h) out[nidx] = h + maxStep;
                            else out[nidx] = h - maxStep;
                            changed = true;
                        }
                    }
                }
            }
        }
        return out;
    }

    /**
     * Precompute a padded biome array of size (16 + 2*radius)^2 for world coords:
     * x in [startX - radius .. startX + 15 + radius]
     * z in [startZ - radius .. startZ + 15 + radius]
     * <p>
     * Returned array indexing: (px, pz) -> index = (px) + (pz) * paddedWidth
     * where px=0..paddedW-1 corresponds to worldX = startX - radius + px.
     */
    private ForgeBiome[] precomputePaddedBiomeGrid(int startX, int startZ, long seed, int radius) {
        int paddedW = 16 + 2 * radius;
        ForgeBiome[] grid = new ForgeBiome[paddedW * paddedW];

        int baseX = startX - radius;
        int baseZ = startZ - radius;
        UnImpressedBiomeSource nmsSrc = (UnImpressedBiomeSource) biomeSource;
        for (int pz = 0; pz < paddedW; pz++) {
            for (int px = 0; px < paddedW; px++) {
                int worldX = baseX + px;
                int worldZ = baseZ + pz;
                ForgeBiome b = nmsSrc.getBiomeProvider().resolveBiome(worldX, 64, worldZ, seed);
                grid[px + pz * paddedW] = b;
            }
        }

        return grid;
    }

    /**
     * Optimized biome-aware blending that reads the paddedBiomeGrid (avoids repeated resolveBiome calls)
     * and caches per-(provider,chunk) chunkHeights arrays so getChunkHeights(...) is not repeatedly invoked.
     * <p>
     * chunkX,chunkZ are the target chunk indices (pos.x,pos.z).
     * startX,startZ are the minBlockX & minBlockZ of the chunk (pos.getMinBlockX/Z()).
     * <p>
     * paddedBiomeGrid must be the precomputePaddedBiomeGrid result for the same radius.
     */
    private int[] computeBiomeAwareBlendedHeightsOptimized(int chunkX, int chunkZ, int startX, int startZ,
                                                           ForgeBiome[] paddedBiomeGrid, int radius) {
        final int W = 16, H = 16;
        int paddedW = 16 + 2 * radius;
        int[] out = new int[W * H];

        double sigma = Math.max(1.0, radius / 2.0);
        int size = radius * 2 + 1;
        double[] distWeights = new double[size * size];
        for (int dz = -radius, i = 0; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++, i++) {
                double d2 = dx * dx + dz * dz;
                distWeights[i] = Math.exp(-d2 / (2.0 * sigma * sigma));
            }
        }

        // cache chunk heights per provider -> map(chunkKey -> int[])
        Map<ChunkHeightProvider, Map<Long, int[]>> heightsCache = new HashMap<>();

        int baseWorldX = startX - radius;
        int baseWorldZ = startZ - radius;

        for (int lz = 0; lz < H; lz++) {
            for (int lx = 0; lx < W; lx++) {
                int cellIdx = lx + lz * W;
                int worldX = startX + lx;
                int worldZ = startZ + lz;
                ForgeBiome centerBiome = paddedBiomeGrid[(lx + radius) + (lz + radius) * paddedW];

                double sum = 0.0;
                double weightSum = 0.0;
                int kIdx = 0;

                for (int sdz = -radius; sdz <= radius; sdz++) {
                    for (int sdx = -radius; sdx <= radius; sdx++, kIdx++) {
                        double distW = distWeights[kIdx];

                        int sampleWorldX = worldX + sdx;
                        int sampleWorldZ = worldZ + sdz;

                        int sampleLocalX = (sampleWorldX - baseWorldX); // absolute in padded grid
                        int sampleLocalZ = (sampleWorldZ - baseWorldZ);
                        if (sampleLocalX < 0 || sampleLocalX >= paddedW || sampleLocalZ < 0 || sampleLocalZ >= paddedW) {
                            continue; // out of padded grid (shouldn't normally happen)
                        }

                        ForgeBiome sampleBiome = paddedBiomeGrid[sampleLocalX + sampleLocalZ * paddedW];

                        // compute which chunk contains the sample (use floorDiv / floorMod consistently)
                        int sampleChunkX = Math.floorDiv(sampleWorldX, 16);
                        int sampleChunkZ = Math.floorDiv(sampleWorldZ, 16);
                        int sampleLocalXInChunk = Math.floorMod(sampleWorldX, 16);
                        int sampleLocalZInChunk = Math.floorMod(sampleWorldZ, 16);

                        // defensive sanity-check
                        if (sampleLocalXInChunk < 0 || sampleLocalXInChunk >= 16 || sampleLocalZInChunk < 0 || sampleLocalZInChunk >= 16) {
                            VSPEPlatformPlugin.platformLogger().warn(String.format("Invalid local-in-chunk coords: sampleWorld=(%s,%s), sampleLocalInChunk=(%s,%s), padded indices=(%s,%s), paddedW=%s",
                                    sampleWorldX, sampleWorldZ, sampleLocalXInChunk, sampleLocalZInChunk, sampleLocalX, sampleLocalZ, paddedW));
                            continue;
                        }

                        int sampleIdx = sampleLocalXInChunk + sampleLocalZInChunk * 16;

                        // provider for the sample biome
                        ChunkHeightProvider sampleProvider = getOrCreateProviderForBiome(sampleBiome);

                        // provider -> inner map
                        Map<Long, int[]> providerMap = heightsCache.computeIfAbsent(sampleProvider, k -> new HashMap<>());

                        // safer chunkKey composition (avoid ^ which may collide)
                        long chunkKey = (((long) sampleChunkX) << 32) | (sampleChunkZ & 0xffffffffL);

                        int[] sampleHeights = providerMap.get(chunkKey);
                        if (sampleHeights == null) {
                            sampleHeights = sampleProvider.getChunkHeights(sampleChunkX, sampleChunkZ);
                            if (sampleHeights == null) {
                                sampleHeights = new int[16 * 16];
                                Arrays.fill(sampleHeights, getMinY());
                            }
                            providerMap.put(chunkKey, sampleHeights);
                        }

                        // final defensive check before indexing
                        if (sampleIdx < 0 || sampleIdx >= sampleHeights.length) {
                            VSPEPlatformPlugin.platformLogger().error(String.format("Sample index out of range when blending heights. sampleIdx=%s, sampleHeights.length=%s, sampleChunk=(%s,%s), sampleLocalInChunk=(%s,%s), sampleWorld=(%s,%s), chunkX=%s, chunkZ=%s",
                                    sampleIdx, (sampleHeights == null ? -1 : sampleHeights.length),
                                    sampleChunkX, sampleChunkZ, sampleLocalXInChunk, sampleLocalZInChunk, sampleWorldX, sampleWorldZ, chunkX, chunkZ));
                            // fallback: skip this sample contribution (safer than throwing)
                            continue;
                        }

                        int sampleH = sampleHeights[sampleIdx];

                        double biomeW = biomeSimilarityWeight(centerBiome, sampleBiome);
                        double w = distW * biomeW;
                        sum += sampleH * w;
                        weightSum += w;
                    }
                }

                if (weightSum <= 0.0) {
                    ChunkHeightProvider localProvider = getOrCreateProviderForBiome(centerBiome);
                    int[] localHeights = localProvider.getChunkHeights(chunkX, chunkZ);
                    out[cellIdx] = (localHeights != null && cellIdx >= 0 && cellIdx < localHeights.length) ? localHeights[cellIdx] : getMinY();
                } else {
                    out[cellIdx] = (int) Math.round(sum / weightSum);
                }
            }
        }

        return out;
    }
}