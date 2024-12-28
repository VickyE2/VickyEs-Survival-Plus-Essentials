package org.vicky.vspe.utilities.global;

import eu.endercentral.crazy_advancements.event.AdvancementGrantEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
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
import org.vicky.vspe.features.AdvancementPlus.Advancements.TestAdvancement;
import org.vicky.vspe.features.AdvancementPlus.BaseAdvancement;
import org.vicky.vspe.systems.Dimension.BaseDimension;
import org.vicky.vspe.utilities.DatabaseManager.templates.Advancement;
import org.vicky.vspe.utilities.DatabaseManager.templates.DatabasePlayer;
import org.vicky.vspe.utilities.global.Events.PlayerReceivedAdvancement;

import java.util.Objects;
import java.util.Optional;

import static org.vicky.vspe.utilities.global.GlobalResources.*;

public class GlobalListeners implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        advancementManager.ADVANCEMENT_MANAGER.addPlayer(player);

        if (!databaseManager.entityExists(DatabasePlayer.class, player.getUniqueId())) {
            VSPE.getInstancedLogger().info(ANSIColor.colorize("cyan[Creating instanced player for new player: ]" + player.getName()));
            DatabasePlayer instancedDatabasePlayer =
                    new DatabasePlayer();
            instancedDatabasePlayer.setId(player.getUniqueId());
            instancedDatabasePlayer.setFirstTime(true);
            databaseManager.saveEntity(instancedDatabasePlayer);
        }
        advancementManager.ADVANCEMENT_MANAGER.addPlayer(player);
        new BukkitRunnable() {
            @Override
            public void run() {
                advancementManager.grantAdvancemet(TestAdvancement.class, player);
                advancementManager.sendAdvancementPackets(player);
            }
        }.runTaskLater(VSPE.getPlugin(), 10);
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        advancementManager.ADVANCEMENT_MANAGER.removePlayer(player);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World previousWorld = event.getFrom();
        World transferringWorld = player.getWorld();

        BaseDimension dimension = dimensionManager.LOADED_DIMENSIONS.get(previousWorld.getName());
        BaseDimension dimension2 = dimensionManager.LOADED_DIMENSIONS.get(transferringWorld.getName());

        if (dimension != null) {
            dimension.disableMechanics(player);
        }
        if (dimension2 != null) {
            dimension2.applyJoinMechanics(player);
            dimension2.applyMechanics(player);
        }
    }

    @EventHandler
    public void onPlayerReceivedAdvancements(AdvancementGrantEvent event) {
        Player player = event.getPlayer();
        eu.endercentral.crazy_advancements.advancement.Advancement advancement =
                event.getAdvancement();
        Optional<BaseAdvancement> optionalBaseAdvancement =
                advancementManager.LOADED_ADVANCEMENTS.stream()
                        .filter(baseAdvancement -> Objects.equals(baseAdvancement.getFormattedTitle(), advancement.getName().getKey()))
                        .findAny();
        if (optionalBaseAdvancement.isPresent()) {
            BaseAdvancement contextAdvancemet = optionalBaseAdvancement.get();
            if (!contextAdvancemet.isHasParent()) {
                advancementManager.ADVANCEMENT_MANAGER.saveProgress(player, advancement);
            } else {
                eu.endercentral.crazy_advancements.advancement.Advancement parent = contextAdvancemet.getInstance().getParent();
                advancementManager.ADVANCEMENT_MANAGER.saveProgress(player, parent, advancement);
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
        DatabasePlayer databasePlayer =
                databaseManager.getEntityById(DatabasePlayer.class, event.getPlayer().getUniqueId());

        Component hoverableAdvancement = Component.text("[" + advancementName + "]").color(TextColor.fromHexString(advancementTextColor))
                .hoverEvent(HoverEvent.showText(
                        Component.text(event.getAdvancement().getTitle() + "\n" + advancementDescription).color(TextColor.fromHexString(advancementTextColor))
                ));

        Component message = Component.text()
                .append(Component.text(playerName + " has completed the advancement: ").color(TextColor.fromHexString("#ffffff")))
                .append(hoverableAdvancement)
                .build();

        Bukkit.getServer().sendMessage(message);

        Advancement contextAdvancement = new Advancement();
        contextAdvancement.setId(event.getAdvancement().getId());
        contextAdvancement.setName(event.getAdvancement().getNamespace());

        databasePlayer.getAccomplishedAdvancements().add(contextAdvancement);
    }
}
