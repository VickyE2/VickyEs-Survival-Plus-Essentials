package org.vicky.vspe.paper;

import org.vicky.vspe.platform.PlatformBiomeFactory;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.BiomeParameters;

public class BukkitBiomeFactory implements PlatformBiomeFactory<BukkitBiome> {

    @Override
    public BukkitBiome createBiome(BiomeParameters biomeParameters) {
        return BukkitBiome.fromParams(biomeParameters);
    }
}
