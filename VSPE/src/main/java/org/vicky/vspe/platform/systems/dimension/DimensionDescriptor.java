package org.vicky.vspe.platform.systems.dimension;

import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.BiomeResolver;

import java.util.List;

public record DimensionDescriptor(
        String name,
        String description,
        boolean shouldGenerateStructures,
        List<DimensionType> dimensionTypes,
        String identifier,
        BiomeResolver<?> resolver) {
}
