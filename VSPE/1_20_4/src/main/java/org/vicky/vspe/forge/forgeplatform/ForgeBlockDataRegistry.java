package org.vicky.vspe.forge.forgeplatform;

import net.minecraft.world.level.block.state.BlockState;
import org.vicky.platform.PlatformPlugin;
import org.vicky.platform.utils.ResourceLocation;
import org.vicky.platform.world.PlatformBlockState;
import org.vicky.vspe.platform.PlatformBlockDataRegistry;

public class ForgeBlockDataRegistry implements PlatformBlockDataRegistry {
    public static ForgeBlockDataRegistry INSTANCE = new ForgeBlockDataRegistry();

    private ForgeBlockDataRegistry() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public PlatformBlockState<BlockState> getBlockState(ResourceLocation id) {
        return (PlatformBlockState<BlockState>) PlatformPlugin.stateFactory().getBlockState(id.asString());
    }
}
