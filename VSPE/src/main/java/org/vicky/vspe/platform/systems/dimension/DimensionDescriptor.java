package org.vicky.vspe.platform.systems.dimension;

import java.util.List;

public record DimensionDescriptor(
        String name,
        String description,
        boolean shouldGenerateStructures,
        List<DimensionType> dimensionTypes,
        String identifier) {
}
