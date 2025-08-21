package org.vicky.vspe.paper;

import org.bukkit.World;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vicky.vspe.nms.impl.NMSChunkGenerator;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.BiomeResolver;

import java.util.List;

/**
 * Terra-like Bukkit wrapper that hands chunk generation off to an NMSChunkGenerator.
 * <p>
 * - Keeps a Bukkit-facing BiomeProvider so other plugins see a safe enum.
 * - Delegates heavy work to the NMSChunkGenerator (which should implement generateToBukkitChunk()).
 */
public class BukkitChunkGeneratorWrapper extends ChunkGenerator {
    private final BiomeResolver<BukkitBiome> resolver;

    /**
     * Construct wrapper from a resolver + seed (creates the NMS generator)
     */
    public BukkitChunkGeneratorWrapper(BiomeResolver<BukkitBiome> resolver, long seed) {
        this.resolver = resolver;
    }

    /**
     * Alternative: construct with an already-created NMSChunkGenerator (if you prefer)
     */
    public BukkitChunkGeneratorWrapper(BiomeResolver<BukkitBiome> resolver, NMSChunkGenerator delegate) {
        this.resolver = resolver;
    }

    @Override
    public @Nullable BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo worldInfo) {
        // Expose the Bukkit-safe provider so plugins and the API see a valid Biome enum for each column
        return new BukkitBiomeProvider(resolver);
    }

    @Override
    public @NotNull List<BlockPopulator> getDefaultPopulators(@NotNull World world) {
        // If you have world-level populators (trees, ores outside the generator), return them here.
        return List.of();
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false;
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
    public boolean shouldGenerateStructures() {
        return true;
    }

    public BiomeResolver<BukkitBiome> getResolver() {
        return resolver;
    }
}
