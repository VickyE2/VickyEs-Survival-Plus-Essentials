package org.vicky.vspe.forge.dimension;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vicky.forge.forgeplatform.useables.ForgePlatformItem;
import org.vicky.forge.forgeplatform.useables.ForgePlatformPlayer;
import org.vicky.forge.forgeplatform.useables.ForgePlatformWorldAdapter;
import org.vicky.forge.forgeplatform.useables.ForgeVec3;
import org.vicky.platform.PlatformItem;
import org.vicky.platform.PlatformPlayer;
import org.vicky.platform.PlatformPlugin;
import org.vicky.platform.world.PlatformLocation;
import org.vicky.platform.world.PlatformWorld;
import org.vicky.utilities.ANSIColor;
import org.vicky.utilities.ContextLogger.ContextLogger;
import org.vicky.utilities.PermittedObjects.AllowedBoolean;
import org.vicky.utilities.UUIDGenerator;
import org.vicky.vspe.forge.advancements.ForgeAdvancement;
import org.vicky.vspe.forge.events.DimensionWarpEvent;
import org.vicky.vspe.forge.forgeplatform.ForgeAdvancementManager;
import org.vicky.vspe.forge.forgeplatform.ForgeDimensionManager;
import org.vicky.vspe.forge.forgeplatform.useables.Descriptored;
import org.vicky.vspe.forge.forgeplatform.useables.ForgeDimensionWarpEvent;
import org.vicky.vspe.platform.PlatformEnvironment;
import org.vicky.vspe.platform.PlatformWorldType;
import org.vicky.vspe.platform.features.advancement.Exceptions.AdvancementNotExists;
import org.vicky.vspe.platform.features.advancement.Exceptions.NullAdvancementUser;
import org.vicky.vspe.platform.systems.dimension.DimensionClass;
import org.vicky.vspe.platform.systems.dimension.DimensionType;
import org.vicky.vspe.platform.systems.dimension.Events.PlatformDimensionWarpEvent;
import org.vicky.vspe.platform.systems.dimension.Exceptions.NoGeneratorException;
import org.vicky.vspe.platform.systems.dimension.Exceptions.WorldNotExistsException;
import org.vicky.vspe.platform.systems.dimension.PlatformBaseDimension;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.BaseGenerator;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.PlatformDimension;
import org.vicky.vspe.platform.utilities.Config;
import org.vicky.vspe.platform.utilities.Hibernate.DBTemplates.AdvanceablePlayer;
import org.vicky.vspe.platform.utilities.Hibernate.DBTemplates.Advancement;
import org.vicky.vspe.platform.utilities.Hibernate.DBTemplates.Dimension;
import org.vicky.vspe.platform.utilities.Hibernate.api.DimensionService;
import org.vicky.vspe.platform.utilities.Hibernate.dao_s.AdvanceablePlayerDAO;
import org.vicky.vspe.platform.utilities.Hibernate.dao_s.AdvancementDAO;
import org.vicky.vspe.platform.utilities.Manager.ManagerNotFoundException;
import org.vicky.vspe.platform.utilities.Manager.ManagerRegistry;
import org.vicky.vspe.systems.dimension.PlatformDimensionTickHandler;

import java.util.*;

import static org.vicky.vspe.VspeForge.MODID;
import static org.vicky.vspe.forge.forgeplatform.AwsomeForgeHacks.getLevel;
import static org.vicky.vspe.forge.forgeplatform.AwsomeForgeHacks.moveAllPlayersToOverworld;
import static org.vicky.vspe.platform.systems.dimension.DimensionType.AQUATIC_WORLD;

public abstract class ForgeBaseDimension implements PlatformBaseDimension<BlockState, Level> {
    protected final static DimensionService service = DimensionService.getInstance();
    private final String name;
    private final String mainName;
    private final List<DimensionType> dimensionTypes;
    private final String seed;
    private final PlatformWorldType worldType;
    private final boolean generateStructures;
    private final ChunkGenerator generator;
    private final ContextLogger logger;
    private ForgePlatformWorldAdapter world = null;
    private String description;
    private boolean worldExists = true;
    private PlatformDimensionTickHandler tickHandler;

