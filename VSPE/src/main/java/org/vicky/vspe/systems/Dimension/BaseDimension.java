package org.vicky.vspe.systems.Dimension;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.vicky.guiparent.GuiCreator;
import org.vicky.utilities.ANSIColor;
import org.vicky.utilities.ContextLogger.ContextLogger;
import org.vicky.vspe.systems.Dimension.Exceptions.NoGeneratorException;
import org.vicky.vspe.systems.Dimension.Exceptions.WorldNotExistsException;
import org.vicky.vspe.systems.Dimension.Generator.BaseGenerator;
import org.vicky.vspe.utilities.Config;
import org.vicky.vspe.utilities.DBTemplates.Dimension;
import org.vicky.utilities.Identifiable;
import org.vicky.vspe.utilities.UUIDGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.vicky.vspe.utilities.global.GlobalResources.*;

public abstract class BaseDimension implements Identifiable {
    private final String name;
    private final String mainName;
    private final World world;
    private final List<DimensionCharacteristics> dimensionCharacteristics;
    private final List<DimensionType> dimensionTypes;
    private final List<Player> players = new ArrayList<>();
    private final Environment environmentType;
    private final String seed;
    private final WorldType worldType;
    private final boolean generateStructures;
    private final BaseGenerator generator;
    private final ContextLogger logger;
    private String description;
    private boolean worldExists = true;

    public BaseDimension(
            String mainName,
            String name,
            List<DimensionType> dimensionTypes,
            Environment environmentType,
            String seed,
            WorldType worldType,
            boolean generateStructures,
            Class<? extends BaseGenerator> generator
    ) throws WorldNotExistsException, NoGeneratorException {
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

        for (DimensionType dimensionType : dimensionTypes) {
            dimensionType.addDimension(this);
        }
    }

    public String getName() {
        return this.name;
    }

    public String getMainName() {
        return this.mainName;
    }

