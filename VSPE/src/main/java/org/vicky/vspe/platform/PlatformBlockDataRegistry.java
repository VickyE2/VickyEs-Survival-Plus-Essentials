package org.vicky.vspe.platform;

import org.vicky.platform.utils.ResourceLocation;
import org.vicky.platform.world.PlatformBlockState;

import java.util.ArrayList;
import java.util.List;

public interface PlatformBlockDataRegistry<T> {
    default List<ResourceLocation> getNonMinecraftDefaults() {
        return new ArrayList<>(List.of(
                ResourceLocation.from("crymorra:magenta_grass_block")
        ));
    }

    PlatformBlockState<T> getBlockState(ResourceLocation id);
}
