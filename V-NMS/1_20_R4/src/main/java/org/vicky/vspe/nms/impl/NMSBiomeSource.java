package org.vicky.vspe.nms.impl;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import org.jetbrains.annotations.NotNull;
import org.vicky.vspe.nms.BiomeCompatibilityAPI;
import org.vicky.vspe.paper.BukkitBiome;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.BiomeResolver;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.InvertedPalette;

import java.util.stream.Stream;

public class NMSBiomeSource extends BiomeSource {
    private final BiomeResolver<BukkitBiome> biomeProvider;

    public NMSBiomeSource(BiomeResolver<BukkitBiome> resolver) {
        this.biomeProvider = resolver;
    }

    @Override
    protected @NotNull Codec<? extends BiomeSource> codec() {
        return BiomeSource.CODEC;
    }

    @Override
    protected @NotNull Stream<Holder<net.minecraft.world.level.biome.Biome>> collectPossibleBiomes() {
        Stream<Holder<net.minecraft.world.level.biome.Biome>> val;
        if (biomeProvider.getBiomePalette() instanceof InvertedPalette<BukkitBiome> i) {
            val = i.getInvertedPaletteMap().keySet().stream()
                    .map(biome -> BiomeCompatibilityAPI.Companion.getBiomeCompatibility().getRegistry()
                            .getHolderOrThrow(biome.toNativeBiome().getResource()));
        } else {
            val = biomeProvider.getBiomePalette().getPaletteMap().values().stream()
                    .map(biome -> BiomeCompatibilityAPI.Companion.getBiomeCompatibility().getRegistry()
                            .getHolderOrThrow(biome.toNativeBiome().getResource()));
        }
        return val;
    }

    @Override
    public @NotNull Holder<net.minecraft.world.level.biome.Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler noise) {
        return BiomeCompatibilityAPI.Companion.getBiomeCompatibility().getRegistry()
                .getHolderOrThrow(biomeProvider.resolveBiome(x, y, z, 0).toNativeBiome().getResource());
    }

    public BiomeResolver<BukkitBiome> getBiomeProvider() {
        return biomeProvider;
    }
}
