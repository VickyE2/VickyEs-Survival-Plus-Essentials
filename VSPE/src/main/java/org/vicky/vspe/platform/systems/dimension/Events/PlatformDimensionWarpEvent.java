package org.vicky.vspe.platform.systems.dimension.Events;

import org.vicky.platform.events.PlatformCancellableEvent;
import org.vicky.vspe.platform.systems.dimension.PlatformBaseDimension;

public interface PlatformDimensionWarpEvent extends PlatformCancellableEvent {
    PlatformBaseDimension getBaseDimension();
    boolean eventIsCancelled();
}