package org.vicky.vspe.forge.dimension;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import org.jetbrains.annotations.NotNull;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.BiomeResolver;

import java.util.stream.Stream;

import static org.vicky.vspe.VspeForge.registryAccess;

public class UnImpressedBiomeSource extends BiomeSource {
    // Dummy codec if you don't need datapack/worldgen json
    public static final Codec<UnImpressedBiomeSource> CODEC =
            Codec.unit(() -> {
                throw new UnsupportedOperationException("Not serializable");
            });
    private final BiomeResolver<ForgeBiome> biomeProvider;

    public UnImpressedBiomeSource(BiomeResolver<ForgeBiome> resolver) {
        this.biomeProvider = resolver;
    }

    @Override
    protected @NotNull Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected @NotNull Stream<Holder<Biome>> collectPossibleBiomes() {
        return biomeProvider.getBiomePalette().getPaletteMap().values().stream()
                .map(forgeBiome -> {
                    ResourceKey<Biome> key = forgeBiome.getResourceKey();
                    return registryAccess.registryOrThrow(net.minecraft.core.registries.Registries.BIOME)
                            .getHolderOrThrow(key);
                });
    }

    @Override
    public @NotNull Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.@NotNull Sampler noise) {
        ForgeBiome resolved = biomeProvider.resolveBiome(x, y, z, 0);
        ResourceLocation rl = resolved.getResourceKey().location();

        ResourceKey<Biome> key = ResourceKey.create(
                net.minecraft.core.registries.Registries.BIOME,
                rl
        );

        return registryAccess.registryOrThrow(net.minecraft.core.registries.Registries.BIOME)
                .getHolderOrThrow(key);
    }

    public BiomeResolver<ForgeBiome> getBiomeProvider() {
        return biomeProvider;
    }
}
