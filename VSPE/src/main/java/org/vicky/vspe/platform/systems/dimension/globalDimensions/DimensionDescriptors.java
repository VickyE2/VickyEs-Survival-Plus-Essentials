package org.vicky.vspe.platform.systems.dimension.globalDimensions;

import org.vicky.vspe.platform.systems.dimension.CoreDimensionRegistry;
import org.vicky.vspe.platform.systems.dimension.DimensionDescriptor;
import org.vicky.vspe.platform.systems.dimension.DimensionType;

import java.util.List;

public final class DimensionDescriptors {
    static final DimensionDescriptor CRYMORRA =
            new DimensionDescriptor(
                    "Crymorra",
                    "A land of frozen war...",
                    true,
                    List.of(DimensionType.FROZEN_WORLD, DimensionType.ELEMENTAL_WORLD),
                    "vspe:crymorra",
                    BiomeResolvers.getInstance().CRYMORRA_BIOME_RESOLVER()
            );

    static {
        // automatic core-side registration — safe even before platform exists
        CoreDimensionRegistry.register(CRYMORRA);
    }
}
