package org.vicky.vspe.nms.impl;

import com.mojang.serialization.Codec;
import de.pauleff.api.ICompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_20_R3.block.data.CraftBlockData;
import org.jetbrains.annotations.NotNull;
import org.vicky.platform.PlatformPlayer;
import org.vicky.platform.utils.Vec3;
import org.vicky.platform.world.PlatformBlock;
import org.vicky.platform.world.PlatformBlockState;
import org.vicky.platform.world.PlatformWorld;
import org.vicky.vspe.nms.BiomeCompatibilityAPI;
import org.vicky.vspe.nms.BiomeWrapper;
import org.vicky.vspe.paper.BukkitBiomeProvider;
import org.vicky.vspe.paper.BukkitBiome;
import org.vicky.vspe.platform.VSPEPlatformPlugin;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class NMSChunkGenerator extends ChunkGenerator {
    private static final WeightedStructurePlacer<BlockData> structurePlacer = new WeightedStructurePlacer<>();
    private static final Map<String, ChunkHeightProvider> heightProviderCache = new HashMap<>();
    private final long seed;

    public NMSChunkGenerator(NMSBiomeSource biomeProvider, long seed) {
        super(biomeProvider);
        this.seed = seed;
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
            BukkitBiome[] biomeForColumn = new BukkitBiome[16 * 16];
            Set<ChunkHeightProvider> providersNeeded = new HashSet<>();
            SeededRandomSource randomSource = new SeededRandomSource(seed);

            for (int lz = 0; lz < 16; lz++) {
                for (int lx = 0; lx < 16; lx++) {
                    int worldX = startX + lx;
                    int worldZ = startZ + lz;

                    BukkitBiome platformBiome = ((NMSBiomeSource) biomeSource).getBiomeProvider().resolveBiome(worldX, 64, worldZ, seed);
                    biomeForColumn[lx + lz * 16] = platformBiome;

                    ChunkHeightProvider provider = getOrCreateProviderForBiome(platformBiome);
                    providersNeeded.add(provider);
                }
            }

            // step 2: compute heights for each provider once
            Map<ChunkHeightProvider, int[]> heightsByProvider = new HashMap<>();
            for (ChunkHeightProvider provider : providersNeeded) {
                // chunk coords (pos.x,pos.z) — provider expects chunk indices (not block coords)
                heightsByProvider.put(provider, provider.getChunkHeights(pos.x, pos.z));
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

            // helper: blended top Y per column (index 0..255)
            java.util.function.IntFunction<Integer> blendedTopY = (idx) -> {
                BukkitBiome biome = biomeForColumn[idx];
                if (biome == null) return maxY - 1;

                ChunkHeightProvider provider = getOrCreateProviderForBiome(biome);
                int[] heights = heightsByProvider.get(provider);
                if (heights == null) return maxY - 1;

                return heights[idx];
            };

            final ChunkPos fPos = pos;
            final int fMinY = minY;
            final int fMaxY = maxY;
            final BukkitBiome[] fBiomeForColumn = biomeForColumn;
            final java.util.function.IntFunction<Integer> fBlendedTopY = blendedTopY;

            // step 3: fill blocks & apply NMS biome holders
            for (int lx = 0; lx < 16; lx++) {
                for (int lz = 0; lz < 16; lz++) {
                    int worldX = startX + lx;
                    int worldZ = startZ + lz;
                    int idx = lx + lz * 16;

                    BukkitBiome biome = biomeForColumn[idx];
                    if (biome == null) continue;

                    ChunkHeightProvider provider = getOrCreateProviderForBiome(biome);
                    int[] heights = heightsByProvider.get(provider);
                    if (heights == null) continue;

                    int topY = heights[idx];
                    if (topY < minY) topY = minY;
                    if (topY >= maxY) topY = maxY - 1;

                    // set bedrock at minY
                    chunk.setBlockState(new BlockPos(worldX, minY, worldZ), Blocks.BEDROCK.defaultBlockState(), false);

                    // fill column from minY+1..topY using palette
                    for (int y = minY + 1; y <= topY; y++) {
                        PlatformBlockState<BlockData> platformBlock = biome.getDistributionPalette().getFor(y);
                        BlockData bukkitData = platformBlock.getNative();
                        BlockState nmsState = ((CraftBlockData) bukkitData).getState();
                        chunk.setBlockState(new BlockPos(worldX, y, worldZ), nmsState, false);
                    }

                    // apply NMS biome holder across vertical column (so chunk stores your custom biome)
                    Holder<Biome> nmsBiome = BiomeCompatibilityAPI.Companion
                            .getBiomeCompatibility()
                            .getRegistry()
                            .getHolderOrThrow(biome.toNativeBiome().getResource());

                    // set biome for each y slice (some MC versions require this)
                    for (int y = minY; y < maxY; y++) {
                        // chunk#setBiome expects local coords 0..15 for x/z
                        chunk.setBiome(lx, y, lz, nmsBiome);
                    }
                }
            }

            // step 4: per-column features
            for (int lz = 0; lz < 16; lz++) {
                for (int lx = 0; lx < 16; lx++) {
                    int idx = lx + lz * 16;
                    BukkitBiome platformBiome = biomeForColumn[idx];
                    if (platformBiome == null) continue;

                    int topY = blendedTopY.apply(idx);

                    FeatureContext ctx = new FeatureContext(
                            seed,
                            pos.x,
                            pos.z,
                            randomSource.fork(seedForChunk.apply(0xF00D_1L)),
                            null, // you passed 'this' before; if features need a world object supply one
                            null
                    );

                    for (BiomeFeature<?, ?> feature : platformBiome.getFeatures()) {
                        if (feature.getPlacement() == FeaturePlacement.PER_COLUMN) {
                            if (feature.shouldPlace(
                                    pos.x * 16 + lx,
                                    topY,
                                    pos.z * 16 + lz,
                                    ctx
                            )) {
                                try {
                                    feature.place(
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

            // step 5: per-chunk random features (TODO: implement as needed)

            PlatformWorld<BlockData, Object> simpleWorld = new PlatformWorld<>() {
                @Override
                public String getName() {
                    return "vspe_nms_world";
                }

                @Override
                public Object getNative() {
                    return null;
                }

                @Override
                public int getHighestBlockYAt(double wx, double wz) {
                    // convert to world coords and lookup blended top Y for this chunk column
                    int xi = (int) Math.floor(wx);
                    int zi = (int) Math.floor(wz);
                    int localX = Math.floorMod(xi - fPos.getMinBlockX(), 16);
                    int localZ = Math.floorMod(zi - fPos.getMinBlockZ(), 16);
                    int idx = localX + localZ * 16;
                    if (idx < 0 || idx >= fBiomeForColumn.length) return fMinY;
                    return fBlendedTopY.apply(idx);
                }

                @Override
                public int getMaxWorldHeight() {
                    return fMaxY;
                }

                @Override
                public List<PlatformPlayer> getPlayers() {
                    return List.of();
                }

                @Override
                public PlatformBlock<BlockData> getBlockAt(double x, double y, double z) {
                    return null;
                }

                @Override
                public PlatformBlock<BlockData> getBlockAt(Vec3 vec3) {
                    return null;
                }

                @Override
                public PlatformBlockState<BlockData> getAirBlockState() {
                    return null;
                }

                @Override
                public PlatformBlockState<BlockData> getWaterBlockState() {
                    return null;
                }

                @Override
                public void setPlatformBlockState(Vec3 vec3, PlatformBlockState<BlockData> platformBlockState) {
                }

                @Override
                public void setPlatformBlockState(Vec3 vec3, PlatformBlockState<BlockData> platformBlockState, ICompoundTag tag) {
                }

                @Override
                public PlatformBlockState<BlockData> createPlatformBlockState(String domain, String id) {
                    return null;
                }

                @Override
                public void loadChunkIfNeeded(int cx, int cz) {
                }

                @Override
                public boolean isChunkLoaded(int cx, int cz) {
                    return true;
                }
            };

            // Minimal PlatformDimension implementation which supplies the simple world and a tiny generator stub.
            // Structure placer will be able to ask for the world & biome resolver.
            PlatformDimension<BlockData, BukkitBiome> simpleDimension = new PlatformDimension<>() {
                @NotNull
                @Override
                public String getId() {
                    return "vspe:temp_dimension";
                }

                @NotNull
                @Override
                public PlatformWorld<BlockData, ?> getWorld() {
                    return simpleWorld;
                }

                @NotNull
                @Override
                public PlatformChunkGenerator<BlockData, BukkitBiome> getChunkGenerator() {
                    // provide a tiny no-op chunk generator; structure placer generally only needs the chunk data adapter
                    return new PlatformChunkGenerator<>() {
                        @NotNull
                        @Override
                        public BukkitBiome getBiome(int i, int i1, int i2) {
                            return biomeForColumn[1 * i2];
                        }

                        @NotNull
                        @Override
                        public ChunkData<BlockData, BukkitBiome> generateChunk(@NotNull ChunkGenerateContext<BlockData, BukkitBiome> chunkGenerateContext) {
                            return null;
                        }
                    };
                }

                @NotNull
                @Override
                public BiomeResolver<BukkitBiome> getBiomeResolver() {
                    return ((NMSBiomeSource) biomeSource).getBiomeProvider();
                }

                @NotNull
                @Override
                public StructurePlacer<BlockData> getStructurePlacer() {
                    return new WeightedStructurePlacer<>();
                }

                @NotNull
                @Override
                public RandomSource getRandom() {
                    return randomSource;
                }
            };

            // Now create the chunk generate context using the simpleDimension and pass the adapter
            ChunkGenerateContext<BlockData, BukkitBiome> chunkGenerateContext =
                    new ChunkGenerateContext<>(pos.x, pos.z, simpleDimension);

            try {
                structurePlacer.placeStructuresInChunk(pos.x, pos.z, chunkGenerateContext, new ChunkData<BlockData, BukkitBiome>() {
                    @Override
                    public void setBiome(int x, int y, int z, @NotNull BukkitBiome bukkitBiome) {
                        BlockData bukkitData = bukkitBiome.getDistributionPalette().getFor(y).getNative();
                        BlockState block = ((CraftBlockData) bukkitData).getState();
                        chunk.setBlockState(new BlockPos(x, y, z), block, false);
                    }

                    @Override
                    public void setBlock(int x, int y, int z, @NotNull PlatformBlockState<BlockData> platformBlockState) {
                        BlockState block = ((CraftBlockData) platformBlockState.getNative()).getState();
                        chunk.setBlockState(new BlockPos(x, y, z), block, false);
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
        if (((NMSBiomeSource) biomeSource).getBiomeProvider() instanceof MultiParameterBiomeResolver<BukkitBiome> resolver)
            return (int) (resolver.getSeaLevel() * 319);
        return 53;
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
        BukkitBiome biome = ((NMSBiomeSource) biomeSource).getBiomeProvider().resolveBiome(x, 0, z, seed);
        ChunkHeightProvider provider = getOrCreateProviderForBiome(biome);
        int[] heights = provider.getChunkHeights(chunkX, chunkZ);

        int minY = getMinY();
        int maxY = getSeaLevel();
        int topY = minY;

        if (heights != null && idx >= 0 && idx < heights.length) {
            int candidate = heights[idx];
            if (candidate >= minY && candidate < maxY) {
                BlockData bukkitData = biome.getDistributionPalette().getFor(candidate).getNative();
                BlockState block = ((CraftBlockData) bukkitData).getState();
                if (heightmap.isOpaque().test(block)) {
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
        BukkitBiome biome = ((NMSBiomeSource) biomeSource).getBiomeProvider().resolveBiome(x, 0, z, seed);
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
                PlatformBlockState<BlockData> p = biome.getDistributionPalette().getFor(y);
                if (p != null) {
                    BlockData bd = p.getNative();
                    array[y - minY] = ((CraftBlockData) bd).getState();
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
    private ChunkHeightProvider getOrCreateProviderForBiome(BukkitBiome biome) {
        String key = biome.getIdentifier();
        return heightProviderCache.computeIfAbsent(key, k -> createProviderFromBiome(biome));
    }

    /**
     * Construct a ChunkHeightProvider for the given biome.
     * Implement this to build the provider using the biome's noise layers / parameters.
     */
    private ChunkHeightProvider createProviderFromBiome(BukkitBiome biome) {
        return new ChunkHeightProvider(
                biome.getHeightSampler().getLayers(),
                319,
                16,
                4,
                1.0,
                512,
                null
        );
    }

    /**
     * Helper that generates into a Bukkit ChunkData + BiomeGrid using our NMS-driven biome/height/palette logic.
     * This lets the Bukkit generator bridge to our NMS logic without the caller having to handle NMS objects.
     * <p>
     * Note: this runs on the server thread (getDefaultWorldGenerator -> generateChunkData) so avoid heavy blocking work.
     */
    public void generateToBukkitChunk(World bukkitWorld,
                                      org.bukkit.generator.ChunkGenerator.ChunkData bukkitChunkData,
                                      int chunkX, int chunkZ,
                                      org.bukkit.generator.ChunkGenerator.BiomeGrid biomeGrid) {

        final int CHUNK_SIZE = 16;
        long worldSeed = bukkitWorld.getSeed();

        // Set seed on biome provider if it expects it
        if (((NMSBiomeSource) this.biomeSource).getBiomeProvider() instanceof BukkitBiomeProvider bp) {
            bp.setSeed(worldSeed);
        }

        // Resolve biomes per-column and collect providers
        BukkitBiome[] biomeForColumn = new BukkitBiome[CHUNK_SIZE * CHUNK_SIZE];
        Set<ChunkHeightProvider> providersNeeded = new HashSet<>();
        for (int lz = 0; lz < CHUNK_SIZE; lz++) {
            for (int lx = 0; lx < CHUNK_SIZE; lx++) {
                int worldX = chunkX * CHUNK_SIZE + lx;
                int worldZ = chunkZ * CHUNK_SIZE + lz;

                BukkitBiome platformBiome = ((NMSBiomeSource) biomeSource).getBiomeProvider().resolveBiome(worldX, 64, worldZ, worldSeed);
                biomeForColumn[lx + lz * CHUNK_SIZE] = platformBiome;

                ChunkHeightProvider provider = getOrCreateProviderForBiome(platformBiome);
                providersNeeded.add(provider);
            }
        }

        // compute heights per provider
        Map<ChunkHeightProvider, int[]> heightsByProvider = new HashMap<>();
        for (ChunkHeightProvider provider : providersNeeded) {
            int[] heights = provider.getChunkHeights(chunkX, chunkZ);
            heightsByProvider.put(provider, heights);
        }

        // helper: blended top per column (index 0..255)
        java.util.function.IntFunction<Integer> blendedTopY = (idx) -> {
            BukkitBiome biome = biomeForColumn[idx];
            if (biome == null) return bukkitChunkData.getMaxHeight() - 1;
            ChunkHeightProvider provider = getOrCreateProviderForBiome(biome);
            int[] heights = heightsByProvider.get(provider);
            if (heights == null) return bukkitChunkData.getMaxHeight() - 1;
            return heights[idx];
        };

        int minY = bukkitChunkData.getMinHeight();
        int maxY = bukkitChunkData.getMaxHeight();

        // fill blocks & set biomes
        for (int lz = 0; lz < CHUNK_SIZE; lz++) {
            for (int lx = 0; lx < CHUNK_SIZE; lx++) {
                int idx = lx + lz * CHUNK_SIZE;
                BukkitBiome platformBiome = biomeForColumn[idx];
                if (platformBiome == null) continue;

                ChunkHeightProvider provider = getOrCreateProviderForBiome(platformBiome);
                int[] heights = heightsByProvider.get(provider);
                if (heights == null) continue;

                int topY = blendedTopY.apply(idx);
                if (topY < minY) topY = minY;
                if (topY >= maxY) topY = maxY - 1;

                // bedrock
                bukkitChunkData.setBlock(lx, minY, lz, org.bukkit.Material.BEDROCK.createBlockData());

                // fill column with palette (PlatformBlockState -> Bukkit BlockData)
                for (int y = minY + 1; y <= topY; y++) {
                    PlatformBlockState<org.bukkit.block.data.BlockData> platformBlock = platformBiome.getDistributionPalette().getFor(y);
                    org.bukkit.block.data.BlockData bstate = platformBlock.getNative();
                    bukkitChunkData.setBlock(lx, y, lz, bstate);
                }

                // set Bukkit-visible biome (safe enum) so Bukkit API callers don't break
                BiomeWrapper nativeBiome = platformBiome.toNativeBiome();
                biomeGrid.setBiome(lx, lz, nativeBiome.getBase());

                // Also set the NMS biome inside the chunk by using the wrapper's setBiome (which writes to NMS chunk)
                // We need a Block to pass to BiomeWrapper#setBiome — create a Location pointing at surface
                int worldX = chunkX * CHUNK_SIZE + lx;
                int worldZ = chunkZ * CHUNK_SIZE + lz;
                int worldY = topY; // surface
                try {
                    nativeBiome.setBiome(bukkitWorld.getBlockAt(worldX, worldY, worldZ));
                } catch (Exception ex) {
                    VSPEPlatformPlugin.platformLogger().warn("Failed to call nativeBiome.setBiome at " + worldX + "," + worldY + "," + worldZ + ": " + ex.getMessage());
                }
            }
        }

        // feature placement per-column (keeps your original pipeline)
        SeededRandomSource randomSource = new SeededRandomSource(worldSeed);
        java.util.function.LongFunction<Long> seedForChunk = (salt) -> {
            long h = worldSeed;
            h ^= (chunkX * 0x9E3779B97F4A7C15L);
            h ^= (chunkZ * 0xC6BC279692B5CC83L);
            h = Long.rotateLeft(h, 31);
            h ^= salt;
            return h;
        };

        for (int lz = 0; lz < CHUNK_SIZE; lz++) {
            for (int lx = 0; lx < CHUNK_SIZE; lx++) {
                int idx = lx + lz * CHUNK_SIZE;
                BukkitBiome platformBiome = biomeForColumn[idx];
                if (platformBiome == null) continue;

                int topY = blendedTopY.apply(idx);

                FeatureContext ctx = new FeatureContext(
                        worldSeed,
                        chunkX,
                        chunkZ,
                        randomSource.fork(seedForChunk.apply(0xF00D_1L)),
                        null, // if your features need a real world, pass one (avoid Bukkit main-thread calls here)
                        null
                );

                for (BiomeFeature<?, ?> feature : platformBiome.getFeatures()) {
                    if (feature.getPlacement() == FeaturePlacement.PER_COLUMN) {
                        if (feature.shouldPlace(chunkX * CHUNK_SIZE + lx, topY, chunkZ * CHUNK_SIZE + lz, ctx)) {
                            try {
                                feature.place(chunkX * CHUNK_SIZE + lx, topY, chunkZ * CHUNK_SIZE + lz, ctx);
                            } catch (Exception ex) {
                                VSPEPlatformPlugin.platformLogger().error("Feature placement error: " + ex.getMessage(), ex);
                            }
                        }
                    }
                }
            }
        }

        // NOTE: structure placement handled by your caller (or add here if you prefer)
    }
}
