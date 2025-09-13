package org.vicky.vspe_forge.dimension;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.vicky.vspe_forge.VspeForge;

import static org.vicky.vspe_forge.VspeForge.MODID;

public class CodecCORE {
    public static final ResourceKey<Codec<? extends ChunkGenerator>> UNIMPRESSED_CHUNK_GENERATOR =
            ResourceKey.create(Registries.CHUNK_GENERATOR, new ResourceLocation(MODID, "unimpressed_chunk"));

    public static final ResourceKey<Codec<? extends BiomeSource>> UNIMPRESSED_BIOME_SOURCE =
            ResourceKey.create(Registries.BIOME_SOURCE, new ResourceLocation(MODID, "unimpressed_bsource"));

    public static void bootstrapGenerators(BootstapContext<Codec<? extends ChunkGenerator>> context) {
        context.register(UNIMPRESSED_CHUNK_GENERATOR, UnImpressedChunkGenerator.CODEC);
        VspeForge.LOGGER.info("Bootstrapped UnImpressedChunkGenerator codec for datagen");
    }

    public static void bootstrapBiomes(BootstapContext<Codec<? extends BiomeSource>> context) {
        context.register(UNIMPRESSED_BIOME_SOURCE, UnImpressedBiomeSource.CODEC);
        VspeForge.LOGGER.info("Bootstrapped UnImpressedBiomeSource codec for datagen");
    }
}
