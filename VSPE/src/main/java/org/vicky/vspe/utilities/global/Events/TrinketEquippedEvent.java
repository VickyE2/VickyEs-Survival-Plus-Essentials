package org.vicky.vspe.utilities.global.Events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.vicky.vspe.features.CharmsAndTrinkets.BaseTrinket;

public class TrinketEquippedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final BaseTrinket trinket;

    public TrinketEquippedEvent(Player player, BaseTrinket trinket) {
        this.player = player;
        this.trinket = trinket;
    }
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public Player getPlayer() {
        return player;
    }

    public BaseTrinket getTrinket() {
        return trinket;
    }

    public boolean isTrinket(BaseTrinket compare) {
        return trinket.getClass().equals(compare.getClass());
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
