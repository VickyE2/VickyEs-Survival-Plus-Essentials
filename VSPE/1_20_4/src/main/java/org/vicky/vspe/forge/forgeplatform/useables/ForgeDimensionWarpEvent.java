package org.vicky.vspe.forge.forgeplatform.useables;

import org.vicky.forge.forgeplatform.useables.ForgeEvent;
import org.vicky.vspe.forge.events.DimensionWarpEvent;
import org.vicky.vspe.platform.systems.dimension.Events.PlatformDimensionWarpEvent;
import org.vicky.vspe.platform.systems.dimension.PlatformBaseDimension;

public class ForgeDimensionWarpEvent extends ForgeEvent implements PlatformDimensionWarpEvent {

    private final PlatformBaseDimension<?, ?> dimension;

    public ForgeDimensionWarpEvent(PlatformBaseDimension<?, ?> dimension, DimensionWarpEvent event) {
        super(event);
        this.dimension = dimension;
    }

    @Override
    public PlatformBaseDimension<?, ?> getBaseDimension() {
        return dimension;
    }

    @Override
    public boolean eventIsCancelled() {
        return true;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        getEvent().setCanceled(cancelled);
    }
}
