package org.vicky.vspe.paper;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.data.BlockData;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;
import org.vicky.platform.world.PlatformBlockState;
import org.vicky.vspe.platform.VSPEPlatformPlugin;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.*;

import java.util.*;

public class BukkitChunkGenerator extends ChunkGenerator {
    private static final java.util.concurrent.ConcurrentMap<String, ChunkHeightProvider> heightProviderCache =
            new java.util.concurrent.ConcurrentHashMap<>();
    private final BukkitBiomeProvider biomeProvider;
    private static final WeightedStructurePlacer<BlockData> structurePlacer = new WeightedStructurePlacer<>();

    public BukkitChunkGenerator(PlatformDimension<BlockData, BukkitBiome> dimension) {
        this.biomeProvider = new BukkitBiomeProvider(dimension);
    }

    @Override
    public boolean shouldGenerateSurface() {
        return true;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return true;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return true;
    }

    @Override
    public @NotNull BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo worldInfo) {
        return biomeProvider;
    }

    @Override
    public @NotNull List<BlockPopulator> getDefaultPopulators(@NotNull World world) {
        return super.getDefaultPopulators(world);
    }


    @Override
    public @NotNull ChunkData generateChunkData(@NotNull World worldInfo,
                                                @NotNull Random random,
                                                int chunkX,
                                                int chunkZ,
                                                @NotNull BiomeGrid biomeGrid) {
        final int CHUNK_SIZE = 16;
        final long worldSeed = worldInfo.getSeed();
        ChunkGenerator.ChunkData bukkitChunkData = createChunkData(worldInfo);
        SeededRandomSource randomSource = new SeededRandomSource(worldInfo.getSeed());

        // --- 1) Resolve biomes for each column ---
        BukkitBiome[] biomeForColumn = new BukkitBiome[CHUNK_SIZE * CHUNK_SIZE];
        Set<ChunkHeightProvider> providersNeeded = new HashSet<>();

        for (int lz = 0; lz < CHUNK_SIZE; lz++) {
            for (int lx = 0; lx < CHUNK_SIZE; lx++) {
                int worldX = chunkX * CHUNK_SIZE + lx;
                int worldZ = chunkZ * CHUNK_SIZE + lz;

                BukkitBiome platformBiome = biomeProvider.resolveBiome(worldX, 64, worldZ, worldSeed);
                biomeForColumn[lx + lz * CHUNK_SIZE] = platformBiome;

                ChunkHeightProvider provider = getOrCreateProviderForBiome(platformBiome);
                providersNeeded.add(provider);
            }
        }

        // --- 2) Ask providers for chunk heights ---
        Map<ChunkHeightProvider, int[]> heightsByProvider = new HashMap<>();
        for (ChunkHeightProvider provider : providersNeeded) {
            int[] heights = provider.getChunkHeights(chunkX, chunkZ);
            heightsByProvider.put(provider, heights);
        }

        // --- Helper: chunk seed ---
        java.util.function.LongFunction<Long> seedForChunk = (salt) -> {
            long h = worldSeed;
            h ^= (chunkX * 0x9E3779B97F4A7C15L);
            h ^= (chunkZ * 0xC6BC279692B5CC83L);
            h = Long.rotateLeft(h, 31);
            h ^= salt;
            return h;
        };

        // --- Adapter for writing blocks/biomes into Bukkit's ChunkData ---
        ChunkDataAdapter adapterChunkData = new ChunkDataAdapter(bukkitChunkData, biomeGrid, chunkX, chunkZ);

        // --- Helper: top Y blending ---
        java.util.function.IntFunction<Integer> blendedTopY = (idx) -> {
            int lx = idx % CHUNK_SIZE;
            int lz = idx / CHUNK_SIZE;
            BukkitBiome biome = biomeForColumn[idx];
            if (biome == null) return worldInfo.getMaxHeight() - 1;

            ChunkHeightProvider provider = getOrCreateProviderForBiome(biome);
            int[] heights = heightsByProvider.get(provider);
            if (heights == null) return worldInfo.getMaxHeight() - 1;

            return heights[idx];
        };

        // --- 3) Fill chunk with base terrain ---
        BlockData bedrockState = Material.BEDROCK.createBlockData();
        int minY = bukkitChunkData.getMinHeight();
        int maxY = bukkitChunkData.getMaxHeight();

        for (int lz = 0; lz < CHUNK_SIZE; lz++) {
            for (int lx = 0; lx < CHUNK_SIZE; lx++) {
                int idx = lx + lz * CHUNK_SIZE;
                BukkitBiome platformBiome = biomeForColumn[idx];
                if (platformBiome == null) continue;

                ChunkHeightProvider provider = getOrCreateProviderForBiome(platformBiome);
                int[] heights = heightsByProvider.get(provider);

                int topY = blendedTopY.apply(idx);
                if (topY < minY) topY = minY;
                if (topY >= maxY) topY = maxY - 1;

                // Bedrock
                bukkitChunkData.setBlock(lx, minY, lz, bedrockState);

                // Fill with biome palette
                for (int y = minY + 1; y <= topY; y++) {
                    PlatformBlockState<BlockData> platformBlock = platformBiome.getDistributionPalette().getFor(y);
                    BlockData bstate = platformBlock.getNative();
                    bukkitChunkData.setBlock(lx, y, lz, bstate);
                }

                // Set biome
                Biome nativeBiome = platformBiome.toNativeBiome().getBukkitBiome();
                biomeGrid.setBiome(lx, lz, nativeBiome);
            }
        }

        // --- 4) Feature placement (per column) ---
        for (int lz = 0; lz < CHUNK_SIZE; lz++) {
            for (int lx = 0; lx < CHUNK_SIZE; lx++) {
                int idx = lx + lz * CHUNK_SIZE;
                BukkitBiome platformBiome = biomeForColumn[idx];
                if (platformBiome == null) continue;

                int[] heights = heightsByProvider.get(getOrCreateProviderForBiome(platformBiome));
                int topY = blendedTopY.apply(idx);

                FeatureContext ctx = new FeatureContext(
                        worldSeed,
                        chunkX,
                        chunkZ,
                        randomSource.fork(seedForChunk.apply(0xF00D_1L)),
                        biomeProvider.getDimension().getWorld(),
                        null
                );

                for (BiomeFeature<?, ?> feature : platformBiome.getFeatures()) {
                    if (feature.getPlacement() == FeaturePlacement.PER_COLUMN) {
                        if (feature.shouldPlace(
                                chunkX * CHUNK_SIZE + lx,
                                topY,
                                chunkZ * CHUNK_SIZE + lz,
                                ctx
                        )) {
                            feature.place(
                                    chunkX * CHUNK_SIZE + lx,
                                    topY,
                                    chunkZ * CHUNK_SIZE + lz,
                                    ctx
                            );
                        }
                    }
                }
            }
        }

        // --- 5) Per-chunk random features ---
        var perChunkRandom = randomSource.fork(seedForChunk.apply(0xDEAD_BEEFL));
        // TODO: loop through features with PER_CHUNK_RANDOM placement

        // --- 6) Structures ---
        ChunkGenerateContext<BlockData, BukkitBiome> chunkGenerateContext =
                new ChunkGenerateContext<>(chunkX, chunkZ, biomeProvider.getDimension());

        try {
            structurePlacer.placeStructuresInChunk(chunkX, chunkZ, chunkGenerateContext, adapterChunkData);
        } catch (Exception ex) {
            VSPEPlatformPlugin.platformLogger().error("Structure placement failed for chunk " + chunkX + "," + chunkZ + ": " + ex.getMessage(), ex);
        }

        return bukkitChunkData;
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

    private int blendedTopY(int localX, int localZ, int[] heights, BukkitBiome[] biomeForColumn, Map<ChunkHeightProvider, int[]> heightsByProvider, int CHUNK_SIZE) {
        int sum = 0, count = 0;
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                int nx = localX + dx;
                int nz = localZ + dz;
                if (nx < 0 || nx >= CHUNK_SIZE || nz < 0 || nz >= CHUNK_SIZE) continue;
                int nidx = nx + nz * CHUNK_SIZE;
                BukkitBiome nb = biomeForColumn[nidx];
                ChunkHeightProvider np = getOrCreateProviderForBiome(nb);
                int[] nheights = heightsByProvider.get(np);
                if (nheights == null || nheights.length <= nidx) continue;
                sum += nheights[nidx];
                count++;
            }
        }
        if (count == 0) return heights[localX + localZ * CHUNK_SIZE];
        int avg = sum / count;
        int orig = heights[localX + localZ * CHUNK_SIZE];
        // blend weight: keep most of original but smooth 40% toward neighbors
        return (int) Math.round(orig * 0.6 + avg * 0.4);
    }


    private long computeChunkSeed(long worldSeed, int chunkX, int chunkZ) {
        long h = worldSeed;
        h ^= (chunkX * 341873128712L + chunkZ * 132897987541L);
        h = h * 6364136223846793005L + 1442695040888963407L;
        return h;
    }

    private Random rndFrom(long s) {
        return new Random(s);
    }
}

