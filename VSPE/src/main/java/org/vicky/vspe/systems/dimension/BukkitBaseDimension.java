package org.vicky.vspe.systems.dimension;

import eu.endercentral.crazy_advancements.advancement.AdvancementDisplay;
import eu.endercentral.crazy_advancements.advancement.AdvancementReward;
import eu.endercentral.crazy_advancements.advancement.AdvancementVisibility;
import eu.endercentral.crazy_advancements.advancement.criteria.Criteria;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.World.Environment;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.vicky.bukkitplatform.useables.*;
import org.vicky.guiparent.ButtonAction;
import org.vicky.guiparent.GuiCreator;
import org.vicky.items_adder.FontImageSender;
import org.vicky.platform.PlatformItem;
import org.vicky.platform.PlatformPlayer;
import org.vicky.platform.world.PlatformLocation;
import org.vicky.utilities.ANSIColor;
import org.vicky.utilities.BukkitHex;
import org.vicky.utilities.ContextLogger.ContextLogger;
import org.vicky.utilities.SmallCapsConverter;
import org.vicky.utilities.UUIDGenerator;
import org.vicky.vspe.features.AdvancementPlus.AdvancementManager;
import org.vicky.vspe.features.AdvancementPlus.AdvancementType;
import org.vicky.vspe.features.AdvancementPlus.Advancements.DimensionParentAdvancement;
import org.vicky.vspe.features.AdvancementPlus.BukkitAdvancement;
import org.vicky.vspe.paper.BukkitDimensionWarpEvent;
import org.vicky.vspe.paper.BukkitWorldTypeAdapter;
import org.vicky.vspe.platform.PlatformEnvironment;
import org.vicky.vspe.platform.PlatformWorldType;
import org.vicky.vspe.platform.features.advancement.Exceptions.AdvancementNotExists;
import org.vicky.vspe.platform.features.advancement.Exceptions.NullAdvancementUser;
import org.vicky.vspe.platform.systems.dimension.DimensionClass;
import org.vicky.vspe.platform.systems.dimension.DimensionDescriptor;
import org.vicky.vspe.platform.systems.dimension.DimensionType;
import org.vicky.vspe.platform.systems.dimension.Events.PlatformDimensionWarpEvent;
import org.vicky.vspe.platform.systems.dimension.Exceptions.NoGeneratorException;
import org.vicky.vspe.platform.systems.dimension.Exceptions.WorldNotExistsException;
import org.vicky.vspe.platform.systems.dimension.PlatformBaseDimension;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.BaseGenerator;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.PlatformDimension;
import org.vicky.vspe.platform.utilities.Manager.ManagerNotFoundException;
import org.vicky.vspe.platform.utilities.Manager.ManagerRegistry;
import org.vicky.vspe.systems.BroadcastSystem.ToastType;
import org.vicky.vspe.systems.dimension.Events.DimensionWarpEvent;
import org.vicky.vspe.utilities.Config;
import org.vicky.vspe.utilities.Hibernate.DBTemplates.AdvanceablePlayer;
import org.vicky.vspe.utilities.Hibernate.DBTemplates.Advancement;
import org.vicky.vspe.utilities.Hibernate.DBTemplates.Dimension;
import org.vicky.vspe.utilities.Hibernate.api.DimensionService;
import org.vicky.vspe.utilities.Hibernate.dao_s.AdvanceablePlayerDAO;
import org.vicky.vspe.utilities.Hibernate.dao_s.AdvancementDAO;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.vicky.vspe.platform.systems.dimension.DimensionType.AQUATIC_WORLD;
import static org.vicky.vspe.utilities.global.GlobalResources.*;

public abstract class BukkitBaseDimension implements PlatformBaseDimension<BlockData, World>, Listener {
    private final static DimensionService service = DimensionService.getInstance();
    private final String name;
    private final String mainName;
    private final BukkitWorldAdapter world;
    private final List<DimensionType> dimensionTypes;
    private final Environment environmentType;
    private final String seed;
    private final PlatformWorldType worldType;
    private final boolean generateStructures;
    private final BaseGenerator generator;
    private final ContextLogger logger;
    private String description;
    private boolean worldExists = true;
    private PlatformDimensionTickHandler tickHandler;

