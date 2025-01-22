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
      return this.player;
   }

   public String getAdvancementName() {
      return this.advancement.getTitle();
   }

   public String getAdvancementDescription() {
      return this.advancement.getDescription();
   }

   public AdvancementType getAdvancementType() {
      return this.advancement.getAdvancementType();
   }

   public ToastType getAdvancementToastType() {
      return this.advancement.getAdvancementTT();
   }

   public BaseAdvancement getAdvancement() {
      return this.advancement;
   }

   public HandlerList getHandlers() {
      return HANDLERS;
   }
}