// Adapter class to write to Bukkit ChunkData
class ChunkDataAdapter implements org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.ChunkData<BlockData, BukkitBiome> {
    private static final int CHUNK_SIZE = 16;
    private final ChunkGenerator.ChunkData delegate;
    private final ChunkGenerator.BiomeGrid biomeGrid;
    private final int chunkX, chunkZ;

    ChunkDataAdapter(ChunkGenerator.ChunkData delegate, ChunkGenerator.BiomeGrid biomeGrid, int chunkX, int chunkZ) {
        this.delegate = delegate;
        this.biomeGrid = biomeGrid;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    @Override
    public void setBlock(int x, int y, int z, @NotNull PlatformBlockState<BlockData> block) {
        int localX = x - chunkX * CHUNK_SIZE;
        int localZ = z - chunkZ * CHUNK_SIZE;
        if (localX < 0 || localX >= CHUNK_SIZE || localZ < 0 || localZ >= CHUNK_SIZE) return;

        BlockData bdata = block.getNative();
        delegate.setBlock(localX, y, localZ, bdata);
    }

    @Override
    public void setBiome(int x, int y, int z, BukkitBiome biome) {
        int localX = x - chunkX * CHUNK_SIZE;
        int localZ = z - chunkZ * CHUNK_SIZE;
        if (localX < 0 || localX >= CHUNK_SIZE || localZ < 0 || localZ >= CHUNK_SIZE) return;

        Biome nativeBiome = biome.toNativeBiome().getBukkitBiome();
        biomeGrid.setBiome(localX, localZ, nativeBiome);
    }
}