package org.vicky.vspe.systems.dimension;

import eu.endercentral.crazy_advancements.advancement.AdvancementDisplay;
import eu.endercentral.crazy_advancements.advancement.AdvancementReward;
import eu.endercentral.crazy_advancements.advancement.AdvancementVisibility;
import eu.endercentral.crazy_advancements.advancement.criteria.Criteria;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.World.Environment;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.vicky.bukkitplatform.useables.BukkitPlatformPlayer;
import org.vicky.bukkitplatform.useables.BukkitWorldAdapter;
import org.vicky.guiparent.ButtonAction;
import org.vicky.guiparent.GuiCreator;
import org.vicky.items_adder.FontImageSender;
import org.vicky.utilities.ANSIColor;
import org.vicky.utilities.BukkitHex;
import org.vicky.utilities.ContextLogger.ContextLogger;
import org.vicky.utilities.SmallCapsConverter;
import org.vicky.utilities.UUIDGenerator;
import org.vicky.vspe.VSPE;
import org.vicky.vspe.features.AdvancementPlus.AdvancementManager;
import org.vicky.vspe.features.AdvancementPlus.AdvancementType;
import org.vicky.vspe.features.AdvancementPlus.Advancements.DimensionParentAdvancement;
import org.vicky.vspe.features.AdvancementPlus.BaseAdvancement;
import org.vicky.vspe.features.AdvancementPlus.Exceptions.AdvancementNotExists;
import org.vicky.vspe.features.AdvancementPlus.Exceptions.NullAdvancementUser;
import org.vicky.vspe.platform.systems.dimension.Exceptions.NoGeneratorException;
import org.vicky.vspe.platform.systems.dimension.Exceptions.WorldNotExistsException;
import org.vicky.vspe.platform.systems.dimension.PlatformBaseDimension;
import org.vicky.vspe.systems.BroadcastSystem.ToastType;
import org.vicky.vspe.systems.dimension.Events.DimensionWarpEvent;
import org.vicky.vspe.systems.dimension.Generator.BaseGenerator;
import org.vicky.vspe.utilities.Config;
import org.vicky.vspe.utilities.Hibernate.DBTemplates.AdvanceablePlayer;
import org.vicky.vspe.utilities.Hibernate.DBTemplates.Advancement;
import org.vicky.vspe.utilities.Hibernate.DBTemplates.Dimension;
import org.vicky.vspe.utilities.Hibernate.api.DimensionService;
import org.vicky.vspe.utilities.Hibernate.dao_s.AdvanceablePlayerDAO;
import org.vicky.vspe.utilities.Hibernate.dao_s.AdvancementDAO;
import org.vicky.vspe.utilities.Manager.ManagerNotFoundException;
import org.vicky.vspe.utilities.Manager.ManagerRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.vicky.vspe.utilities.global.GlobalResources.*;

public abstract class BukkitBaseDimension implements PlatformBaseDimension<BlockData, World>, Listener {
    private final static DimensionService service = DimensionService.getInstance();
    private final String name;
    private final String mainName;
    private final BukkitWorldAdapter world;
    private final List<DimensionCharacteristics> dimensionCharacteristics;
    private final List<DimensionType> dimensionTypes;
    private final Environment environmentType;
    private final String seed;
    private final WorldType worldType;
    private final boolean generateStructures;
    private final BaseGenerator generator;
    private final ContextLogger logger;
    private String description;
    private boolean worldExists = true;
    private Location globalSpawnLocation;
    private DimensionTickHandler tickHandler;

