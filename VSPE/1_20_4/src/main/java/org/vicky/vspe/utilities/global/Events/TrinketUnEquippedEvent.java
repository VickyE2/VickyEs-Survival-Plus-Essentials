package org.vicky.vspe.utilities.global.Events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.vicky.vspe.features.CharmsAndTrinkets.BaseTrinket;

public class TrinketUnEquippedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final BaseTrinket trinket;

    /**
     * This event is fired when a player using the trinket gui puts a trinket in the appropriate slot.
     *
     * @param player The {@link Player} in context
     * @param trinket The {@link BaseTrinket} equipped
     */
    public TrinketUnEquippedEvent(@NotNull Player player, BaseTrinket trinket) {
        this.player = player;
        this.trinket = trinket;
    }

    public Player getPlayer() {
        return player;
    }

    public BaseTrinket getTrinket() {
        return trinket;
    }

    public boolean isTrinket(BaseTrinket compare) {
        if (trinket == null) return false;
        return trinket.getClass().equals(compare.getClass());
    }
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