    public ForgeBaseDimension(
            @NotNull String mainName,
            @NotNull String name,
            @NotNull List<DimensionType> dimensionTypes,
            @NotNull String seed,
            @NotNull PlatformWorldType worldType,
            boolean generateStructures,
            @NotNull ChunkGenerator generator
    ) throws ManagerNotFoundException {
        this.name = name;
        this.mainName = mainName;
        this.logger = new ContextLogger(ContextLogger.ContextType.SYSTEM, "DIMENSION-" + name.toUpperCase());
        this.dimensionTypes = dimensionTypes;
        this.seed = seed;
        this.worldType = worldType;
        this.generateStructures = generateStructures;
        this.generator = generator;
        DimensionClass.registerCustomDimension(name);
        Optional<ForgeAdvancementManager> oM = ManagerRegistry.getManager(ForgeAdvancementManager.class);
        try {
            ForgeAdvancementManager manager = oM.get();
            manager.addAdvancement(this.getDimensionJoinAdvancement());
        } catch (NoSuchElementException e) {
            throw new ManagerNotFoundException("Failed to locate advancement manager...");
        }
        for (DimensionType dimensionType : dimensionTypes) {
            dimensionType.addDimension((PlatformDimension<?, ?>) this);
        }
        ResourceLocation dimId = ResourceLocation.parse(getIdentifier());
        if (this instanceof Descriptored descriptored) {
            WorldManager.registerAtStartup(dimId, getDimensionType(), () -> generator, (world) -> {
                this.world = new ForgePlatformWorldAdapter(world);
                return null;
            }, descriptored.getDescriptor());
        } else {
            WorldManager.registerAtStartup(dimId, getDimensionType(), () -> generator, (world) -> {
                this.world = new ForgePlatformWorldAdapter(world);
                return null;
            });
        }
    }

    protected abstract void dimensionAdvancementGainProcedures(ServerPlayer player);

    public String getName() {
        return this.name;
    }

    public String getMainName() {
        return mainName;
    }

