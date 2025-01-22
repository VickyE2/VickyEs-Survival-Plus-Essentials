package org.vicky.vspe.utilities.global;

import eu.endercentral.crazy_advancements.advancement.Advancement;
import eu.endercentral.crazy_advancements.event.AdvancementGrantEvent;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextComponent.Builder;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.vicky.utilities.ANSIColor;
import org.vicky.vspe.VSPE;
import org.vicky.vspe.features.AdvancementPlus.BaseAdvancement;
import org.vicky.vspe.features.AdvancementPlus.Advancements.TestAdvancement;
import org.vicky.vspe.systems.Dimension.BaseDimension;
import org.vicky.vspe.utilities.DatabaseManager.templates.DatabasePlayer;
import org.vicky.vspe.utilities.global.Events.PlayerReceivedAdvancement;

public class GlobalListeners implements Listener {
   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent event) {
      final Player player = event.getPlayer();
      GlobalResources.advancementManager.ADVANCEMENT_MANAGER.addPlayer(player);
      if (!GlobalResources.databaseManager.entityExists(DatabasePlayer.class, player.getUniqueId())) {
         VSPE.getInstancedLogger().info(ANSIColor.colorize("cyan[Creating instanced player for new player: ]" + player.getName()));
         DatabasePlayer instancedDatabasePlayer = new DatabasePlayer();
         instancedDatabasePlayer.setId(player.getUniqueId());
         instancedDatabasePlayer.setFirstTime(true);
         GlobalResources.databaseManager.saveEntity(instancedDatabasePlayer);
      }

      GlobalResources.advancementManager.ADVANCEMENT_MANAGER.addPlayer(player);
      (new BukkitRunnable() {
         public void run() {
            GlobalResources.advancementManager.grantAdvancemet(TestAdvancement.class, player);
            GlobalResources.advancementManager.sendAdvancementPackets(player);
         }
      }).runTaskLater(VSPE.getPlugin(), 10L);
   }

   @EventHandler
   public void onPlayerLeave(PlayerQuitEvent event) {
      Player player = event.getPlayer();
      GlobalResources.advancementManager.ADVANCEMENT_MANAGER.removePlayer(player);
   }

   @EventHandler
   public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
      final Player player = event.getPlayer();
      World previousWorld = event.getFrom();
      World transferringWorld = player.getWorld();
      Optional<BaseDimension> dimension = GlobalResources.dimensionManager
         .LOADED_DIMENSIONS
         .stream()
         .filter(baseDimension -> Objects.equals(baseDimension.getWorld().getName(), previousWorld.getName()))
         .findAny();
      Optional<BaseDimension> dimension2 = GlobalResources.dimensionManager
         .LOADED_DIMENSIONS
         .stream()
         .filter(baseDimension -> Objects.equals(baseDimension.getWorld().getName(), transferringWorld.getName()))
         .findAny();
      if (dimension.isPresent()) {
         BaseDimension context = dimension.get();
         context.disableMechanics(player);
      }

      if (dimension2.isPresent()) {
         final BaseDimension context = dimension2.get();
         context.applyJoinMechanics(player);
         (new BukkitRunnable() {
            public void run() {
               context.applyMechanics(player);
            }
         }).runTaskLater(VSPE.getPlugin(), 10L);
      }
   }

   @EventHandler
   public void onPlayerReceivedAdvancements(AdvancementGrantEvent event) {
      Player player = event.getPlayer();
      Advancement advancement = event.getAdvancement();
      Optional<BaseAdvancement> optionalBaseAdvancement = GlobalResources.advancementManager
         .LOADED_ADVANCEMENTS
         .stream()
         .filter(baseAdvancement -> Objects.equals(baseAdvancement.getFormattedTitle(), advancement.getName().getKey()))
         .findAny();
      if (optionalBaseAdvancement.isPresent()) {
         BaseAdvancement contextAdvancemet = optionalBaseAdvancement.get();
         if (!contextAdvancemet.isHasParent()) {
            GlobalResources.advancementManager.ADVANCEMENT_MANAGER.saveProgress(player, new Advancement[]{advancement});
         } else {
            Advancement parent = contextAdvancemet.getInstance().getParent();
            GlobalResources.advancementManager.ADVANCEMENT_MANAGER.saveProgress(player, new Advancement[]{parent, advancement});
         }
      }
   }

   @EventHandler
   public void onPlayerReceivedAdvancement(PlayerReceivedAdvancement event) {
      VSPE.getInstancedLogger().info("Player gained advancement...");
      String playerName = event.getPlayer().getName();
      String advancementName = event.getAdvancementName();
      String advancementDescription = event.getAdvancementDescription();
      String advancementTextColor = event.getAdvancementType().getAdvancementColor();
      DatabasePlayer databasePlayer = GlobalResources.databaseManager.getEntityById(DatabasePlayer.class, event.getPlayer().getUniqueId());
      Component hoverableAdvancement = ((TextComponent)Component.text("[" + advancementName + "]").color(TextColor.fromHexString(advancementTextColor)))
         .hoverEvent(
            HoverEvent.showText(
               Component.text(
                     ((TextComponent)Component.text(event.getAdvancement().getTitle()).color(TextColor.fromHexString(advancementTextColor)))
                           .decoration(TextDecoration.BOLD, true)
                        + "\n"
                        + advancementDescription
                  )
                  .color(TextColor.fromHexString(advancementTextColor))
            )
         );
      Component message = ((Builder)((Builder)Component.text()
               .append(Component.text(playerName + " has completed the advancement: ").color(TextColor.fromHexString("#ffffff"))))
            .append(hoverableAdvancement))
         .build();
      Bukkit.getServer().sendMessage(message);
      org.vicky.vspe.utilities.DatabaseManager.templates.Advancement contextAdvancement = new org.vicky.vspe.utilities.DatabaseManager.templates.Advancement();
      contextAdvancement.setId(event.getAdvancement().getId());
      contextAdvancement.setName(event.getAdvancement().getNamespace());
      databasePlayer.getAccomplishedAdvancements().add(contextAdvancement);
   }
}
