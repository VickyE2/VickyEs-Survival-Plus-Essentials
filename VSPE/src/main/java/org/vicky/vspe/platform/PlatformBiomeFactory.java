package org.vicky.vspe.platform;

import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.BiomeParameters;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.PlatformBiome;

public interface PlatformBiomeFactory<B extends PlatformBiome> {
    B createBiome(BiomeParameters config);
}
