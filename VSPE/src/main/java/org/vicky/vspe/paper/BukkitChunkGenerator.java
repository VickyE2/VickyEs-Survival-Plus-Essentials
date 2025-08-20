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
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.ChunkGenerateContext;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.ChunkHeightProvider;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.PlatformChunkGenerator;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.PlatformDimension;

import java.util.*;

public class BukkitChunkGenerator extends ChunkGenerator implements PlatformChunkGenerator<BlockData, BukkitBiome> {

    private static final java.util.concurrent.ConcurrentMap<String, ChunkHeightProvider> heightProviderCache =
            new java.util.concurrent.ConcurrentHashMap<>();
    private final BukkitBiomeProvider biomeProvider;

    public BukkitChunkGenerator(PlatformDimension<BlockData, BukkitBiome> dimension) {
        this.biomeProvider = new BukkitBiomeProvider(dimension);
    }

    @Override
    public @NotNull org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.ChunkData generateChunk(@NotNull ChunkGenerateContext chunkGenerateContext) {

        return null;
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
    public @NotNull BukkitBiome getBiome(int i, int i1, int i2) {
        return biomeProvider.getBiomeProvider().resolveBiome(i, i1, i2, biomeProvider.getSeed());
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

        ChunkData chunkData = createChunkData(worldInfo);
        final int CHUNK_SIZE = 16;

        // 1) Determine biome per column and gather unique providers required for this chunk
        BukkitBiome[] biomeForColumn = new BukkitBiome[CHUNK_SIZE * CHUNK_SIZE];
        Set<ChunkHeightProvider> providersNeeded = new HashSet<>();

        for (int localZ = 0; localZ < CHUNK_SIZE; localZ++) {
            for (int localX = 0; localX < CHUNK_SIZE; localX++) {
                int worldX = chunkX * CHUNK_SIZE + localX;
                int worldZ = chunkZ * CHUNK_SIZE + localZ;

                // ask your biome resolver for this column (y can be a nominal value - resolver uses noise)
                // use the same seed you use everywhere for determinism
                BukkitBiome b = biomeProvider.resolveBiome(worldX, 64, worldZ, biomeProvider.getSeed());

                int idx = localX + localZ * CHUNK_SIZE;
                biomeForColumn[idx] = b;

                ChunkHeightProvider provider = getOrCreateProviderForBiome(b);
                providersNeeded.add(provider);
            }
        }

        // 2) For each provider needed, compute the heights for this chunk once
        Map<ChunkHeightProvider, int[]> heightsByProvider = new HashMap<>(providersNeeded.size());
        for (ChunkHeightProvider provider : providersNeeded) {
            // this calls the provider's chunk-level height generation and is expected to return
            // an int[] of size chunkSize*chunkSize with the same indexing (localX + localZ*chunkSize).
            int[] heights = provider.getChunkHeights(chunkX, chunkZ);
            heightsByProvider.put(provider, heights);
        }

        final var BEDROCK = Material.BEDROCK.createBlockData();

        int minY = chunkData.getMinHeight();
        int maxY = chunkData.getMaxHeight();
        int seaLevel = 63;

        for (int localZ = 0; localZ < CHUNK_SIZE; localZ++) {
            for (int localX = 0; localX < CHUNK_SIZE; localX++) {
                int idx = localX + localZ * CHUNK_SIZE;

                BukkitBiome b = biomeForColumn[idx];
                ChunkHeightProvider provider = getOrCreateProviderForBiome(b);
                int[] heights = heightsByProvider.get(provider);

                // defensive: if provider didn't return heights for some reason, fallback to sea level
                int topY = blendedTopY(localX, localZ, heights, biomeForColumn, heightsByProvider, CHUNK_SIZE);

                // clamp to allowed Y range for this ChunkData
                if (topY < minY) topY = minY;
                if (topY >= maxY) topY = maxY - 1;

                // Basic fill - bottom bedrock, stone up to top, simple filler & top block
                chunkData.setBlock(localX, minY, localZ, BEDROCK);
                for (int y = minY + 1; y <= topY; y++) {
                    chunkData.setBlock(localX, y, localZ, b.getDistributionPalette().getFor(y).getNative());
                }

                // Set the biome in BiomeGrid (convert your BukkitBiome -> org.bukkit.block.Biome)
                Biome nativeB = b.toNativeBiome().getBukkitBiome();
                biomeGrid.setBiome(localX, localZ, nativeB);
            }
        }

        // 4) optional: run features using deterministic RNG based on chunk + world seed

        return chunkData;
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
        // TODO: create with real noise layers and parameters from your BiomeParameters
        // Example (pseudocode):
        // List<Pair<NoiseSampler, Double>> layers = biome.getParameters().buildNoiseLayers();
        // return new ChunkHeightProvider(layers, someMaxHeight, ...);
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
}
