package org.vicky.vspe.platform;

import org.jetbrains.annotations.Nullable;
import org.vicky.platform.utils.ResourceLocation;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.NbtStructure;

public interface PlatformStructureManager {
    @Nullable <T> NbtStructure<T> getStructure(ResourceLocation id);
}