    private World checkWorld() throws WorldNotExistsException, NoGeneratorException {
        World existingWorld = Bukkit.getWorld(this.getName());
        boolean isConfiguredAsExisting = (databaseManager.getEntityById(Dimension.class, UUIDGenerator.generateUUIDFromString(name).toString()) != null);
        if (isConfiguredAsExisting) {
            if (existingWorld != null) {
                dimensionManager.LOADED_DIMENSIONS.add(this);

                Dimension context;
                if (databaseManager.getEntityById(Dimension.class, UUIDGenerator.generateUUIDFromString(name).toString()) != null) {
                    context = databaseManager.getEntityById(Dimension.class, UUIDGenerator.generateUUIDFromString(name).toString());
                    context.setState(true);
                } else {
                    context = new Dimension();
                    context.setId(UUIDGenerator.generateUUIDFromString(name));
                    context.setName(mainName);
                    context.setState(true);
                }

                databaseManager.saveOrUpdate(context);
                return existingWorld;
            } else {
                logger.printBukkit("A critical config mismatch has occurred", true);
                throw new WorldNotExistsException(
                        "World with name '" + this.getName() + "' is marked as existing in the configuration, but it does not exist in Bukkit... Please edit the config and set the dimension [ " + this.mainName + " ] to false."
                );
            }
        } else if (existingWorld != null) {
            logger.printBukkit(ANSIColor.colorize("yellow[From configuration, World of dimension " + this.mainName + "does not exist but is found in bukkit. Please verify that is the world.]"));
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

    private World createWorld(String name) throws NoGeneratorException {
        if (this.generator == null) {
            logger.printBukkit("Generator is null for dimension: " + name, true);
            worldExists = false;
            throw new NoGeneratorException("Dimension's Generator is not loaded in registry");
        }
        boolean successful = worldManager.addWorld(name, this.environmentType, this.seed, this.worldType, this.generateStructures, this.generator.getGeneratorName());
        if (successful) {
            dimensionManager.LOADED_DIMENSIONS.add(this);

            Dimension context;
            if (databaseManager.getEntityById(Dimension.class, UUIDGenerator.generateUUIDFromString(name).toString()) != null) {
                context = databaseManager.getEntityById(Dimension.class, UUIDGenerator.generateUUIDFromString(name).toString());
                context.setState(true);
            } else {
                context = new Dimension();
                context.setId(UUIDGenerator.generateUUIDFromString(name));
                context.setName(mainName);
                context.setState(true);
            }

            databaseManager.saveOrUpdate(context);
            return worldManager.getMVWorld(name).getCBWorld();
        } else {
            logger.printBukkit("Failed to generate Multiverse Dimension...", true);
            worldExists = false;
            return null;
        }
    }

    public void addPlayer(Player player) {
        this.players.add(player);
    }

    private void isRandomSpawning() {
        this.dimensionCharacteristics.add(DimensionCharacteristics.RANDOM_SPAWN);
    }

    private void isGlobalSpawning() {
        this.dimensionCharacteristics.add(DimensionCharacteristics.GLOBAL_SPAWN);
    }

    public World getWorld() {
        return this.world;
    }

    public boolean isPlayerInDimension(Player player) {
        return players.stream().anyMatch(p -> p.getUniqueId().equals(player.getUniqueId()));
    }

    public boolean takePlayerToDimension(Player player) {
        if (worldExists) {
            Optional<DimensionCharacteristics> globalSpawning = this.dimensionCharacteristics
                    .stream()
                    .filter(dimensionCharacteristics1 -> dimensionCharacteristics1.equals(DimensionCharacteristics.GLOBAL_SPAWN))
                    .findAny();
            if (globalSpawning.isPresent()) {
                Location targetLocation = new Location(this.world, 0.0, 100.0, 0.0);
                players.add(player);
                return player.teleport(targetLocation);
            } else {
                Random random = new Random();
                Location targetLocation = null;

                for (int attempt = 0; attempt < 10; attempt++) {
                    int x = random.nextInt(100000) - 50000;
                    int z = random.nextInt(100000) - 50000;
                    int y = this.world.getHighestBlockYAt(x, z);
                    Location allegedLocation = new Location(this.world, x, y, z);
                    Block block = allegedLocation.getBlock();
                    if (this.dimensionTypes.stream().anyMatch(dimensionType -> dimensionType.equals(DimensionType.AQUATIC_WORLD))) {
                        if (block.getType() == Material.WATER && allegedLocation.getBlockY() > 10) {
                            Block below = block.getRelative(0, -1, 0);
                            if (below.getType().isSolid()) {
                                allegedLocation.add(0.5, 1.0, 0.5);
                                targetLocation = allegedLocation;
                                break;
                            }
                        }
                    } else if (block.getType() == Material.WATER) {
                        Block below = block.getRelative(0, -1, 0);
                        if (below.getType().isSolid()) {
                            allegedLocation.add(0.5, 1.0, 0.5);
                            targetLocation = allegedLocation;
                            break;
                        }
                    }
                }

                if (targetLocation != null) {
                    players.add(player);
                    return player.teleport(targetLocation);
                } else {
                    player.sendMessage(Component.text("There was an issue trying to get you to that world").color(TextColor.fromHexString("#440000")).append(Component.text("[err: NSS]").decorate(TextDecoration.ITALIC, TextDecoration.BOLD)));
                    return false;
                }
            }
        } else {
            player.sendMessage(Component.text("There was an issue trying to get you to that world").color(TextColor.fromHexString("#440000")).append(Component.text("[err: WNX]").decorate(TextDecoration.ITALIC, TextDecoration.BOLD)));
            return false;
        }
    }

    protected void removePlayerFromDimension() {

    }

    public abstract void applyMechanics(Player var1);

    public abstract void disableMechanics(Player var1);

    public abstract void applyJoinMechanics(Player var1);

    protected void deleteDimension() {
        logger.printBukkit("Deleting dimension " + this.getName());
        worldManager.removePlayersFromWorld(this.getName());
        dimensionManager.LOADED_DIMENSIONS.remove(this);
        Dimension context;
        if (databaseManager.getEntityById(Dimension.class, UUIDGenerator.generateUUIDFromString(name).toString()) != null) {
            context = databaseManager.getEntityById(Dimension.class, UUIDGenerator.generateUUIDFromString(name).toString());
            databaseManager.deleteEntity(context);
        }
        worldManager.deleteWorld(this.getName());
    }

    protected void disableDimension() {
        logger.printBukkit("Disabling dimension " + this.getName());
        worldManager.removePlayersFromWorld(this.getName());
        dimensionManager.UNLOADED_DIMENSIONS.add(this);
        dimensionManager.LOADED_DIMENSIONS.remove(this);
        Dimension context;
        if (databaseManager.getEntityById(Dimension.class, UUIDGenerator.generateUUIDFromString(name).toString()) != null) {
            context = databaseManager.getEntityById(Dimension.class, UUIDGenerator.generateUUIDFromString(name).toString());
            context.setState(false);
        } else {
            context = new Dimension();
            context.setId(UUIDGenerator.generateUUIDFromString(name));
            context.setName(mainName);
            context.setState(false);
        }
        databaseManager.saveOrUpdate(context);
        worldManager.unloadWorld(this.getName());
    }

    protected void enableDimension() {
        logger.printBukkit("Enabling dimension " + this.getName());
        dimensionManager.LOADED_DIMENSIONS.add(this);
        dimensionManager.UNLOADED_DIMENSIONS.remove(this);
        Dimension context;
        if (databaseManager.getEntityById(Dimension.class, UUIDGenerator.generateUUIDFromString(name).toString()) != null) {
            context = databaseManager.getEntityById(Dimension.class, UUIDGenerator.generateUUIDFromString(name).toString());
            context.setState(true);
        } else {
            context = new Dimension();
            context.setId(UUIDGenerator.generateUUIDFromString(name));
            context.setName(mainName);
            context.setState(true);
        }
        databaseManager.saveOrUpdate(context);
        worldManager.loadWorld(this.getName());
    }

    public GuiCreator.ItemConfig getItemConfig(int position) {
        return new GuiCreator.ItemConfig(
                null,
                Component.text(this.mainName).color(TextColor.fromHexString(this.dimensionTypes.get(0).getHexCode())).toString(),
                Integer.toString(position),
                true,
                null,
                this.name,
                List.of("Dimension categories: " + this.dimensionTypes, (this.description != null ? "Description: " + this.description : ""))
        );
    }

    @Override
    public String getIdentifier() {
        return this.name;
    }
}
