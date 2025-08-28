package org.vicky.vspe.paper;

import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;
import org.vicky.vspe.nms.BiomeWrapper;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.BiomeResolver;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.InvertedPalette;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.Palette;

import java.util.List;
import java.util.stream.Stream;

public class BukkitBiomeProvider extends BiomeProvider implements BiomeResolver<BukkitBiome> {
    private final BiomeResolver<BukkitBiome> biomeProvider;
    private volatile long seed;

    public BukkitBiomeProvider(BiomeResolver<BukkitBiome> resolver) {
        this.biomeProvider = resolver;
    }

    @Override
    public @NotNull Biome getBiome(@NotNull WorldInfo worldInfo, int i, int i1, int i2) {
        return biomeProvider.resolveBiome(i, i1, i2, seed).toNativeBiome().getBase();
    }

    @Override
    public @NotNull List<Biome> getBiomes(@NotNull WorldInfo worldInfo) {
        Stream<BukkitBiome> val;
        if (biomeProvider.getBiomePalette() instanceof InvertedPalette<BukkitBiome> i) {
            val = i.getInvertedPaletteMap().keySet().stream();
        } else {
            val = biomeProvider.getBiomePalette().getPaletteMap().values().stream();
        }
        return val.map(BukkitBiome::toNativeBiome)
                .map(BiomeWrapper::getBase)
                .toList();
    }

    public BiomeResolver<BukkitBiome> getBiomeProvider() {
        return biomeProvider;
    }

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
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
