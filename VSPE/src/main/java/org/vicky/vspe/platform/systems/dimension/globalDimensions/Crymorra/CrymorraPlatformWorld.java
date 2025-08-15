package org.vicky.vspe.platform.systems.dimension.globalDimensions.Crymorra;

import org.vicky.platform.world.PlatformWorld;

public abstract class CrymorraPlatformWorld<T, N> implements PlatformWorld<T, N> {
    @Override
    public final String getName() {
        return "Crymorra";
    }
}
