package org.vicky.vspe;

import com.google.gson.JsonObject;
import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiversePortals.MultiversePortals;
import com.onarandombox.multiverseinventories.MultiverseInventories;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vicky.bukkitplatform.useables.BukkitLocationAdapter;
import org.vicky.bukkitplatform.useables.BukkitWorldAdapter;
import org.vicky.ecosystem.server.CommunicatorServer;
import org.vicky.platform.PlatformConfig;
import org.vicky.platform.PlatformLogger;
import org.vicky.platform.PlatformScheduler;
import org.vicky.utilities.*;
import org.vicky.vicky_utils;
import org.vicky.vspe.ecosystem.VSPECommunicateableImpl;
import org.vicky.vspe.features.AdvancementPlus.AdvancementManager;
import org.vicky.vspe.features.AdvancementPlus.BukkitAdvancement;
import org.vicky.vspe.features.CharmsAndTrinkets.BaseTrinket;
import org.vicky.vspe.features.CharmsAndTrinkets.CnTManager;
import org.vicky.vspe.features.CharmsAndTrinkets.gui.CharnsNTrinkets.PlayerEquippedTrinketScreen;
import org.vicky.vspe.features.CharmsAndTrinkets.gui.CharnsNTrinkets.PlayerEquippedTrinketScreenListener;
import org.vicky.vspe.nms.impl.NMSInjectListener;
import org.vicky.vspe.paper.*;
import org.vicky.vspe.platform.PlatformBiomeFactory;
import org.vicky.vspe.platform.PlatformBlockDataRegistry;
import org.vicky.vspe.platform.PlatformStructureManager;
import org.vicky.vspe.platform.VSPEPlatformPlugin;
import org.vicky.vspe.platform.features.CharmsAndTrinkets.PlatformTrinketManager;
import org.vicky.vspe.platform.features.CharmsAndTrinkets.exceptions.TrinketProcessingFailureException;
import org.vicky.vspe.platform.features.advancement.Exceptions.AdvancementProcessingFailureException;
import org.vicky.vspe.platform.features.advancement.PlatformAdvancementManager;
import org.vicky.vspe.platform.systems.dimension.CoreDimensionRegistry;
import org.vicky.vspe.platform.systems.dimension.DimensionDescriptor;
import org.vicky.vspe.platform.systems.dimension.Exceptions.NullManagerDimension;
import org.vicky.vspe.platform.systems.dimension.PlatformDimensionManager;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.StructureCacheUtils;
import org.vicky.vspe.platform.systems.platformquestingintegration.QuestProductionFactory;
import org.vicky.vspe.platform.utilities.Manager.EntityNotFoundException;
import org.vicky.vspe.platform.utilities.Manager.IdentifiableManager;
import org.vicky.vspe.platform.utilities.Manager.ManagerNotFoundException;
import org.vicky.vspe.platform.utilities.Manager.ManagerRegistry;
import org.vicky.vspe.structure_gen.*;
import org.vicky.vspe.systems.dimension.*;
import org.vicky.vspe.utilities.Config;
import org.vicky.vspe.utilities.Hibernate.DBTemplates.*;
import org.vicky.vspe.utilities.global.GlobalListeners;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import static org.vicky.vspe.systems.dimension.VSPEBukkitDimensionManager.prepareGenerators;
import static org.vicky.vspe.utilities.global.GlobalResources.*;

public final class VSPE extends JavaPlugin implements Listener, VSPEPlatformPlugin {
    private static JavaPlugin plugin;
    private final vicky_utils utils = (vicky_utils) getServer().getPluginManager().getPlugin("Vicky-s_Utilities");

    public static JavaPlugin getPlugin() {
        return plugin;
    }

    public static Logger getInstancedLogger() {
        return plugin.getLogger();
    }

