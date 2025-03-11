package org.vicky.vspe;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiversePortals.MultiversePortals;
import com.onarandombox.multiverseinventories.MultiverseInventories;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.vicky.utilities.ANSIColor;
import org.vicky.utilities.ConfigManager;
import org.vicky.utilities.FileManager;
import org.vicky.utilities.Identifiable;
import org.vicky.vicky_utils;
import org.vicky.vspe.features.AdvancementPlus.AdvancementManager;
import org.vicky.vspe.features.AdvancementPlus.Exceptions.AdvancementProcessingFailureException;
import org.vicky.vspe.features.CharmsAndTrinkets.BaseTrinket;
import org.vicky.vspe.features.CharmsAndTrinkets.CnTManager;
import org.vicky.vspe.features.CharmsAndTrinkets.exceptions.TrinketProcessingFailureException;
import org.vicky.vspe.features.CharmsAndTrinkets.gui.CharnsNTrinkets.PlayerEquippedTrinketScreen;
import org.vicky.vspe.features.CharmsAndTrinkets.gui.CharnsNTrinkets.PlayerEquippedTrinketScreenListener;
import org.vicky.vspe.systems.Dimension.DimensionManager;
import org.vicky.vspe.systems.Dimension.DimensionsGUI;
import org.vicky.vspe.systems.Dimension.DimensionsGUIListener;
import org.vicky.vspe.utilities.Config;
import org.vicky.vspe.utilities.Manager.EntityNotFoundException;
import org.vicky.vspe.utilities.Manager.IdentifiableManager;
import org.vicky.vspe.utilities.Manager.ManagerNotFoundException;
import org.vicky.vspe.utilities.Manager.ManagerRegistry;
import org.vicky.vspe.utilities.global.GlobalListeners;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import static org.vicky.vspe.utilities.global.GlobalResources.*;

public final class VSPE extends JavaPlugin implements Listener {
    private static JavaPlugin plugin;
    private vicky_utils utils = (vicky_utils) getServer().getPluginManager().getPlugin("Vicky-s_Utilities");

    public static JavaPlugin getPlugin() {
        return plugin;
    }

    public static Logger getInstancedLogger() {
        return plugin.getLogger();
    }

