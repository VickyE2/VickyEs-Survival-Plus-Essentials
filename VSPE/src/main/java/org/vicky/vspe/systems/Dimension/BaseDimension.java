package org.vicky.vspe.systems.Dimension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.vicky.vspe.VSPE;
import org.vicky.vspe.systems.Dimension.Exceptions.WorldNotExistsException;
import org.vicky.vspe.systems.Dimension.Generator.BaseGenerator;
import org.vicky.vspe.utilities.Config;
import org.vicky.vspe.utilities.global.GlobalResources;

public abstract class BaseDimension {
   private final String name;
   private final String mainName;
   private final World world;
   private final List<DimensionCharacteristics> dimensionCharacteristics;
   private final List<DimensionType> dimensionTypes;
   private final Environment environmentType;
   private final String seed;
   private final WorldType worldType;
   private final boolean generateStructures;
   private final BaseGenerator generator;

   public BaseDimension(
      String mainName,
      String name,
      List<DimensionType> dimensionTypes,
      Environment environmentType,
      String seed,
      WorldType worldType,
      boolean generateStructures,
      BaseGenerator generator
   ) throws WorldNotExistsException {
      this.name = name;
      this.mainName = mainName;
      this.dimensionCharacteristics = new ArrayList<>();
      this.world = this.checkWorld();
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

      GlobalResources.dimensionManager.LOADED_DIMENSIONS.add(this);
      Config.configs.put("Dimensions." + this.getMainName() + ".exists", false);
   }

   public String getName() {
      return this.name;
   }

   public String getMainName() {
      return this.mainName;
   }

   private World checkWorld() throws WorldNotExistsException {
      World existingWorld = Bukkit.getWorld(this.getName());
      Boolean isConfiguredAsExisting = (Boolean)GlobalResources.configManager.getConfigValue("Dimensions." + this.getMainName() + ".exists");
      if (isConfiguredAsExisting != null && isConfiguredAsExisting) {
         if (existingWorld != null) {
            return existingWorld;
         } else {
            throw new WorldNotExistsException(
               "World with name '" + this.getName() + "' is marked as existing in the configuration, but it does not exist in Bukkit."
            );
         }
      } else if (existingWorld != null) {
         VSPE.getInstancedLogger()
            .warning("From configuration, World of dimension " + this.mainName + "does not exist but is found in bukkit. Please verify that is the world.");
         return existingWorld;
      } else {
         Config.configs.put("Dimensions." + this.getMainName() + ".exists", true);
         return this.createWorld(this.getName());
      }
   }

   private World createWorld(String name) {
      GlobalResources.worldManager.addWorld(name, this.environmentType, this.seed, this.worldType, this.generateStructures, this.generator.getGeneratorName());
      return GlobalResources.worldManager.getMVWorld("").getCBWorld();
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
      return player.getWorld().equals(this.world);
   }

   public boolean takePlayerToDimension(Player player) {
      Optional<DimensionCharacteristics> globalSpawning = this.dimensionCharacteristics
         .stream()
         .filter(dimensionCharacteristics1 -> dimensionCharacteristics1.equals(DimensionCharacteristics.GLOBAL_SPAWN))
         .findAny();
      if (globalSpawning.isPresent()) {
         Location targetLocation = new Location(this.world, 0.0, 100.0, 0.0);
         return player.teleport(targetLocation);
      } else {
         Random random = new Random();
         Location targetLocation = null;

         for (int attempt = 0; attempt < 10; attempt++) {
            int x = random.nextInt(100000) - 50000;
            int z = random.nextInt(100000) - 50000;
            int y = this.world.getHighestBlockYAt(x, z);
            Location allegedLocation = new Location(this.world, (double)x, (double)y, (double)z);
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
            player.teleport(targetLocation);
            return player.teleport(targetLocation);
         } else {
            player.sendMessage("Could not find a safe location to teleport to... Damn your luck");
            return false;
         }
      }
   }

   public abstract void applyMechanics(Player var1);

   public abstract void disableMechanics(Player var1);

   public abstract void applyJoinMechanics(Player var1);
}