    public BukkitBaseDimension(
            String mainName,
            String name,
            List<DimensionType> dimensionTypes,
            Environment environmentType,
            String seed,
            PlatformWorldType worldType,
            boolean generateStructures,
            Class<? extends BaseGenerator> generator
    ) throws WorldNotExistsException, NoGeneratorException, ManagerNotFoundException {
        this.name = name;
        this.mainName = mainName;
        this.logger = new ContextLogger(ContextLogger.ContextType.SYSTEM, "DIMENSION-" + name.toUpperCase());
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
            dimensionType.addDimension((PlatformDimension<?, ?>) this);
        }
    }

    public BukkitBaseDimension(
            DimensionDescriptor descriptor,
            String seed
    ) throws WorldNotExistsException, NoGeneratorException, ManagerNotFoundException {
        this.name = descriptor.name().toLowerCase().replace("!@#$^&*()-=+'\";:/?.,\\|}{][ ", "_");
        this.mainName = descriptor.name();
        this.logger = new ContextLogger(ContextLogger.ContextType.SYSTEM, "DIMENSION-" + name.toUpperCase());
        this.dimensionTypes = descriptor.dimensionTypes();
        this.environmentType = Environment.NORMAL;
        this.seed = seed;
        this.worldType = worldType;
        this.generateStructures = descriptor.shouldGenerateStructures();
        this.generator = null;
        this.world = this.checkWorld();
        DimensionClass.registerCustomDimension(name);
        Optional<AdvancementManager> oM = ManagerRegistry.getManager(AdvancementManager.class);
        try {
            AdvancementManager manager = oM.get();
            Bukkit.getPluginManager().registerEvents(this, manager.getPlugin());
            manager.addAdvancement(this.getDimensionJoinAdvancement());
        } catch (NoSuchElementException e) {
            throw new ManagerNotFoundException("Failed to locate advancement manager...");
        }
        for (DimensionType dimensionType : dimensionTypes) {
            dimensionType.addDimension((PlatformDimension<?, ?>) this);
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
        } else if (existingWorld.getBukkitWorld() != null) {
            logger.print(ANSIColor.colorize("yellow[From configuration, World of dimension " + this.mainName + "does not exist but is found in bukkit. Please verify that is the world. use /dimension set " + this.mainName + " world true" + "]"));
            return existingWorld;
        } else {
            Config.configs.put("Dimensions." + this.getName() + ".exists", true);
            return this.createWorld();
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BukkitWorldAdapter createWorld() throws NoGeneratorException {
        if (this.generator == null) {
            logger.print("Generator is null for dimension: " + name, true);
            worldExists = false;
            throw new NoGeneratorException("Dimension's Generator is not loaded in registry");
        }
        boolean successful = worldManager.addWorld(name, this.environmentType, this.seed, BukkitWorldTypeAdapter.adapt(this.worldType), this.generateStructures, this.generator != null ? this.generator.getGeneratorName() : "");
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
                ButtonAction.ofRunCode(p -> takePlayerToDimension(BukkitPlatformPlayer.of(p)), true)
        );
    }

    public BukkitAdvancement getDimensionJoinAdvancement() {
        return new BukkitAdvancement(
                GuiCreator.createItem(getItemConfig(0), null),
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
                                        .filter(ItemConfigPlatformItem.class::isInstance)
                                        .map(ItemConfigPlatformItem.class::cast)                 // safe cast
                                        .map(item -> "gold[    ▪ ]" + "rainbow-" + item.getStack().getRarity().getColor() + "[" + item.getName().toLowerCase() + "]")
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
                        for (PlatformItem item : dimensionAdvancementGainItems()) {
                            if (item instanceof BukkitItem bukkitItem) {
                                player.getInventory().addItem(bukkitItem.getStack());
                            }
                            if (item instanceof ItemConfigPlatformItem itemConfigPlatformItem) {
                                ItemStack itemStack = GuiCreator.createItem(itemConfigPlatformItem.getStack(), player);
                                player.getInventory().addItem(itemStack);
                            }
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
                throw new NullAdvancementUser("Failed to locate advanceable player whilst trying to enter dimension", BukkitPlatformPlayer.of(event.getPlayer()));
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

    @Override
    public boolean isSafeSpawnLocation(@Nullable PlatformLocation platformLocation) {
        if (platformLocation == null) {
            return false;
        }

        var loc = BukkitLocationAdapter.to(platformLocation);
        var block = loc.getBlock();

        // Unsafe: inside liquid unless dimension allows aquatic spawns
        if (block.isLiquid() && !this.getDimensionTypes().contains(AQUATIC_WORLD)) {
            return false;
        }

        // Unsafe: inside a solid block (e.g. suffocating inside stone)
        if (block.isSolid()) {
            return false;
        }

        // Optional: also check headroom
        var above = block.getRelative(BlockFace.UP);
        return !above.isSolid();
    }


    @Override
    public @Nullable Double findGroundYAt(int x, int z) {
        var bukkitWorld = world.getBukkitWorld();
        bukkitWorld.loadChunk(x >> 4, z >> 4);
        int y = bukkitWorld.getHighestBlockYAt(x, z);
        if (y <= bukkitWorld.getMinHeight()) {
            return null;
        }

        return (double) y;
    }

    @Override
    public @Nullable PlatformLocation locationAt(double v, double v1, double v2) {
        return world.getBlockAt(v, v1, v2).getLocation();
    }

    @Override
    public boolean dimensionJoinCondition(PlatformPlayer platformPlayer) {
        if (platformPlayer instanceof BukkitPlatformPlayer player)
            return dimensionJoinCondition(player.getBukkitPlayer());
        throw new IllegalArgumentException("Got non BukkitPlatformPlayer for warp event");
    }

    @Override
    public PlatformItem getDimensionIcon(int i) {
        return new ItemConfigPlatformItem(getItemConfig(i));
    }

    @Override
    public void applyJoinMechanics(PlatformPlayer platformPlayer) {
        if (platformPlayer instanceof BukkitPlatformPlayer player)
            applyJoinMechanics(player.getBukkitPlayer());
    }

    @Override
    public void disableMechanics(PlatformPlayer platformPlayer) {
        if (platformPlayer instanceof BukkitPlatformPlayer player)
            disableMechanics(player.getBukkitPlayer());
    }

    @Override
    public void applyMechanics(PlatformPlayer platformPlayer) {
        if (platformPlayer instanceof BukkitPlatformPlayer player)
            applyMechanics(player.getBukkitPlayer());
    }

    @Override
    public void removePlayerFromDimension(PlatformPlayer platformPlayer) {
        if (platformPlayer instanceof BukkitPlatformPlayer player) {

        }
    }

    @Override
    public boolean isPlayerInDimension(PlatformPlayer platformPlayer) {
        if (platformPlayer instanceof BukkitPlatformPlayer player)
            return isPlayerInDimension(player.getBukkitPlayer());
        return false;
    }

    @Override
    public PlatformDimensionWarpEvent createWarpEvent(PlatformPlayer platformPlayer) {
        if (platformPlayer instanceof BukkitPlatformPlayer player)
            return new BukkitDimensionWarpEvent(this, new DimensionWarpEvent(player.getBukkitPlayer(), this));
        throw new IllegalArgumentException("Got non BukkitPlatformPlayer for warp event");
    }

    @Override
    public void dimensionAdvancementGainProcedures(PlatformPlayer platformPlayer) {
        if (platformPlayer instanceof BukkitPlatformPlayer player)
            dimensionAdvancementGainProcedures(player.getBukkitPlayer());
    }

    @Override
    public PlatformDimensionTickHandler getTickHandler() {
        return tickHandler;
    }

    @Override
    public void setTickHandler(PlatformDimensionTickHandler platformDimensionTickHandler) {

    }

    @Override
    public boolean dimensionExists() {
        return getWorld() != null;
    }

    @Override
    public BaseGenerator getGenerator() {
        return generator;
    }

    @Override
    public boolean generatesStructures() {
        return false;
    }

    @Override
    public PlatformWorldType getWorldType() {
        return worldType;
    }

    @Override
    public String getSeed() {
        return "";
    }

    @Override
    public PlatformEnvironment getEnvironmentType() {
        return null;
    }

    @Override
    public List<org.vicky.vspe.platform.systems.dimension.DimensionType> getDimensionTypes() {
        return new ArrayList<>(dimensionTypes);
    }

    @Override
    public boolean takePlayerToDimension(PlatformPlayer player) {
        return PlatformBaseDimension.super.takePlayerToDimension(player);
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