    @Override
    public void onLoad() {
        plugin = this;
        VSPEPlatformPlugin.set(this);
        if (utils != null) {
            vicky_utils.addTemplateClasses(
                    AdvanceablePlayer.class,
                    Advancement.class,
                    AvailableTrinket.class,
                    CnTPlayer.class,
                    Dimension.class
            );
            pluginCommunicator = CommunicatorServer.getInstance()
                    .register("VSPE", getClassLoader(), new VSPECommunicateableImpl());
        } else {
            getLogger().severe("Vicky Utilities is missing... not found!");
        }
        var obj = new JsonObject();
                obj.addProperty("plugin", getName()
                        .replace("[", "")
                        .replace("]", "")
                );
        CommunicatorServer.getInstance()
                .sendAsync("Vicky-s_Utilities", "test", obj);
        dimensionManager = new VSPEBukkitDimensionManager();
        dimensionsGUIListener = new DimensionsGUIListener(this);
        dimensionManager.processDimensionGenerators(false);
        kraterosGenerationEngine = new KraterosGenerationEngine(this);
    }

    @Override
    public void onEnable() {
        prepareGenerators();
        CoreDimensionRegistry.installInto(this);
        getServer().getPluginManager().registerEvents(new NMSInjectListener(), this);
        Bukkit.getPluginManager().registerEvents(this, this);
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null
                && Bukkit.getPluginManager().getPlugin("MythicMobs") != null
                && Bukkit.getPluginManager().getPlugin("ItemsAdder") != null
                && Bukkit.getPluginManager().getPlugin("Terra") != null
                && Bukkit.getPluginManager().getPlugin("Vicky-s_Utilities") != null) {
            classLoader.getLoaders().add(this.getClassLoader());
            Thread.currentThread().setContextClassLoader(classLoader);

            trinketManager = new CnTManager(this);

            FileManager fileManager = new FileManager(this);
            List<String> files = List.of("contents/vspe_trinkets/", "contents/vspe_dimensions/");
            fileManager.extractDefaultIAAssets(files);
            extractFolderFromJar("structure_packs", new File(getDataFolder() + "/structure_packs/"));

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

            configManager = new ConfigManager(new File(getDataFolder() ,"config.yml"));
            Config config = new Config(this);
            config.registerConfigs();
            registerCommands();
            kraterosGenerationEngine.generateStructurePacks();
            getServer().getPluginManager().registerEvents(new GlobalListeners(), this);

            dimensionManager.processDimensions();
            processPendingDimensions();

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

    private void registerCommands() {
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
        List<Argument<?>> managerArgumentList = new ArrayList<>();
        managerArgumentList.add(new StringArgument("system").replaceSuggestions(ArgumentSuggestions.strings(info -> ManagerRegistry.getRegisteredManagersArray())));
        managerArgumentList.add(new StringArgument("identifier").replaceSuggestions(ArgumentSuggestions.strings(info -> {
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
                .withArguments(managerArgumentList)
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
                .withArguments(managerArgumentList)
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

        StructureCacheUtils.getDefaultOffsets(100);

        enableArguments.add(new StringArgument("system").replaceSuggestions(ArgumentSuggestions.strings(info -> ManagerRegistry.getRegisteredManagersArray())));
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
        List<Argument<?>> dimensionArguments = new ArrayList<>();
        dimensionArguments.add(new StringArgument("action").replaceSuggestions(ArgumentSuggestions.strings(info -> List.of("gui", "set").toArray(String[]::new))));
        dimensionArguments.add(new StringArgument("identifier").replaceSuggestions(ArgumentSuggestions.strings(info -> {
            String action = (String) info.previousArgs().get("action");
            if (action == null) return new String[0];
            if (action.equals("set")) {
                Optional<VSPEBukkitDimensionManager> oM = ManagerRegistry.getManager(VSPEBukkitDimensionManager.class);
                if (oM.isEmpty()) {
                    try {
                        throw new ManagerNotFoundException("Failed to locate dimension manager in registered managers");
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
            }
            return new String[0];
        })).setOptional(true));
        dimensionArguments.add(new StringArgument("attribute").replaceSuggestions(ArgumentSuggestions.strings(info -> {
            String action = (String) info.previousArgs().get("identifier");
            if (action == null) {
                return new String[0];
            }
            else {
                return List.of("world").toArray(new String[0]);
            }
        })).setOptional(true));
        dimensionArguments.add(new StringArgument("value").replaceSuggestions(ArgumentSuggestions.strings(info -> {
            String attribute = (String) info.previousArgs().get("attribute");
            if (attribute == null) return new String[0];
            if (attribute.equals("world")) {
                return List.of("true", "false").toArray(String[]::new);
            }
            return new String[0];
        })).setOptional(true));
        new CommandAPICommand("dimensions")
                .withAliases("dim", "vDim")
                .withFullDescription("A command to do various things on a dimension...")
                .withArguments(dimensionArguments)
                .executes(executionInfo -> {
                    String action = (String) executionInfo.args().get("action");
                    String identifier = (String) executionInfo.args().get("identifier");
                    String attribute = (String) executionInfo.args().get("attribute");
                    String value = (String) executionInfo.args().get("value");

                    if (action == null) {
                        executionInfo.sender().sendMessage(Component.text("Action CANNOT be null :D", TextColor.color(225, 0, 0), TextDecoration.BOLD, TextDecoration.ITALIC));
                        String x = "9";
                        Integer.valueOf(x);
                        byte[] w = x.getBytes();
                        return;
                    }
                    if (action.equals("gui"))
                        if (executionInfo.sender() instanceof Player)
                            new DimensionsGUI().showGui((Player) executionInfo.sender());
                        else
                            executionInfo.sender().sendMessage(Component.text("You need to be a player to do that :D", TextColor.color(225, 0, 0), TextDecoration.BOLD, TextDecoration.ITALIC));
                    else if (action.equals("set")) {
                        if (executionInfo.sender().isOp())
                            if (identifier != null) {
                                Optional<BukkitBaseDimension> oBD = dimensionManager.LOADED_DIMENSIONS.stream().map(BukkitBaseDimension.class::cast).filter(k -> k.getIdentifier().equals(identifier)).findAny();
                                if (oBD.isPresent()) {
                                    BukkitBaseDimension dimension = oBD.get();
                                    if (attribute != null) {
                                        if (attribute.equals("world")) {
                                            if (value != null && (value.equals("true") || value.equals("false"))) {
                                                configManager.setBracedConfigValue("Dimensions." + dimension.getName() + ".exists", value, null);
                                            }
                                            else {
                                                executionInfo.sender().sendMessage(Component.text("Value CANNOT be null and must be true or false :D", TextColor.color(225, 0, 0), TextDecoration.BOLD, TextDecoration.ITALIC));
                                            }
                                        }
                                    }
                                    else {
                                        executionInfo.sender().sendMessage(Component.text("Attribute CANNOT be null :D", TextColor.color(225, 0, 0), TextDecoration.BOLD, TextDecoration.ITALIC));
                                    }
                                }
                                else {
                                    try {
                                        throw new NullManagerDimension("Cannot find id on command operation", identifier);
                                    } catch (NullManagerDimension e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            }
                            else {
                                executionInfo.sender().sendMessage(Component.text("Identifier CANNOT be null :D", TextColor.color(225, 0, 0), TextDecoration.BOLD, TextDecoration.ITALIC));
                            }
                        else
                            executionInfo.sender().sendMessage(Component.text("You need to be an op to do that", TextColor.color(225, 0, 0), TextDecoration.BOLD, TextDecoration.ITALIC));
                    }
                })
                .register(this);

        List<Argument<?>> cityConfigArgs = new ArrayList<>();
        cityConfigArgs.add(new StringArgument("packName").replaceSuggestions(ArgumentSuggestions.strings(info -> kraterosGenerationEngine.getLoadedStructures().stream().map(StructurePack::getName).toArray(String[]::new))));
        cityConfigArgs.add(new LocationArgument("generationPoint"));
        cityConfigArgs.add(new StringArgument("roadStrategy").replaceSuggestions(ArgumentSuggestions.strings(info -> List.of("").toArray(String[]::new))));
        cityConfigArgs.addAll(List.of(
                new CustomArgument<>(new StringArgument("cityType"),
                        info -> CityType.valueOf(info.input())),
                new CustomArgument<>(new StringArgument("cityShape"), info -> {
                    String input = info.input().toLowerCase();
                    return switch (input) {
                        case "circle" -> Shape.CIRCLE.INSTANCE;
                        case "rect" -> Shape.RECT.INSTANCE;
                        case "star" -> Shape.STAR.INSTANCE;
                        case "triangle" -> Shape.TRIANGLE.INSTANCE;
                        default -> {
                            if (input.startsWith("polygon:")) {
                                int sides = Integer.parseInt(input.split(":")[1]);
                                yield new Shape.POLYGON(sides);
                            }
                            throw CustomArgument.CustomArgumentException.fromString("Invalid shape format");
                        }
                    };
                }),
                new CustomArgument<>(new StringArgument("arrangement"),
                        info -> ArrangementType.valueOf(info.input())),
                new FloatArgument("density", 0.1f, 2.0f),
                new FloatArgument("heightRatio", 0.1f, 3.0f),
                new IntegerArgument("buildingMaxSpacing", 1, 50),
                new IntegerArgument("buildingMinSpacing", 0, 49),
                new IntegerArgument("buildingPadding", 0, 20),
                new IntegerArgument("roadPadding", 0, 20),
                new BooleanArgument("hasTownHall").setOptional(true),
                new IntegerArgument("townHallSpacing").setOptional(true)
        ));

        new CommandAPICommand("generateCity")
                .withArguments(cityConfigArgs)
                .executesPlayer((player, args) -> {
                    String packName = (String) args.get("packName");
                    Location location = (Location) args.get("generationPoint");
                    StructurePack found = null;
                    CityType cityType = (CityType) args.get("cityType");
                    Shape shape = (Shape) args.get("shape");
                    ArrangementType arrangement = (ArrangementType) args.get("arrangement");
                    boolean hasTownHall = (boolean) args.get("hasTownHall");
                    float density = (float) args.get("density");
                    float heightRatio = (float) args.get("heightRatio");
                    Integer townHallSpacing = (Integer) args.getOptional("townHallSpacing").orElse(null);
                    int buildingMaxSpacing = (int) args.get("buildingMaxSpacing");
                    int buildingMinSpacing = (int) args.get("buildingMinSpacing");
                    int buildingPadding = (int) args.get("buildingPadding");
                    int roadPadding = (int) args.get("roadPadding");
                    for (StructurePack pack : kraterosGenerationEngine.getLoadedStructures()) {
                        if (pack.getName().equals(packName)) {
                            found = pack;
                            break;
                        }
                    }
                    KraterBukkitCityGenerator generatorKt = new KraterBukkitCityGenerator(
                            new CityConfig(
                                    cityType, shape, hasTownHall,
                                    arrangement, density, heightRatio, townHallSpacing,
                                    buildingMaxSpacing, buildingMinSpacing,
                                    buildingPadding, roadPadding
                            ),
                            new BukkitWorldAdapter(player.getWorld()),
                            found,
                            BukkitLocationAdapter.from(location),
                            new DistributedRoadLayoutEngine(RoadLayoutStrategy.Centered.INSTANCE),
                            true,
                            true,
                            RoadLayoutEngineKt.getDefaultRoadPalettes().get(3)
                    );

                    generatorKt.buildCity();
                })
                .register(this);
    }

    @EventHandler
    public void onItemsAdderLoaded(ItemsAdderLoadDataEvent event) {
        if (event.getCause() != ItemsAdderLoadDataEvent.Cause.FIRST_LOAD) return;
        advancementManager = new AdvancementManager(this);
        try {
            advancementManager.processAdvancements();
        } catch (AdvancementProcessingFailureException e) {
            throw new RuntimeException(e);
        }
        advancementManager.loadManagerProgress();
        try {
            trinketManager.processTrinkets();
        } catch (TrinketProcessingFailureException e) {
            throw new RuntimeException(e);
        }
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            getLogger().info(ANSIColor.colorize("cyan[Saving advancement and progress]"));
            advancementManager.saveAndUnloadManagerProgress();
        }, 10200L, 10200L);
        Bukkit.getScheduler().runTaskTimer(this, DimensionTickLoop.INSTANCE, 1L, 20L);
    }

    @Override
    public void onDisable() {
        // vicky_utils.unhookDependantPlugin(this);
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

    @Override
    public @Nullable ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        var gen = VSPEBukkitDimensionManager.GENERATORS.get(id.replace("VSPE:", "").toUpperCase());
        if (gen == null) {
            getInstancedLogger().warning("Generator " + id + " is not registered with the dimension manager. This could be an error.");
        }
        return gen;
    }

    @Override
    public void registerDimensionDescriptor(DimensionDescriptor dimensionDescriptor) {
        getInstancedLogger().info("Added descriptor " + dimensionDescriptor.name());
        VSPEBukkitDimensionManager.DIMENSION_DESCRIPTOR_SET.add(dimensionDescriptor);
    }

    @Override
    public void processPendingDimensions() {
        dimensionManager.loadDimensionsFromDescriptors();
    }

    @Override
    public PlatformScheduler getPlatformScheduler() {
        return new VSPEBukkitPlatformScheduler();
    }

    @Override
    public PlatformStructureManager<BlockData> getPlatformStructureManager() {
        return new VSPEBukkitStructureManager();
    }

    @Override
    public PlatformBlockDataRegistry<?> getPlatformBlockDataRegistry() {
        return new VSPEBukkitBlockDataRegistry();
    }

    @Override
    public PlatformConfig getPlatformConfig() {
        return new PlatformConfig() {
            @Override
            public boolean getBooleanValue(String s) {
                return configManager.getBooleanValue(s);
            }

            @Override
            public String getStringValue(String s) {
                return configManager.getStringValue(s);
            }

            @Override
            public Integer getIntegerValue(String s) {
                return configManager.getIntegerValue(s);
            }

            @Override
            public Float getFloatValue(String s) {
                return (float) configManager.getIntegerValue(s);
            }

            @Override
            public Double getDoubleValue(String s) {
                return configManager.getDoubleValue(s);
            }

            @Override
            public void setConfigValue(String s, PermittedObject<?> permittedObject) {
                configManager.setBracedConfigValue(s, permittedObject.getValue(), "");
            }

            @Override
            public boolean doesKeyExist(String s) {
                return configManager.doesPathExist(s);
            }

            @Override
            public void saveConfig() {
                configManager.saveConfig();
            }
        };
    }

    @Override
    public boolean platformIsNative() {
        return true;
    }

    @Override
    public File getPlatformDataFolder() {
        return getDataFolder();
    }

    @Override
    public PlatformLogger getPlatformLogger() {
        return new PlatformLogger() {
            @Override
            public void info(String s) {
                getLogger().info(s);
            }

            @Override
            public void warn(String s) {
                getLogger().warning(s);
            }

            @Override
            public void error(String s) {
                getLogger().severe(s);
            }

            @Override
            public void debug(String s) {
                getLogger().info("[DEBUG] " + s);
            }

            @Override
            public void error(String s, Throwable throwable) {
                getLogger().severe(s);
                throwable.printStackTrace();
            }
        };
    }

    @Override
    public PlatformDimensionManager<BlockData, World> getDimensionManager() {
        return dimensionManager;
    }

    @Override
    public PlatformTrinketManager<?> getPlatformTrinketManager() {
        return trinketManager;
    }

    @Override
    public QuestProductionFactory getQuestProductionFactory() {
        return null;
    }

    @Override
    public PlatformAdvancementManager<BukkitAdvancement> getPlatformAdvancementManager() {
        return advancementManager;
    }

    @Override
    public PlatformBiomeFactory<BukkitBiome> getPlatformBiomeFactory() {
        return new BukkitBiomeFactory();
    }
}