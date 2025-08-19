package org.vicky.vspe.paper;

import org.bukkit.block.Biome;
import org.bukkit.block.data.BlockData;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;
import org.vicky.vspe.nms.BiomeWrapper;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.BiomeResolver;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.Palette;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.PlatformDimension;

import java.util.List;

public class BukkitBiomeProvider extends BiomeProvider implements BiomeResolver<BukkitBiome> {

    private final BiomeResolver<BukkitBiome> biomeProvider;
    private final long seed;
    private final PlatformDimension<?, BukkitBiome> dimension;

    public BukkitBiomeProvider(PlatformDimension<BlockData, BukkitBiome> dimension) {
        this.dimension = dimension;
        this.biomeProvider = dimension.getBiomeResolver();
        this.seed = dimension.getRandom().getSeed();
    }

    @Override
    public @NotNull Biome getBiome(@NotNull WorldInfo worldInfo, int i, int i1, int i2) {
        return biomeProvider.resolveBiome(i, i1, i2, seed).toNativeBiome().getBukkitBiome();
    }

    @Override
    public @NotNull List<Biome> getBiomes(@NotNull WorldInfo worldInfo) {
        return biomeProvider.getBiomePalette().getPaletteMap()
                .values().stream()
                .map(BukkitBiome::toNativeBiome)
                .map(BiomeWrapper::getBukkitBiome)
                .toList();
    }

    public BiomeResolver<BukkitBiome> getBiomeProvider() {
        return biomeProvider;
    }

    public PlatformDimension<?, BukkitBiome> getDimension() {
        return dimension;
    }

    public long getSeed() {
        return seed;
    }

    @Override
    public @NotNull Palette<BukkitBiome> getBiomePalette() {
        return biomeProvider.getBiomePalette();
    }

    @Override
    public @NotNull BukkitBiome resolveBiome(int i, int i1, int i2, long l) {
        return biomeProvider.resolveBiome(i, i1, i2, l);
    }
}
