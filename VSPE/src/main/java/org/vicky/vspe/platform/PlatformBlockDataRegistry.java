package org.vicky.vspe.platform;

import org.vicky.platform.utils.ResourceLocation;
import org.vicky.platform.world.PlatformBlockState;

import java.util.List;

public interface PlatformBlockDataRegistry {
    default List<ResourceLocation> getNonMinecraftDefaults() {
        return NativeTypeMapper.nativeMaps.keySet().stream().map(ResourceLocation::from).toList();
    }

    <T> PlatformBlockState<T> getBlockState(ResourceLocation id);
}
