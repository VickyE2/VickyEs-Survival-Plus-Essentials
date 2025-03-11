package org.vicky.vspe.systems.Dimension.Events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.vicky.vspe.systems.Dimension.BaseDimension;

public class PlayerEnterDimensionEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final BaseDimension dimension;
    private boolean cancelled;

    public PlayerEnterDimensionEvent(Player player, BaseDimension dimension) {
        super(player);
        this.dimension = dimension;
        this.cancelled = false;
    }

    public BaseDimension getDimension() {
        return dimension;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}