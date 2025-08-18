package org.vicky.vspe.paper;

import org.bukkit.event.Event;
import org.vicky.platform.events.PlatformEvent;

public record BukkitPlatformEvent(Event event) implements PlatformEvent {
    @Override
    public String getEventName() {
        return event.getEventName();
    }
}
