package org.vicky.vspe.paper;

import org.vicky.vspe.platform.systems.dimension.Events.PlatformDimensionWarpEvent;
import org.vicky.vspe.platform.systems.dimension.PlatformBaseDimension;
import org.vicky.vspe.systems.dimension.BukkitBaseDimension;
import org.vicky.vspe.systems.dimension.Events.DimensionWarpEvent;

public record BukkitDimensionWarpEvent(BukkitBaseDimension dimension,
                                       DimensionWarpEvent e) implements PlatformDimensionWarpEvent {

    @Override
    public String getEventName() {
        return e.getEventName();
    }

    @Override
    public PlatformBaseDimension<?, ?> getBaseDimension() {
        return dimension;
    }

    @Override
    public boolean eventIsCancelled() {
        return e.isCancelled();
    }
}
