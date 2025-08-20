package org.vicky.vspe.platform.systems.dimension.globalDimensions;

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
                    "vspe:crymorra"
            );
}