    @Override
    public void onLoad() {
        if (utils != null) {
            vicky_utils.registerTemplatePackage("VSPE-0.0.1-ARI", "org.vicky.vspe.utilities.Hibernate.DBTemplates");
            vicky_utils.hookDependantPlugin(this);
            vicky_utils.addClassLoader(this.getClassLoader());
        } else {
            getLogger().severe("Vicky Utilities is missing... not found!");
        }
        dimensionManager = new DimensionManager();
        dimensionsGUIListener = new DimensionsGUIListener(this);
        dimensionManager.processDimensionGenerators(false);
    }

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null
                && Bukkit.getPluginManager().getPlugin("MythicMobs") != null
                && Bukkit.getPluginManager().getPlugin("ItemsAdder") != null
                && Bukkit.getPluginManager().getPlugin("Terra") != null
                && Bukkit.getPluginManager().getPlugin("Vicky-s_Utilities") != null) {

            plugin = this;
            classLoader.getLoaders().add(this.getClassLoader());
            Thread.currentThread().setContextClassLoader(classLoader);

            Bukkit.getPluginManager().registerEvents(this, this);
            trinketManager = new CnTManager(this);

            FileManager fileManager = new FileManager(this);
            List<String> files = List.of("contents/vspe_trinkets/", "contents/vspe_dimensions/");
            fileManager.extractDefaultIAAssets(files);

            MultiverseCore core = (MultiverseCore) Bukkit.getServer().getPluginManager().getPlugin("Multiverse-Core");
            assert core != null;
            MultiverseInventories inventories = (MultiverseInventories) Bukkit.getServer().getPluginManager().getPlugin("Multiverse-Inventories");
            assert inventories != null;
            MultiversePortals portals = (MultiversePortals) Bukkit.getServer().getPluginManager().getPlugin("Multiverse-Portals");
            assert portals != null;
            if (utils != null) {
                sqlManager = utils.getSQLManager();
                databaseManager = utils.getDatabaseManager();
            } else {
                throw new RuntimeException("Plugin Vicky Utilities cannot be found on motherboard?");
            }

            worldManager = core.getMVWorldManager();
            trinketScreenListener = new PlayerEquippedTrinketScreenListener(this);

            getLogger().info(ANSIColor.colorize("cyan[Starting up VickyE's Survival Plus Essentials]"));

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

                Optional<IdentifiableManager> oM = ManagerRegistry.getManager(system);
                if (oM.isEmpty()) {
                    try {
                        throw new ManagerNotFoundException("Failed to locate manager in registered managers");
                    } catch (ManagerNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
                IdentifiableManager manager = oM.get();
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
                            ManagerRegistry.getManager(system).get().removeEntity(id);
                            executionInfo.sender().sendMessage(Component.text("Successfully removed " + executionInfo.args().get("identifier") + " from " + executionInfo.args().get("system")).color(TextColor.fromHexString("#00aa00")));
                        }
                        catch (NoSuchElementException e) {
                            try {
                                executionInfo.sender().sendMessage("That manager does not exist");
                                throw new ManagerNotFoundException("Failed to locate manager in registered managers");
                            } catch (ManagerNotFoundException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                        catch (EntityNotFoundException e) {
                            executionInfo.sender().sendMessage("Error occurred while while removing system entity: " + e.getMessage() + ", Please contact a developer on this issue..");
                        }
                    });
            CommandAPICommand disable = new CommandAPICommand("disable")
                    .withArguments(arguments)
                    .executes(executionInfo -> {
                        try {
                            String id = (String) executionInfo.args().get("identifier");
                            String system = (String) executionInfo.args().get("system");
                            ManagerRegistry.getManager(system).get().disableEntity(id);
                            executionInfo.sender().sendMessage(Component.text("Successfully disabled " + executionInfo.args().get("identifier") + " from " + executionInfo.args().get("system")).color(TextColor.fromHexString("#00aa00")));
                        }
                        catch (NoSuchElementException e) {
                            try {
                                executionInfo.sender().sendMessage("That manager does not exist");
                                throw new ManagerNotFoundException("Failed to locate manager in registered managers");
                            } catch (ManagerNotFoundException ex) {
                                throw new RuntimeException(ex);
                            }
                        } catch (EntityNotFoundException e) {
                            executionInfo.sender().sendMessage("Error occurred while while disabling system entity: " + e.getMessage() + ", Please contact a developer on this issue..");
                        }
                    });
            List<Argument<?>> enableArguments = new ArrayList<>();
            enableArguments.add(new StringArgument("system").replaceSuggestions(ArgumentSuggestions.strings(info -> ManagerRegistry.getRegisteredManagers())));
            enableArguments.add(new StringArgument("identifier").replaceSuggestions(ArgumentSuggestions.strings(info -> {
                String system = (String) info.previousArgs().get("system");

                Optional<IdentifiableManager> oM = ManagerRegistry.getManager(system);
                if (oM.isEmpty()) {
                    try {
                        throw new ManagerNotFoundException("Failed to locate manager in registered managers");
                    } catch (ManagerNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
                IdentifiableManager manager = oM.get();
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
                            ManagerRegistry.getManager(system).get().enableEntity(id);
                            executionInfo.sender().sendMessage(Component.text("Successfully enabled " + executionInfo.args().get("identifier") + " from " + executionInfo.args().get("system")).color(TextColor.fromHexString("#00aa00")));
                        }
                        catch (NoSuchElementException e) {
                            try {
                                executionInfo.sender().sendMessage("That manager does not exist");
                                throw new ManagerNotFoundException("Failed to locate manager in registered managers");
                            } catch (ManagerNotFoundException ex) {
                                throw new RuntimeException(ex);
                            }
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

            new CommandAPICommand("manage_trinket")
                    .withAliases("mt", "mtk")
                    .withFullDescription("Opens the gui for the user to manage his equipped trinkets")
                    .executesPlayer(executionInfo -> {
                        new PlayerEquippedTrinketScreen(this)
                                .showGui(executionInfo.sender());
                    })
                    .register(this);

            List<Argument<?>> trinkets = new ArrayList<>();
            trinkets.add(new StringArgument("trinket")
                    .replaceSuggestions(ArgumentSuggestions.strings(info ->
                            trinketManager.getRegisteredEntities().stream()
                                    .filter(entity -> entity instanceof BaseTrinket)
                                    .map(trinket -> ((BaseTrinket) trinket).getFormattedName())
                                    .toArray(String[]::new)
                    )));
            new CommandAPICommand("get_trinket")
                    .withAliases("gtk", "gt")
                    .withPermission("vspe_admin")
                    .withArguments(trinkets)
                    .withFullDescription("Gives the player the specified trinket.")
                    .executesPlayer(executionInfo -> {
                        trinketManager.givePlayerTrinket(executionInfo.sender(), (String) executionInfo.args().get("trinket"));
                    })
                    .register(this);
            new CommandAPICommand("dimensions")
                    .withAliases("aD", "gAD")
                    .withFullDescription("Opens up the dimensions gui for the player")
                    .executesPlayer(executionInfo -> {
                        new DimensionsGUI().showGui(executionInfo.sender());
                    })
                    .register(this);

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

    @EventHandler
    public void onItemsAdderLoaded(ItemsAdderLoadDataEvent event) {
        if (event.getCause() != ItemsAdderLoadDataEvent.Cause.FIRST_LOAD) return;
        try {
            advancementManager = new AdvancementManager(this);
            try {
                advancementManager.processAdvancements();
            } catch (AdvancementProcessingFailureException e) {
                throw new RuntimeException(e);
            }
            advancementManager.loadManagerProgress();
            dimensionManager.processDimensions();
            trinketManager.processTrinkets();
            Bukkit.getScheduler().runTaskTimer(this, () -> {
                getLogger().info(ANSIColor.colorize("cyan[Saving advancement and progress]"));
                advancementManager.saveAndUnloadManagerProgress();
            }, 10200L, 10200L);
        } catch (TrinketProcessingFailureException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDisable() {
        if (advancementManager != null)
            advancementManager.saveManagerProgress();
    }

    public void extractFolderFromJar(String folderPath, File destinationFolder) {
        if (!destinationFolder.exists()) {
            destinationFolder.mkdirs(); // Create the destination folder if it doesn't exist
        }

        try {
            getLogger()
                    .info(
                            ANSIColor.colorize(
                                    "orange[Extracting folder:] yellow["
                                            + folderPath
                                            + "] orange[to] yellow["
                                            + destinationFolder
                                            + "]"));
            URL jarUrl = getClass().getProtectionDomain().getCodeSource().getLocation();
            JarFile jarFile = new JarFile(new File(jarUrl.toURI()));

            Enumeration<JarEntry> entries = jarFile.entries();
            boolean foundEntries = false;

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                // Only process entries that are within the specified folder
                if (entry.getName().startsWith(folderPath + "/") && !entry.isDirectory()) {
                    foundEntries = true; // Mark that we found at least one entry
                    File destFile =
                            new File(
                                    destinationFolder,
                                    entry.getName().substring(folderPath.length() + 1)); // Adjust index for the slash

                    File parent = destFile.getParentFile();
                    if (!parent.exists()) {
                        parent.mkdirs(); // Create parent directories if needed
                    }

                    // Copy the file from the JAR to the destination
                    try (InputStream is = jarFile.getInputStream(entry);
                         FileOutputStream fos = new FileOutputStream(destFile)) {

                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = is.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
            }

            if (!foundEntries) {
                getLogger().info("No entries found in the JAR file for: " + folderPath);
            }

            jarFile.close();
        } catch (Exception e) {
            getLogger().severe("Failed to extract folder from JAR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}