    public BukkitBaseDimension(
            String mainName,
            String name,
            List<DimensionType> dimensionTypes,
            Environment environmentType,
            String seed,
            WorldType worldType,
            boolean generateStructures,
            Class<? extends BaseGenerator> generator
    ) throws WorldNotExistsException, NoGeneratorException, ManagerNotFoundException {
        this.name = name;
        this.mainName = mainName;
        this.logger = new ContextLogger(ContextLogger.ContextType.SYSTEM, "DIMENSION-" + name.toUpperCase());
        this.dimensionCharacteristics = new ArrayList<>();
        this.dimensionTypes = dimensionTypes;
        this.environmentType = environmentType;
        this.seed = seed;
        this.worldType = worldType;
        this.generateStructures = generateStructures;
        this.generator = dimensionManager.LOADED_GENERATORS.stream().filter(g -> g.getClass().equals(generator)).findFirst().orElse(null);
        this.world = this.checkWorld();
        DimensionClass.registerCustomDimension(name);
        Optional<AdvancementManager> oM = ManagerRegistry.getManager(AdvancementManager.class);
        try {
            AdvancementManager manager = oM.get();
            Bukkit.getPluginManager().registerEvents(this, manager.getPlugin());
            manager.addAdvancement(this.getDimensionJoinAdvancement());
        }catch (NoSuchElementException e) {
            throw new ManagerNotFoundException("Failed to locate advancement manager...");
        }
        for (DimensionType dimensionType : dimensionTypes) {
            dimensionType.addDimension(this);
        }
    }

    protected abstract void dimensionAdvancementGainProcedures(Player player);

    public String getName() {
        return this.name;
    }

