package org.vicky.vspe_forge.forgeplatform.useables;

import org.jetbrains.annotations.NotNull;
import org.vicky.vspe.platform.systems.dimension.DimensionDescriptor;

public interface Descriptored {
    @NotNull DimensionDescriptor getDescriptor();
}
