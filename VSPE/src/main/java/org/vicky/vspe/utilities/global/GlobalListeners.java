package org.vicky.vspe.utilities.global;

import eu.endercentral.crazy_advancements.advancement.Advancement;
import eu.endercentral.crazy_advancements.event.AdvancementGrantEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
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
import org.vicky.bukkitplatform.useables.BukkitPlatformPlayer;
import org.vicky.utilities.ANSIColor;
import org.vicky.utilities.ContextLogger.ContextLogger;
import org.vicky.utilities.DatabaseManager.dao_s.DatabasePlayerDAO;
import org.vicky.vspe.VSPE;
import org.vicky.vspe.features.AdvancementPlus.Advancements.TestAdvancement;
import org.vicky.vspe.features.AdvancementPlus.BukkitAdvancement;
import org.vicky.vspe.platform.features.advancement.Exceptions.AdvancementNotExists;
import org.vicky.vspe.platform.features.advancement.Exceptions.NullAdvancementUser;
import org.vicky.vspe.systems.dimension.BukkitBaseDimension;
import org.vicky.vspe.utilities.Hibernate.DBTemplates.AdvanceablePlayer;
import org.vicky.vspe.utilities.Hibernate.DBTemplates.CnTPlayer;
import org.vicky.vspe.utilities.Hibernate.api.AdvanceablePlayerService;
import org.vicky.vspe.utilities.Hibernate.dao_s.AdvanceablePlayerDAO;
import org.vicky.vspe.utilities.Hibernate.dao_s.AdvancementDAO;
import org.vicky.vspe.utilities.Hibernate.dao_s.CnTPlayerDAO;
import org.vicky.vspe.utilities.global.Events.PlayerReceivedAdvancement;

import java.util.Objects;
import java.util.Optional;

