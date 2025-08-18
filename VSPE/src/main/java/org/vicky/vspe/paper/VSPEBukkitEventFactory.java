package org.vicky.vspe.paper;

import org.bukkit.Bukkit;
import org.vicky.platform.events.PlatformEvent;
import org.vicky.platform.events.PlatformEventFactory;
import org.vicky.platform.exceptions.UnsupportedEventException;

public class VSPEBukkitEventFactory implements PlatformEventFactory {
    @Override
    public <T extends PlatformEvent> T firePlatformEvent(T t) throws UnsupportedEventException {
        if (t instanceof BukkitPlatformEvent(org.bukkit.event.Event event)) {
            Bukkit.getPluginManager().callEvent(event);
        }
        throw new UnsupportedEventException("That isn't a bukkit event ;-;");
    }
}