    public ForgePlatformWorldAdapter checkWorld() throws WorldNotExistsException, NoGeneratorException {
        ForgePlatformWorldAdapter existingWorld = new ForgePlatformWorldAdapter(getLevel(MODID + ":" + this.getName()));
        boolean isConfiguredAsExisting = (service.getDimensionById(UUIDGenerator.generateUUIDFromString(name).toString()) != null);
        if (isConfiguredAsExisting) {
            if (existingWorld.getNative() != null) {
                ManagerRegistry.getManager(ForgeDimensionManager.class).get().LOADED_DIMENSIONS.add(this);

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
        } else if (existingWorld.getNative() != null) {
            logger.print(ANSIColor.colorize("yellow[From configuration, World of dimension " + this.mainName + "does not exist but is found in bukkit. Please verify that is the world. use /dimension set " + this.mainName + " world true" + "]"));
            return existingWorld;
        } else {
            Config.configs.put("Dimensions." + this.getName() + ".exists", new AllowedBoolean(true));
            return this.createWorld();
        }
    }

    public ChunkGenerator getForgeGenerator() {
        return generator;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public PlatformWorld<BlockState, Level> createWorld(String s) throws NoGeneratorException {
        return createWorld();
    }

    public ForgePlatformWorldAdapter createWorld() throws NoGeneratorException {
        if (this.generator == null) {
            logger.print("Generator is null for dimension: " + name, true);
            worldExists = false;
            throw new NoGeneratorException("Dimension's Generator is not loaded in registry");
        }
        boolean successful = WorldManager.isRegistered(ResourceLocation.parse(getIdentifier()));
        if (successful) {
            ManagerRegistry.getManager(ForgeDimensionManager.class).get().LOADED_DIMENSIONS.add(this);

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
            return world;
        } else {
            logger.print("Failed to generate Dimension...", true);
            worldExists = false;
            return null;
        }
    }

    public ForgePlatformWorldAdapter getWorld() {
        return this.world;
    }

    public boolean isPlayerInDimension(ServerPlayer player) {
        return world.getNative().players().stream().anyMatch(p -> p.getUUID().equals(player.getUUID()));
    }

    public abstract void applyMechanics(ServerPlayer var1);

    public abstract void disableMechanics(ServerPlayer var1);

    public abstract void applyJoinMechanics(ServerPlayer var1);

    public void deleteDimension() {
        logger.print("Deleting dimension " + this.getName());
        moveAllPlayersToOverworld((ServerLevel) world.getNative());
        ManagerRegistry.getManager(ForgeDimensionManager.class).get().LOADED_DIMENSIONS.remove(this);
        Dimension context;
        if (service.getDimensionById(UUIDGenerator.generateUUIDFromString(name).toString()) != null) {
            context = service.getDimensionById(UUIDGenerator.generateUUIDFromString(name).toString());
            service.deleteDimension(context);
        }
        WorldManager.unregister(ResourceLocation.parse(this.getIdentifier()));
    }

    public void disableDimension() {
        logger.print("Disabling dimension " + this.getName());
        moveAllPlayersToOverworld((ServerLevel) world.getNative());
        ManagerRegistry.getManager(ForgeDimensionManager.class).get().UNLOADED_DIMENSIONS.add(this);
        ManagerRegistry.getManager(ForgeDimensionManager.class).get().LOADED_DIMENSIONS.remove(this);
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
        WorldManager.unregister(ResourceLocation.parse(this.getIdentifier()));
    }

    public void enableDimension() {
        logger.print("Enabling dimension " + this.getName());
        ManagerRegistry.getManager(ForgeDimensionManager.class).get().LOADED_DIMENSIONS.add(this);
        ManagerRegistry.getManager(ForgeDimensionManager.class).get().UNLOADED_DIMENSIONS.remove(this);
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
        WorldManager.unregister(ResourceLocation.parse(this.getIdentifier()));
    }

    public ItemStack getDimensionIcon() {
        return null;
    }

    public abstract ForgeAdvancement getDimensionJoinAdvancement();

    @SubscribeEvent
    protected final void onPlayerEnterDimension(DimensionWarpEvent event) {
        if (!this.dimensionJoinCondition(event.getPlayer())) {
            event.setCanceled(true);
            return;
        }
        Optional<AdvanceablePlayer> oAP = new AdvanceablePlayerDAO().findById(event.getPlayer().getUUID());
        if (oAP.isEmpty()) {
            try {
                event.setCanceled(true);
                throw new NullAdvancementUser("Failed to locate advanceable player whilst trying to enter dimension", new ForgePlatformPlayer(event.getPlayer()));
            } catch (NullAdvancementUser e) {
                throw new RuntimeException(e);
            }
        }
        AdvanceablePlayer player = oAP.get();
        Optional<Advancement> oDA =
                new AdvancementDAO().findByName(this.getDimensionJoinAdvancement().getTitle());
        if (oDA.isEmpty()) {
            try {
                event.setCanceled(true);
                throw new AdvancementNotExists("Failed to locate advancement whilst trying to enter dimension", this.getDimensionJoinAdvancement());
            } catch (AdvancementNotExists e) {
                throw new RuntimeException(e);
            }
        }
        Advancement dimensionAdvancement = oDA.get();
        if (!player.getAccomplishedAdvancements().contains(dimensionAdvancement)) {
            if (!ManagerRegistry.getManager(ForgeAdvancementManager.class).get().grantAdvancement(getDimensionJoinAdvancement().getClass(), new ForgePlatformPlayer(event.getPlayer())))
                event.setCanceled(true);
        }
    }

    @Override
    public boolean isSafeSpawnLocation(@Nullable PlatformLocation platformLocation) {
        if (platformLocation == null) {
            return false;
        }

        ForgeVec3 loc = (ForgeVec3) PlatformPlugin.locationAdapter().toNative(platformLocation);
        ForgeVec3 up = (ForgeVec3) PlatformPlugin.locationAdapter().toNative(platformLocation.add(0, 1, 0));
        var block = loc.getBlock();

        if (!block.isSolid() && !this.getDimensionTypes().contains(AQUATIC_WORLD)) {
            return false;
        }

        if (block.isSolid()) {
            return false;
        }

        return !up.getBlock().isSolid();
    }


    @Override
    public @Nullable Double findGroundYAt(int x, int z) {
        world.loadChunkIfNeeded(x >> 4, z >> 4);
        int y = world.getHighestBlockYAt(x, z);
        if (y <= world.getNative().getMinBuildHeight()) {
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
        if (platformPlayer instanceof ForgePlatformPlayer player)
            return dimensionJoinCondition(player.getHandle());
        throw new IllegalArgumentException("Got non ForgePlatformPlayer for warp event");
    }

    @Override
    public PlatformItem getDimensionIcon(int i) {
        return new ForgePlatformItem(getDimensionIcon());
    }

    @Override
    public void applyJoinMechanics(PlatformPlayer platformPlayer) {
        if (platformPlayer instanceof ForgePlatformPlayer player)
            applyJoinMechanics(player.getHandle());
    }

    @Override
    public void disableMechanics(PlatformPlayer platformPlayer) {
        if (platformPlayer instanceof ForgePlatformPlayer player)
            disableMechanics(player.getHandle());
    }

    @Override
    public void applyMechanics(PlatformPlayer platformPlayer) {
        if (platformPlayer instanceof ForgePlatformPlayer player)
            applyMechanics(player.getHandle());
    }

    @Override
    public void removePlayerFromDimension(PlatformPlayer platformPlayer) {
        if (platformPlayer instanceof ForgePlatformPlayer player) {

        }
    }

    @Override
    public boolean isPlayerInDimension(PlatformPlayer platformPlayer) {
        if (platformPlayer instanceof ForgePlatformPlayer player)
            return isPlayerInDimension(player.getHandle());
        return false;
    }

    @Override
    public PlatformDimensionWarpEvent createWarpEvent(PlatformPlayer platformPlayer) {
        if (platformPlayer instanceof ForgePlatformPlayer player)
            return new ForgeDimensionWarpEvent(this, new DimensionWarpEvent(player.getHandle(), this));
        throw new IllegalArgumentException("Got non ForgePlatformPlayer for warp event");
    }

    @Override
    public void dimensionAdvancementGainProcedures(PlatformPlayer platformPlayer) {
        if (platformPlayer instanceof ForgePlatformPlayer player)
            dimensionAdvancementGainProcedures(player.getHandle());
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
        return null;
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

    protected abstract boolean dimensionJoinCondition(ServerPlayer player);

    @Override
    public String getIdentifier() {
        return MODID + ":" + this.name;
    }

    public net.minecraft.world.level.dimension.DimensionType getDimensionType() {
        return new net.minecraft.world.level.dimension.DimensionType(
                OptionalLong.of(12000), // fixedTime
                false, // hasSkylight
                false, // hasCeiling
                false, // ultraWarm
                false, // natural
                1.0, // coordinateScale
                true, // bedWorks
                false, // respawnAnchorWorks
                0, // minY
                256, // height
                256, // logicalHeight
                BlockTags.INFINIBURN_OVERWORLD, // infiniburn
                BuiltinDimensionTypes.OVERWORLD_EFFECTS, // effectsLocation
                1.0f, // ambientLight
                new net.minecraft.world.level.dimension.DimensionType.MonsterSettings(false, false, ConstantInt.of(0), 0));
    }
}
