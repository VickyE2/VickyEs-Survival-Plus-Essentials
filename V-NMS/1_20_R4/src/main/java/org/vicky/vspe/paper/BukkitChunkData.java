package org.vicky.vspe.paper;

import org.bukkit.block.data.BlockData;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;
import org.vicky.platform.world.PlatformBlockState;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.ChunkData;

public class BukkitChunkData implements ChunkData<BlockData, BukkitBiome> {

    private final ChunkGenerator.ChunkData data;
    private final ChunkGenerator.BiomeGrid grid;

    public BukkitChunkData(ChunkGenerator.ChunkData delegate, ChunkGenerator.BiomeGrid biomeGrid) {
        this.data = delegate;
        this.grid = biomeGrid;
    }

    @Override
    public void setBlock(int i, int i1, int i2, @NotNull PlatformBlockState<BlockData> platformBlockState) {
        data.setBlock(i, i1, i2, platformBlockState.getNative());
    }

    @Override
    public void setBiome(int i, int i1, int i2, @NotNull BukkitBiome bukkitBiome) {
        grid.setBiome(i, i1, i2, bukkitBiome.toNativeBiome().getBukkitBiome());
    }
}
