package org.vicky.vspe.paper;

import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.vicky.platform.utils.ResourceLocation;
import org.vicky.platform.world.PlatformBlockState;
import org.vicky.vspe.platform.PlatformBlockDataRegistry;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.SimpleBlockState;

public class VSPEBukkitBlockDataRegistry implements PlatformBlockDataRegistry<BlockData> {
    @Override
    public PlatformBlockState<BlockData> getBlockState(ResourceLocation resourceLocation) {
        return SimpleBlockState.Companion.from("minecraft:grass_block", Bukkit::createBlockData);
    }
}
