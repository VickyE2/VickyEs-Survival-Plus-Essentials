package org.vicky.vspe.paper;

import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class BukkitBlockPopulator extends BlockPopulator {

    private final BukkitBiome biome;

    public BukkitBlockPopulator(BukkitBiome bukkitBiome) {
        this.biome = bukkitBiome;
    }

    @Override
    public void populate(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull LimitedRegion limitedRegion) {
        super.populate(worldInfo, random, chunkX, chunkZ, limitedRegion);
    }
}
