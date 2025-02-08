package org.vicky.vspe;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiversePortals.MultiversePortals;
import com.onarandombox.multiverseinventories.MultiverseInventories;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.vicky.utilities.ANSIColor;
import org.vicky.utilities.ConfigManager;
import org.vicky.utilities.Identifiable;
import org.vicky.vicky_utils;
import org.vicky.vspe.features.AdvancementPlus.AdvancementManager;
import org.vicky.vspe.features.AdvancementPlus.Exceptions.AdvancementProcessingFailureException;
import org.vicky.vspe.systems.Dimension.DimensionManager;
import org.vicky.vspe.utilities.Config;
import org.vicky.vspe.utilities.Manager.EntityNotFoundException;
import org.vicky.utilities.Identifiable;
import org.vicky.vspe.utilities.Manager.IdentifiableManager;
import org.vicky.vspe.utilities.Manager.ManagerRegistry;
import org.vicky.vspe.utilities.global.GlobalListeners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import static org.vicky.vspe.utilities.global.GlobalResources.*;

public final class VSPE extends JavaPlugin {
    private static JavaPlugin plugin;

    public static JavaPlugin getPlugin() {
        return plugin;
    }

    public static Logger getInstancedLogger() {
        return plugin.getLogger();
    }
    public vicky_utils utils = (vicky_utils) getServer().getPluginManager().getPlugin("Vicky-s_Utilities");


