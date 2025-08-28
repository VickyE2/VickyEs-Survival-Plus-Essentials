package org.vicky.vspe.utilities.global.Events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.vicky.vspe.features.CharmsAndTrinkets.BaseTrinket;

public class TrinketGenerationEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final BaseTrinket trinket;
    private boolean isCancelled = false;

    public TrinketGenerationEvent(BaseTrinket trinket) {
        this.trinket = trinket;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    public void setCancelled(boolean cancelled) {
        isCancelled = cancelled;
    }

    public BaseTrinket getTrinket() {
        return trinket;
    }
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
    return HANDLERS;
    }
}
