package org.vicky.vspe.utilities.global.Events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.vicky.vspe.features.AdvancementPlus.AdvancementType;
import org.vicky.vspe.features.AdvancementPlus.BaseAdvancement;
import org.vicky.vspe.systems.BroadcastSystem.ToastType;

public class PlayerReceivedAdvancement extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final BaseAdvancement advancement;

    public PlayerReceivedAdvancement(Player player, BaseAdvancement advancement) {
        this.player = player;
        this.advancement = advancement;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public Player getPlayer() {
        return player;
    }

    public String getAdvancementName() {
        return advancement.getTitle();
    }

    public String getAdvancementDescription() {
        return advancement.getDescription();
    }

    public AdvancementType getAdvancementType() {
        return advancement.getAdvancementType();
    }

    public ToastType getAdvancementToastType() {
        return advancement.getAdvancementTT();
    }

    public BaseAdvancement getAdvancement() {
        return advancement;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
