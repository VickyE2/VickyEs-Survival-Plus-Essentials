package org.vicky.vspe.systems.Dimension;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.vicky.vspe.VSPE;
import org.vicky.vspe.systems.Dimension.Exceptions.WorldNotExistsException;
import org.vicky.vspe.systems.Dimension.Generator.BaseGenerator;
import org.vicky.vspe.utilities.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.vicky.vspe.utilities.global.GlobalResources.*;

public abstract class BaseDimension {

    private final String name;
    private final String mainName;
    private final World world;
    private final List<DimensionCharacteristics> dimensionCharacteristics;
    private final List<DimensionType> dimensionTypes;
    private final World.Environment environmentType;
    private final String seed;
    private final WorldType worldType;
    private final boolean generateStructures;
    private final BaseGenerator generator;

    public BaseDimension(String mainName, String name, List<DimensionType> dimensionTypes, World.Environment environmentType, String seed, WorldType worldType, boolean generateStructures, BaseGenerator generator) throws WorldNotExistsException {
        this.name = name;
        this.mainName = mainName;
        this.dimensionCharacteristics = new ArrayList<>();
        this.world = checkWorld();
        this.dimensionTypes = dimensionTypes;
        this.environmentType = environmentType;
        this.seed = seed;
        this.worldType = worldType;
        this.generateStructures = generateStructures;
        this.generator = generator;
        DimensionClass.registerCustomDimension(name);
        for (DimensionType dimensionType : dimensionTypes) {
            dimensionType.addDimension(this);
        }

        dimensionManager.LOADED_DIMENSIONS.add(this);
        Config.configs.put("Dimensions." + getMainName() + ".exists", false);
    }

    public String getName() {
        return name;
    }

    public String getMainName() {
        return mainName;
    }

    /**
     * This method must be implemented properly or an exception would be thrown
     *
     * @return This returns a world that defines what world this dimension belongs to.
     * <p style="color: red;">Make sure to add a check method to avoid overwriting worlds.</p>
     */
    private World checkWorld() throws WorldNotExistsException {
        World existingWorld = Bukkit.getWorld(getName());
        Boolean isConfiguredAsExisting = (Boolean) configManager.getConfigValue("Dimensions." + getMainName() + ".exists");

        if (isConfiguredAsExisting != null && isConfiguredAsExisting) {
            // Case 1: Config says the world exists
            if (existingWorld != null) {
                return existingWorld; // World already exists in Bukkit
            } else {
                throw new WorldNotExistsException("World with name '" + getName() + "' is marked as existing in the configuration, but it does not exist in Bukkit.");
            }
        } else {
            // Case 2: Config says the world does not exist or is not set
            if (existingWorld != null) {
                VSPE.getInstancedLogger().warning("From configuration, World of dimension " + this.mainName + "does not exist but is found in bukkit. Please verify that is the world.");
                return existingWorld; // Return the existing world
            } else {
                Config.configs.put("Dimensions." + getMainName() + ".exists", true);
                return createWorld(getName()); // Create a new world
            }
        }
    }

    /**
     * This method must be implemented properly or an exception would be thrown
     *
     * @param name The name of the dimension's world to be generated. it should follow bukkit's naming system
     * @return This returns a world that defines what world this dimension belongs to.
     */
    private World createWorld(String name) {
        worldManager.addWorld(
                name,
                environmentType,
                seed,
                worldType,
                generateStructures,
                generator.getGeneratorName()
        );

        return worldManager.getMVWorld("").getCBWorld();
    }

    private void isRandomSpawning() {
        dimensionCharacteristics.add(DimensionCharacteristics.RANDOM_SPAWN);
    }

    private void isGlobalSpawning() {
        dimensionCharacteristics.add(DimensionCharacteristics.GLOBAL_SPAWN);
    }

    public World getWorld() {
        return world;
    }

    /**
     * Checks if a player is in this dimension.
     *
     * @param player The player to check.
     * @return True if the player is in this dimension, false otherwise.
     */
    public boolean isPlayerInDimension(Player player) {
        return player.getWorld().equals(world);
    }

    /**
     * Teleports a player to a place in the dimension. Doesn't always return true ;-;.
     *
     * @param player The player to teleport.
     * @return True if the player is in this dimension, false otherwise.
     */
    public boolean takePlayerToDimension(Player player) {
        Optional<DimensionCharacteristics> globalSpawning = dimensionCharacteristics.stream()
                .filter(dimensionCharacteristics1 -> dimensionCharacteristics1.equals(DimensionCharacteristics.GLOBAL_SPAWN))
                .findAny();
        if (globalSpawning.isPresent()) {
            Location targetLocation = new Location(world, 0, 100, 0);
            return player.teleport(targetLocation);
        } else {
            Random random = new Random();
            Location targetLocation = null;

            for (int attempt = 0; attempt < 10; attempt++) { // Try 10 times to find a safe location
                int x = random.nextInt(100000) - 50000; // Random x between -50000 and 50000
                int z = random.nextInt(100000) - 50000;
                int y = world.getHighestBlockYAt(x, z); // Get the highest block's y-coordinate
                Location allegedLocation = new Location(world, x, y, z);

                Block block = allegedLocation.getBlock();

                if (dimensionTypes.stream().anyMatch(dimensionType -> dimensionType.equals(DimensionType.AQUATIC_WORLD))) {
                    if (block.getType() == Material.WATER && allegedLocation.getBlockY() > 10) {
                        Block below = block.getRelative(0, -1, 0);
                        if (below.getType().isSolid()) {
                            allegedLocation.add(0.5, 1.0, 0.5); // Position above the water surface
                            targetLocation = allegedLocation;
                            break; // Safe location found
                        }
                    }
                } else {
                    if (block.getType() == Material.WATER) {
                        Block below = block.getRelative(0, -1, 0);
                        if (below.getType().isSolid()) {
                            allegedLocation.add(0.5, 1.0, 0.5); // Position above the water surface
                            targetLocation = allegedLocation;
                            break; // Safe location found
                        }
                    }
                }
            }
            if (targetLocation != null) {
                player.teleport(targetLocation);
                return player.teleport(targetLocation);
            } else {
                player.sendMessage("Could not find a safe location to teleport to... Damn your luck");
                return false;
            }
        }
    }

    /**
     * Allows you to define mechanics specific to this dimension.
     *
     * @param player The player to apply mechanics to.
     */
    public abstract void applyMechanics(Player player);

    /**
     * Disable mechanics given to the player (Usually used on leave checker).
     *
     * @param player The player to disable mechanics for.
     */
    public abstract void disableMechanics(Player player);

    /**
     * Allows you to define mechanics to run when players join specific to this dimension.
     *
     * @param player The player to apply mechanics to.
     */
    public abstract void applyJoinMechanics(Player player);
}