    public BukkitWorldAdapter checkWorld() throws WorldNotExistsException, NoGeneratorException {
        BukkitWorldAdapter existingWorld = new BukkitWorldAdapter(Bukkit.getWorld(this.getName()));
        boolean isConfiguredAsExisting = (service.getDimensionById(UUIDGenerator.generateUUIDFromString(name).toString()) != null);
        if (isConfiguredAsExisting) {
            if (existingWorld.getBukkitWorld() != null) {
                dimensionManager.LOADED_DIMENSIONS.add(this);

                Dimension context;
                if (service.getDimensionById(UUIDGenerator.generateUUIDFromString(name).toString()) != null) {
                    context = service.getDimensionById(UUIDGenerator.generateUUIDFromString(name).toString());
                    context.setState(true);
                } else {
                    context = new Dimension();
                    context.setId(UUIDGenerator.generateUUIDFromString(name));
                    context.setName(mainName);
                    context.setState(true);
                }
                service.createDimension(context);
                return existingWorld;
            } else {
                logger.print("A critical config mismatch has occurred", true);
                throw new WorldNotExistsException(
                        "World with name '" + this.getName() + "' is marked as existing in the configuration, but it does not exist in Bukkit... Please edit the config and set the dimension [ " + this.mainName + " ] to false."
                );
            }
        } else if (existingWorld != null) {
            logger.print(ANSIColor.colorize("yellow[From configuration, World of dimension " + this.mainName + "does not exist but is found in bukkit. Please verify that is the world. use /dimension set " + this.mainName + " world true" + "]"));
            return existingWorld;
        } else {
            Config.configs.put("Dimensions." + this.getName() + ".exists", true);
            return this.createWorld(this.getName());
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BukkitWorldAdapter createWorld(String name) throws NoGeneratorException {
        if (this.generator == null) {
            logger.print("Generator is null for dimension: " + name, true);
            worldExists = false;
            throw new NoGeneratorException("Dimension's Generator is not loaded in registry");
        }
        boolean successful = worldManager.addWorld(name, this.environmentType, this.seed, this.worldType, this.generateStructures, this.generator.getGeneratorName());
        if (successful) {
            dimensionManager.LOADED_DIMENSIONS.add(this);

            Dimension context;
            if (service.getDimensionById(UUIDGenerator.generateUUIDFromString(name).toString()) != null) {
                context = service.getDimensionById(UUIDGenerator.generateUUIDFromString(name).toString());
                context.setState(true);
            } else {
                context = new Dimension();
                context.setId(UUIDGenerator.generateUUIDFromString(name));
                context.setName(mainName);
                context.setState(true);
            }

            service.createDimension(context);
            return new BukkitWorldAdapter(worldManager.getMVWorld(name).getCBWorld());
        } else {
            logger.print("Failed to generate Multiverse Dimension...", true);
            worldExists = false;
            return null;
        }
    }

    protected void isRandomSpawning() {
        this.dimensionCharacteristics.add(DimensionCharacteristics.RANDOM_SPAWN);
        this.dimensionCharacteristics.remove(DimensionCharacteristics.GLOBAL_SPAWN);
    }

    protected void isGlobalSpawning(Location spawnLocation) {
        this.dimensionCharacteristics.add(DimensionCharacteristics.GLOBAL_SPAWN);
        this.dimensionCharacteristics.remove(DimensionCharacteristics.RANDOM_SPAWN);
        this.globalSpawnLocation = spawnLocation.clone();
    }

    public BukkitWorldAdapter getWorld() {
        return this.world;
    }

    public boolean isPlayerInDimension(Player player) {
        return world.getBukkitWorld().getPlayers().stream().anyMatch(p -> p.getUniqueId().equals(player.getUniqueId()));
    }

    public abstract void applyMechanics(Player var1);

    public abstract void disableMechanics(Player var1);

    public abstract void applyJoinMechanics(Player var1);

    protected void deleteDimension() {
        logger.print("Deleting dimension " + this.getName());
        worldManager.removePlayersFromWorld(this.getName());
        dimensionManager.LOADED_DIMENSIONS.remove(this);
        Dimension context;
        if (service.getDimensionById(UUIDGenerator.generateUUIDFromString(name).toString()) != null) {
            context = service.getDimensionById(UUIDGenerator.generateUUIDFromString(name).toString());
            service.deleteDimension(context);
        }
        worldManager.deleteWorld(this.getName());
    }

    public void disableDimension() {
        logger.print("Disabling dimension " + this.getName());
        worldManager.removePlayersFromWorld(this.getName());
        dimensionManager.UNLOADED_DIMENSIONS.add(this);
        dimensionManager.LOADED_DIMENSIONS.remove(this);
        Dimension context;
        if (service.getDimensionById(UUIDGenerator.generateUUIDFromString(name).toString()) != null) {
            context = service.getDimensionById(UUIDGenerator.generateUUIDFromString(name).toString());
            context.setState(false);
        } else {
            context = new Dimension();
            context.setId(UUIDGenerator.generateUUIDFromString(name));
            context.setName(mainName);
            context.setState(false);
        }
        service.updateDimension(context);
        worldManager.unloadWorld(this.getName());
    }

    public void enableDimension() {
        logger.print("Enabling dimension " + this.getName());
        dimensionManager.LOADED_DIMENSIONS.add(this);
        dimensionManager.UNLOADED_DIMENSIONS.remove(this);
        Dimension context;
        if (service.getDimensionById(UUIDGenerator.generateUUIDFromString(name).toString()) != null) {
            context = service.getDimensionById(UUIDGenerator.generateUUIDFromString(name).toString());
            context.setState(true);
        } else {
            context = new Dimension();
            context.setId(UUIDGenerator.generateUUIDFromString(name));
            context.setName(mainName);
            context.setState(true);
        }
        service.updateDimension(context);
        worldManager.loadWorld(this.getName());
    }

    public GuiCreator.ItemConfig getItemConfig(int position) {
        return new GuiCreator.ItemConfig(
                null,
                Component.text(this.mainName).color(TextColor.fromHexString(this.dimensionTypes.get(0).getHexCode())).content(),
                Integer.toString(position),
                true,
                null,
                "vspe_dimensions:" + this.name,
                List.of("Dimension categories: " +
                                this.dimensionTypes.stream()
                                        .map(type -> " " + FontImageSender.getImage("vspe_dimensions:" + type.name().toLowerCase()))
                                        .collect(Collectors.joining(" ")),
                        (this.description != null ? "Description: " + this.description : "")),
                ButtonAction.ofRunCode(p -> takePlayerToDimension(BukkitPlatformPlayer.of(p)), true)
        );
    }

    public GuiCreator.ItemConfig getItemConfig(AdvanceablePlayer player) {
        return new GuiCreator.ItemConfig(
                null,
                Component.text(this.mainName).color(TextColor.fromHexString(this.dimensionTypes.get(0).getHexCode())).content(),
                "",
                true,
                null,
                "vspe_dimensions:" + this.name,
                List.of("Dimension categories: " +
                                this.dimensionTypes.stream()
                                        .map(type -> " " + FontImageSender.getImage("vspe_dimensions:" + type.name().toLowerCase()))
                                        .collect(Collectors.joining(" ")),
                        (this.description != null ? "Description: " + this.description : ""),
                        ChatColor.YELLOW + "→ "  + ChatColor.GOLD + SmallCapsConverter.toSmallCaps(player.getAccomplishedAdvancements().stream().anyMatch(d -> d.getId().equals(UUIDGenerator.generateUUIDFromString(name))) ? "You've visited this dimension before" : "You are yet to visit this dimension..")),
                ButtonAction.ofRunCode(this::takePlayerToDimension, true)
        );
    }

    public BaseAdvancement getDimensionJoinAdvancement() {

        return new BaseAdvancement(
                GuiCreator.createItem(getItemConfig(0), null, VSPE.getPlugin()),
                AdvancementDisplay.AdvancementFrame.CHALLENGE,
                BukkitHex.colorize(
                    String.format("""
                        orange[Find the %s portal and jump through it]
                          purple[Rewards:]
                        %s
                        """,
                            BukkitBaseDimension.this.mainName,
                                dimensionAdvancementGainItems().isEmpty()
                                        ? "~ nothing ~"
                                        : dimensionAdvancementGainItems().stream()
                                        .map(item -> "gold[    ▪ ]" + "rainbow-" + item.getRarity().getColor() + "[" + item.getName().toLowerCase() + "]")
                                        .collect(Collectors.joining("\n")))
                ),
                BukkitBaseDimension.this.mainName,
                new NamespacedKey("vspe", "dimension_"+ getIdentifier()),
                List.of(BukkitBaseDimension.this),
                AdvancementType.CHALLENGE,
                AdvancementVisibility.ALWAYS,
                ToastType.POPUP_TOAST,
                DimensionParentAdvancement.class
        ) {
            @Override
            protected Criteria advancementCriteria() {
                return null;
            }

            @Override
            protected AdvancementReward advancementReward() {
                return new AdvancementReward() {
                    @Override
                    public void onGrant(Player player) {
                        for(GuiCreator.ItemConfig item : dimensionAdvancementGainItems()) {
                            ItemStack itemStack = GuiCreator.createItem(item, player, VSPE.getPlugin());
                            player.getInventory().addItem(itemStack);
                        }
                        player.giveExp(5);
                    }
                };
            }

            @Override
            protected void performGrantAdvancement(OfflinePlayer var1) {
                dimensionAdvancementGainProcedures((Player) var1);
            }
        };
    }

    @EventHandler
    protected final void onPlayerEnterDimension(DimensionWarpEvent event) {
        if (!this.dimensionJoinCondition(event.getPlayer())) {
            event.setCancelled(true);
            return;
        }
        Optional<AdvanceablePlayer> oAP = new AdvanceablePlayerDAO().findById(event.getPlayer().getUniqueId());
        if (oAP.isEmpty()) {
            try {
                event.setCancelled(true);
                throw new NullAdvancementUser("Failed to locate advanceable player whilst trying to enter dimension", event.getPlayer());
            } catch (NullAdvancementUser e) {
                throw new RuntimeException(e);
            }
        }
        AdvanceablePlayer player = oAP.get();
        Optional<Advancement> oDA =
                new AdvancementDAO().findByName(this.getDimensionJoinAdvancement().getTitle());
        if (oDA.isEmpty()) {
            try {
                event.setCancelled(true);
                throw new AdvancementNotExists("Failed to locate advancement whilst trying to enter dimension", this.getDimensionJoinAdvancement());
            } catch (AdvancementNotExists e) {
                throw new RuntimeException(e);
            }
        }
        Advancement dimensionAdvancement = oDA.get();
        if (!player.getAccomplishedAdvancements().contains(dimensionAdvancement)) {
            if (!advancementManager.grantAdvancemet(getDimensionJoinAdvancement().getClass(), event.getPlayer())) event.setCancelled(true);
        }
    }
    public final void setTickHandler(DimensionTickHandler handler) {
        this.tickHandler = handler;
    }
    public final void tick() {
        if (tickHandler != null) {
            tickHandler.tick(world.getPlayers(), this.world);
        }
    }

    protected abstract boolean dimensionJoinCondition(Player player);

    @Override
    public String getIdentifier() {
        return this.name;
    }
}