public class GlobalListeners implements Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        if (new AdvanceablePlayerDAO().findById(player.getUniqueId()).isEmpty()) {
            new ContextLogger(ContextLogger.ContextType.FEATURE, "HIBERNATE-PLAYER")
                    .print(
                            ANSIColor.colorize(
                                    "cyan[Creating instanced advanceable player for new player: ]" + player.getName()));
            AdvanceablePlayer advanceablePlayer = new AdvanceablePlayer();
            advanceablePlayer.setDatabasePlayer(new DatabasePlayerDAO().findById(player.getUniqueId()).get());
            new AdvanceablePlayerDAO().create(advanceablePlayer);
        }
        if (new CnTPlayerDAO().findById(player.getUniqueId()).isEmpty()) {
            new ContextLogger(ContextLogger.ContextType.FEATURE, "HIBERNATE-PLAYER")
                    .print(
                            ANSIColor.colorize(
                                    "cyan[Creating instanced trinket player for new player: ]" + player.getName()));
            CnTPlayer tPlayer = new CnTPlayer();
            tPlayer.setDatabasePlayer(new DatabasePlayerDAO().findById(player.getUniqueId()).get());
            new CnTPlayerDAO().create(tPlayer);
        }
        GlobalResources.advancementManager.ADVANCEMENT_MANAGER.addPlayer(player);
        (new BukkitRunnable() {
            public void run() {
                GlobalResources.advancementManager.grantAdvancemet(TestAdvancement.class, player);
                GlobalResources.advancementManager.sendAdvancementPackets(player);
            }
        }).runTaskLater(VSPE.getPlugin(), 10L);
        World transferringWorld = player.getWorld();
        Optional<BukkitBaseDimension> dimension2 = GlobalResources.dimensionManager
                .LOADED_DIMENSIONS
                .stream()
                .map(BukkitBaseDimension.class::cast)
                .filter(BukkitBaseDimension -> Objects.equals(BukkitBaseDimension.getWorld().getName(), transferringWorld.getName()))
                .findAny();
        if (dimension2.isPresent()) {
            final BukkitBaseDimension context = dimension2.get();
            context.applyJoinMechanics(player);
            (new BukkitRunnable() {
                public void run() {
                    context.applyMechanics(player);
                }
            }).runTaskLater(VSPE.getPlugin(), 10L);
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        GlobalResources.advancementManager.ADVANCEMENT_MANAGER.removePlayer(player);World transferringWorld = player.getWorld();
        Optional<BukkitBaseDimension> dimension2 = GlobalResources.dimensionManager
                .LOADED_DIMENSIONS
                .stream()
                .map(BukkitBaseDimension.class::cast)
                .filter(BukkitBaseDimension -> Objects.equals(BukkitBaseDimension.getWorld().getName(), transferringWorld.getName()))
                .findAny();
        if (dimension2.isPresent()) {
            final BukkitBaseDimension context = dimension2.get();
            context.disableMechanics(player);
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        final Player player = event.getPlayer();
        World previousWorld = event.getFrom();
        World transferringWorld = player.getWorld();
        Optional<BukkitBaseDimension> dimension = GlobalResources.dimensionManager
                .LOADED_DIMENSIONS
                .stream()
                .map(BukkitBaseDimension.class::cast)
                .filter(BukkitBaseDimension -> Objects.equals(BukkitBaseDimension.getWorld().getName(), previousWorld.getName()))
                .findAny();
        Optional<BukkitBaseDimension> dimension2 = GlobalResources.dimensionManager
                .LOADED_DIMENSIONS
                .stream()
                .map(BukkitBaseDimension.class::cast)
                .filter(BukkitBaseDimension -> Objects.equals(BukkitBaseDimension.getWorld().getName(), transferringWorld.getName()))
                .findAny();
        if (dimension.isPresent()) {
            BukkitBaseDimension context = dimension.get();
            context.disableMechanics(player);
        }

        if (dimension2.isPresent()) {
            final BukkitBaseDimension context = dimension2.get();
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
        Optional<BukkitAdvancement> optionalBaseAdvancement = GlobalResources.advancementManager
                .LOADED_ADVANCEMENTS
                .stream()
                .filter(baseAdvancement -> Objects.equals(baseAdvancement.getFormattedTitle(), advancement.getName().getKey()))
                .findAny();
        if (optionalBaseAdvancement.isPresent()) {
            BukkitAdvancement contextAdvancemet = optionalBaseAdvancement.get();
            if (!contextAdvancemet.isHasParent()) {
                GlobalResources.advancementManager.ADVANCEMENT_MANAGER.saveProgress(player, advancement);
            } else {
                Advancement parent = contextAdvancemet.getInstance().getParent();
                GlobalResources.advancementManager.ADVANCEMENT_MANAGER.saveProgress(player, parent, advancement);
            }
        }
    }

    @EventHandler
    public void onPlayerReceivedAdvancement(PlayerReceivedAdvancement event) throws AdvancementNotExists, NullAdvancementUser {
        final Player player = event.getPlayer();
        final AdvanceablePlayerService service = AdvanceablePlayerService.getInstance();
        final AdvancementDAO advancementService = new AdvancementDAO();
        VSPE.getInstancedLogger().info("Player gained advancement...");
        String playerName = player.getName();
        String advancementName = event.getAdvancementName();
        String advancementDescription = event.getAdvancementDescription();
        String advancementTextColor = event.getAdvancementType().getAdvancementColor();
        Optional<AdvanceablePlayer> oAP = service.getPlayerById(player.getUniqueId());
        if (oAP.isEmpty()) {
            throw new NullAdvancementUser("Failed to get AdvancementPlayer from database", BukkitPlatformPlayer.of(player));
        }
        AdvanceablePlayer databasePlayer = oAP.get();
        Component hoverMessage = Component
                .text()
                .append(
                        Component.text(event.getAdvancement().getTitle())
                                .color(TextColor.fromHexString(advancementTextColor))
                                .decoration(TextDecoration.BOLD, true)
                )
                .append(Component.text("\n" + advancementDescription))
                .color(TextColor.fromHexString(advancementTextColor))
                .build();
        Component hoverableAdvancement = Component.text("[" + advancementName + "]").color(TextColor.fromHexString(advancementTextColor))
                .hoverEvent(HoverEvent.showText(hoverMessage))
                .clickEvent(ClickEvent.runCommand(""));
        Component message = Component.text()
                .append(Component.text(playerName + " has completed the advancement: ").color(TextColor.fromHexString("#ffffff")))
                .append(hoverableAdvancement)
                .build();
        Bukkit.getServer().sendMessage(message);
        Optional<org.vicky.vspe.utilities.Hibernate.DBTemplates.Advancement> contextAdvancement =
                advancementService.findByName(event.getAdvancement().getTitle());
        if (contextAdvancement.isPresent()) {
            if (databasePlayer.getAccomplishedAdvancements().stream().noneMatch(a -> Objects.equals(a.getId(), contextAdvancement.get().getId())))
                databasePlayer.getAccomplishedAdvancements().add(contextAdvancement.get());
            service.updatePlayer(databasePlayer);
        }
        else {
            throw new AdvancementNotExists("Advancement could not be added to player as it fails to exist", event.getAdvancement());
        }
    }
}
