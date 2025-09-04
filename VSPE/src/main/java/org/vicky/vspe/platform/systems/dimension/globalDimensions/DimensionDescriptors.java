package org.vicky.vspe.platform.systems.dimension.globalDimensions;

import org.vicky.vspe.platform.systems.dimension.CoreDimensionRegistry;
import org.vicky.vspe.platform.systems.dimension.DimensionDescriptor;
import org.vicky.vspe.platform.systems.dimension.DimensionType;

import java.util.List;

public final class DimensionDescriptors {
    public static final DimensionDescriptor CRYMORRA =
            new DimensionDescriptor(
                    "Crymorra",
                    "A land of frozen war...",
                    true,
                    List.of(DimensionType.FROZEN_WORLD, DimensionType.ELEMENTAL_WORLD),
                    "vspe:crymorra",
                    BiomeResolvers.getInstance().CRYMORRA_BIOME_RESOLVER(),
                    64,
                    null /*PlatformPlugin.stateFactory().getBlockState("minecraft:water")*/,
                    24000,
                    true,
                    false,
                    false,
                    false,
                    true,
                    false,
                    0.7f,
                    4.0f,
                    15,
                    15,
                    218,
                    319,
                    -64
            );

    static {
        // automatic core-side registration — safe even before platform exists
        CoreDimensionRegistry.register(CRYMORRA);
    }
}
