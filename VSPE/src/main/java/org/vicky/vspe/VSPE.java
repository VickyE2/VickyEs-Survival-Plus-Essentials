package org.vicky.vspe;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiversePortals.MultiversePortals;
import com.onarandombox.multiverseinventories.MultiverseInventories;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.BooleanArgument;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.vicky.utilities.ANSIColor;
import org.vicky.utilities.ConfigManager;
import org.vicky.vicky_utils;
import org.vicky.vspe.addon.util.BaseStructure;
import org.vicky.vspe.features.AdvancementPlus.AdvancementManager;
import org.vicky.vspe.systems.Dimension.DimensionManager;
import org.vicky.vspe.utilities.Config;
import org.vicky.vspe.utilities.DatabaseManager.HibernateDatabaseManager;
import org.vicky.vspe.utilities.DatabaseManager.SQLManager;
import org.vicky.vspe.utilities.global.GlobalListeners;

import java.util.ArrayList;
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


    @Override
    public void onLoad() {
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

            new CommandAPICommand("regenerateGenerators")
                    .withArguments(new BooleanArgument("clean"))
                    .executes(executionInfo -> {
                        executionInfo.sender().sendMessage(ANSIColor.colorize("Regenerating Generators... Please yellow[Standby] :D"));

                        Boolean clean = (Boolean) executionInfo.args().get("clean");
                        dimensionManager.processDimensionGenerators(Boolean.FALSE.equals(clean));
                    }).register(this);


            worldManager = core.getMVWorldManager();

            getLogger().info(ANSIColor.colorize("cyan[Starting up VickyE's Survival Plus Essentials]"));
            vicky_utils.hookDependantPlugin(this);

            System.setProperty(
                    "javax.xml.bind.context.factory",
                    "org.vicky.shaded.org.glassfish.jaxb.runtime.v2.ContextFactory"
            );

            try {
                sqlManager = new SQLManager();
                sqlManager.initialize(this);
                sqlManager.createDatabase();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            configManager = new ConfigManager(this, "config.yml");
            databaseManager = new HibernateDatabaseManager();

            Config config = new Config(this);
            config.registerConfigs();

            advancementManager = new AdvancementManager();
            advancementManager.processAdvancements();
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
        this.getLogger().info("Disabling [ VSPE ]");
        advancementManager.saveManagerProgress();
    }
}