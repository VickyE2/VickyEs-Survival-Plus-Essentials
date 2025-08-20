package org.vicky.vspe.platform;

import org.jetbrains.annotations.NotNull;
import org.vicky.vspe.platform.systems.dimension.DimensionDescriptor;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.PlatformBiome;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.PlatformDimension;

public interface PlatformDimensionFactory<D extends PlatformDimension<T, B>, T, B extends PlatformBiome> {
    @NotNull T createDimension(DimensionDescriptor info);
}
