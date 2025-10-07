package org.vicky.vspe_forge.forgeplatform;

import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.world.level.biome.Biome;
import org.vicky.platform.utils.ResourceLocation;
import org.vicky.vspe.platform.PlatformBiomeFactory;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.BiomeParameters;
import org.vicky.vspe_forge.VspeForge;
import org.vicky.vspe_forge.dimension.ForgeBiome;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class ForgeBiomeFactory implements PlatformBiomeFactory<ForgeBiome> {

    public static final Set<ForgeBiome> REGISTERED_BIOMES = new HashSet<>();

    public static void boostrap(BootstapContext<Biome> context) {
        REGISTERED_BIOMES.forEach((b) -> {
            VspeForge.LOGGER.info("Registering Biome: {}", b.getResourceKey().location());
            context.register(b.getResourceKey(), b.funner.apply(context));
        });
    }

    @Override
    public Optional<ForgeBiome> getFor(ResourceLocation loc) {
        return REGISTERED_BIOMES.stream().filter(it -> it.getResourceKey().location().toString().equals(loc.asString())).findAny()
    }

    @Override
    public ForgeBiome createBiome(BiomeParameters config) {
        var biome = new ForgeBiome(config);
        REGISTERED_BIOMES.add(biome);
        return biome;
    }
}
