package org.vicky.vspe.platform;

import org.vicky.platform.utils.ResourceLocation;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.BiomeParameters;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.PlatformBiome;

import java.util.Optional;

public interface PlatformBiomeFactory<B extends PlatformBiome> {
    B createBiome(BiomeParameters config);

    Optional<B> getFor(ResourceLocation loc);
}