    @Override
    public void onLoad() {
        if (utils != null) {
            vicky_utils.registerTemplatePackage("VSPE-0.0.1-ARI", "org.vicky.vspe.utilities.DBTemplates");
        } else {
            getLogger().severe("Vicky Utilities is missing... not found!");
        }
        dimensionManager = new DimensionManager();
        dimensionManager.processDimensionGenerators(false);
    }

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null
                && Bukkit.getPluginManager().getPlugin("MythicMobs") != null
                && Bukkit.getPluginManager().getPlugin("ItemsAdder") != null
                && Bukkit.getPluginManager().getPlugin("Terra") != null
                && Bukkit.getPluginManager().getPlugin("Vicky-s_Utilities") != null) {

            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            plugin = this;

            MultiverseCore core = (MultiverseCore) Bukkit.getServer().getPluginManager().getPlugin("Multiverse-Core");
            assert core != null;
            MultiverseInventories inventories = (MultiverseInventories) Bukkit.getServer().getPluginManager().getPlugin("Multiverse-Inventories");
            assert inventories != null;
            MultiversePortals portals = (MultiversePortals) Bukkit.getServer().getPluginManager().getPlugin("Multiverse-Portals");
            assert portals != null;
            if (utils != null) {
                sqlManager = utils.getSQLManager();
                databaseManager = utils.getDatabaseManager();
            }else {
                throw new RuntimeException("Plugin Vicky Utilities cannot be found on motherboard?");
            }

            worldManager = core.getMVWorldManager();

            getLogger().info(ANSIColor.colorize("cyan[Starting up VickyE's Survival Plus Essentials]"));
            vicky_utils.hookDependantPlugin(this);

            System.setProperty(
                    "javax.xml.bind.context.factory",
                    "org.vicky.shaded.org.glassfish.jaxb.runtime.v2.ContextFactory"
            );

            dimensionManager.processDimensions();
            configManager = new ConfigManager(this, "config.yml");
            Config config = new Config(this);
            config.registerConfigs();

            CommandAPICommand regenerate = new CommandAPICommand("regenerateGenerators")
                    .withArguments(new BooleanArgument("clean"))
                    .executes(executionInfo -> {
                        executionInfo.sender().sendMessage(ANSIColor.colorize("Regenerating Generators... Please yellow[Standby] :D"));

                        Boolean clean = (Boolean) executionInfo.args().get("clean");
                        dimensionManager.processDimensionGenerators(Boolean.FALSE.equals(clean));
                    });
            CommandAPICommand refresh = new CommandAPICommand("refreshAdvancements")
                    .executes(executionInfo -> {
                        executionInfo.sender().sendMessage(ANSIColor.colorize("Refreshing Advancements... Please yellow[Standby] :D"));

                        try {
                            advancementManager.processAdvancements();
                        } catch (AdvancementProcessingFailureException e) {
                            executionInfo.sender().sendMessage("Error occurred while refreshing advancements: " + e.getMessage() + ", Please contact a developer on this issue..");
                        }
                    });
            List<Argument<?>> arguments = new ArrayList<>();
            arguments.add(new StringArgument("system").replaceSuggestions(ArgumentSuggestions.strings(info -> ManagerRegistry.getRegisteredManagers())));
            arguments.add(new StringArgument("identifier").replaceSuggestions(ArgumentSuggestions.strings(info -> {
                String system = (String) info.previousArgs().get("system");

                IdentifiableManager manager = ManagerRegistry.getManager(system);
                Collection<String> entities = manager.getRegisteredEntities()
                        .stream()
                        .map(Identifiable::getIdentifier)
                        .toList();

                return entities.toArray(new String[0]);
            })));
            CommandAPICommand remove = new CommandAPICommand("remove")
                    .withArguments(arguments)
                    .executes(executionInfo -> {
                        try {
                            String id = (String) executionInfo.args().get("identifier");
                            String system = (String) executionInfo.args().get("system");
                            ManagerRegistry.getManager(system).removeEntity(id);
                            executionInfo.sender().sendMessage(Component.text("Successfully removed " + executionInfo.args().get("identifier") + " from " + executionInfo.args().get("system")).color(TextColor.fromHexString("#00aa00")));
                        } catch (EntityNotFoundException e) {
                            executionInfo.sender().sendMessage("Error occurred while while removing system entity: " + e.getMessage() + ", Please contact a developer on this issue..");

                        }
                    });
            CommandAPICommand disable = new CommandAPICommand("disable")
                    .withArguments(arguments)
                    .executes(executionInfo -> {
                        try {
                            String id = (String) executionInfo.args().get("identifier");
                            String system = (String) executionInfo.args().get("system");
                            ManagerRegistry.getManager(system).disableEntity(id);
                            executionInfo.sender().sendMessage(Component.text("Successfully disabled " + executionInfo.args().get("identifier") + " from " + executionInfo.args().get("system")).color(TextColor.fromHexString("#00aa00")));
                        } catch (EntityNotFoundException e) {
                            executionInfo.sender().sendMessage("Error occurred while while disabling system entity: " + e.getMessage() + ", Please contact a developer on this issue..");
                        }
                    });
            List<Argument<?>> enableArguments = new ArrayList<>();
            enableArguments.add(new StringArgument("system").replaceSuggestions(ArgumentSuggestions.strings(info -> ManagerRegistry.getRegisteredManagers())));
            enableArguments.add(new StringArgument("identifier").replaceSuggestions(ArgumentSuggestions.strings(info -> {
                String system = (String) info.previousArgs().get("system");

                IdentifiableManager manager = ManagerRegistry.getManager(system);
                Collection<String> entities = manager.getUnregisteredEntities()
                        .stream()
                        .map(Identifiable::getIdentifier)
                        .toList();

                return entities.toArray(new String[0]);
            })));
            CommandAPICommand enable = new CommandAPICommand("enable")
                    .withArguments(enableArguments)
                    .withHelp("", "")
                    .executes(executionInfo -> {
                        try {
                            String id = (String) executionInfo.args().get("identifier");
                            String system = (String) executionInfo.args().get("system");
                            ManagerRegistry.getManager(system).enableEntity(id);
                            executionInfo.sender().sendMessage(Component.text("Successfully enabled " + executionInfo.args().get("identifier") + " from " + executionInfo.args().get("system")).color(TextColor.fromHexString("#00aa00")));
                        } catch (EntityNotFoundException e) {
                            executionInfo.sender().sendMessage("Error occurred while enabling system entity: " + e.getMessage() + ", Please contact a developer on this issue..");
                        }
                    });
            new CommandAPICommand("vspe")
                    .withSubcommand(regenerate)
                    .withSubcommand(refresh)
                    .withSubcommand(remove)
                    .withSubcommand(disable)
                    .withSubcommand(enable)
                    .register(this);

            advancementManager = new AdvancementManager();
            try {
                advancementManager.processAdvancements();
            } catch (AdvancementProcessingFailureException e) {
                throw new RuntimeException(e);
            }
            advancementManager.loadManagerProgress();

            Bukkit.getScheduler().runTaskTimer(this, () -> {
                getLogger().info(ANSIColor.colorize("cyan[Saving advancement and progress]"));
                advancementManager.saveAndUnloadManagerProgress();
            }, 10200L, 10200L);

            getServer().getPluginManager().registerEvents(new GlobalListeners(), this);

        } else if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().severe(ANSIColor.colorize("red[Could not find PlaceholderAPI! This plugin is required.]"));
            Bukkit.getPluginManager().disablePlugin(this);
        } else if (Bukkit.getPluginManager().getPlugin("MythicMobs") == null) {
            getLogger().severe(ANSIColor.colorize("red[Could not find MythicMobs! This plugin is required.]"));
            Bukkit.getPluginManager().disablePlugin(this);
        } else if (Bukkit.getPluginManager().getPlugin("ItemsAdder") == null) {
            getLogger().severe(ANSIColor.colorize("red[Could not find ItemsAdder! This plugin is required.]"));
            Bukkit.getPluginManager().disablePlugin(this);
        } else if (Bukkit.getPluginManager().getPlugin("Terra") == null) {
            getLogger().severe(ANSIColor.colorize("red[Could not find Terra! This plugin is required.]"));
            Bukkit.getPluginManager().disablePlugin(this);
        } else if (Bukkit.getPluginManager().getPlugin("Vicky-s_Utilities") == null) {
            getLogger().severe(ANSIColor.colorize("red[Could not find High Priority plugin VickyE's Utilities! This plugin is NECESSARY. Get it-]"));
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (advancementManager != null)
            advancementManager.saveManagerProgress();
    }
}