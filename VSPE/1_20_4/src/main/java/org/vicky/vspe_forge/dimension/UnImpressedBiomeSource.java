package org.vicky.vspe_forge.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import org.jetbrains.annotations.NotNull;
import org.vicky.vspe.platform.systems.dimension.CoreDimensionRegistry;
import org.vicky.vspe.platform.systems.dimension.DimensionDescriptor;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.BiomeResolver;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.InvertedPalette;

import java.util.stream.Stream;

import static org.vicky.vspe_forge.VspeForge.registryAccess;

public class UnImpressedBiomeSource extends BiomeSource {
    public static final Codec<UnImpressedBiomeSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("descriptor_name").forGetter(src -> src.descriptor.name()),
            Codec.STRING.fieldOf("descriptor_id").forGetter(src -> src.descriptor.identifier()),
            Codec.LONG.fieldOf("seed").forGetter(src -> src.seed)
    ).apply(instance, (name, id, seed) -> {
        try {
            Class.forName("org.vicky.vspe.platform.systems.dimension.globalDimensions.DimensionDescriptors");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        DimensionDescriptor desc = CoreDimensionRegistry.getRegisteredDescriptors().stream().filter(it -> it.identifier().equals(id)).findFirst().get();
        return new UnImpressedBiomeSource(desc, seed);
    }));

    private final BiomeResolver<ForgeBiome> biomeProvider;
    private final long seed;
    final DimensionDescriptor descriptor;

    public UnImpressedBiomeSource(DimensionDescriptor resolver, long seed) {
        this.biomeProvider = (BiomeResolver<ForgeBiome>) resolver.resolver();
        this.descriptor = resolver;
        this.seed = seed;
    }

    @Override
    protected @NotNull Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected @NotNull Stream<Holder<Biome>> collectPossibleBiomes() {
        Stream<Holder<Biome>> val;
        if (biomeProvider.getBiomePalette() instanceof InvertedPalette<ForgeBiome> i) {
            val = i.getInvertedPaletteMap().keySet().stream()
                    .map(forgeBiome -> {
                        ResourceKey<Biome> key = forgeBiome.getResourceKey();
                        return registryAccess
                                .lookup(Registries.BIOME)
                                .get().getOrThrow(key);
                    });
        } else {
            val = biomeProvider.getBiomePalette().getPaletteMap().values().stream()
                    .map(forgeBiome -> {
                        ResourceKey<Biome> key = forgeBiome.getResourceKey();
                        return registryAccess
                                .lookup(Registries.BIOME)
                                .get().getOrThrow(key);
                    });
        }
        return val;
    }

    @Override
    public @NotNull Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.@NotNull Sampler noise) {
        ForgeBiome resolved = biomeProvider.resolveBiome(x, 0, z, seed);
        ResourceKey<Biome> key = resolved.getResourceKey();
        return registryAccess
                .lookup(Registries.BIOME)
                .get().getOrThrow(key);
    }

    public BiomeResolver<ForgeBiome> getBiomeProvider() {
        return biomeProvider;
    }
}
