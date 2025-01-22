package org.vicky.vspe.systems.Dimension;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.reflections.Reflections;
import org.reflections.scanners.Scanner;
import org.vicky.vspe.VSPE;
import org.vicky.vspe.addon.util.BaseStructure;
import org.vicky.vspe.systems.Dimension.Generator.BaseGenerator;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.BaseBiome;
import org.vicky.vspe.systems.Dimension.Generator.utils.Extrusion.Extrusion;
import org.vicky.vspe.systems.Dimension.Generator.utils.Feature.Feature;
import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locator;
import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.BaseClass;
import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.MetaClass;
import org.vicky.vspe.systems.Dimension.Generator.utils.Palette.Palette;
import org.vicky.vspe.systems.Dimension.Generator.utils.progressbar.progressbars.ConsoleProgressBar;

public class DimensionManager {
   public static final Set<String> DIMENSION_PACKAGES = new HashSet<>();
   public static final Set<String> DIMENSION_ZIP_NAMES = new HashSet<>();
   public final Set<BaseDimension> LOADED_DIMENSIONS = new HashSet<>();

   public void processDimensionGenerators(boolean clean) {
      for (String packagePath : DIMENSION_PACKAGES) {
         Reflections reflections = new Reflections(packagePath, new Scanner[0]);
         Set<Class<? extends BaseGenerator>> dimensionGenerators = reflections.getSubTypesOf(BaseGenerator.class);

         for (Class<? extends BaseGenerator> clazz : dimensionGenerators) {
            try {
               Constructor<? extends BaseGenerator> constructor = clazz.getDeclaredConstructor();
               constructor.setAccessible(true);
               BaseGenerator generator = constructor.newInstance();
               DIMENSION_ZIP_NAMES.add(generator.getPackID() + "-" + generator.getPackVersion() + ".zip");
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException var18) {
               throw new RuntimeException(var18);
            }
         }

         if (clean) {
            File[] files = new File(VSPE.getPlugin().getDataFolder().getParent(), "Terra/packs/").listFiles(File::isFile);
            if (files != null) {
               for (File file : files) {
                  if (DIMENSION_ZIP_NAMES.stream().anyMatch(name -> name.equals(file.getName()))) {
                     file.delete();
                  }
               }
            }
         }

         for (Class<? extends BaseGenerator> clazz : dimensionGenerators) {
            try {
               Constructor<? extends BaseGenerator> constructor = clazz.getDeclaredConstructor();
               constructor.setAccessible(true);
               BaseGenerator generator = constructor.newInstance();

               try {
                  generator.generatePack(new ConsoleProgressBar());
               } catch (StackOverflowError var19) {
                  StackTraceElement[] stackTrace = var19.getStackTrace();
                  StringBuilder classesInvolved = new StringBuilder();
                  VSPE.getInstancedLogger().warning("Dimension " + generator.getGeneratorName() + " Has a dependency cycle");

                  for (StackTraceElement element : stackTrace) {
                     Class<?> context = Class.forName(element.getClassName());
                     if (BaseStructure.class.isAssignableFrom(context)) {
                        classesInvolved.append("Structure - ").append(context.getName()).append(", ");
                     } else if (BaseGenerator.class.isAssignableFrom(context)) {
                        classesInvolved.append("Generator - ").append(context.getName()).append(", ");
                     } else if (Feature.class.isAssignableFrom(context)) {
                        classesInvolved.append("Feature - ").append(context.getName()).append(", ");
                     } else if (BaseBiome.class.isAssignableFrom(context)) {
                        classesInvolved.append("Biome - ").append(context.getName()).append(", ");
                     } else if (MetaClass.class.isAssignableFrom(context)) {
                        classesInvolved.append("MetaClass - ").append(context.getName()).append(", ");
                     } else if (BaseClass.class.isAssignableFrom(context)) {
                        classesInvolved.append("BaseClass - ").append(context.getName()).append(", ");
                     } else if (Locator.class.isAssignableFrom(context)) {
                        classesInvolved.append("Locator - ").append(context.getName()).append(", ");
                     } else if (Palette.class.isAssignableFrom(context)) {
                        classesInvolved.append("Palette - ").append(context.getName()).append(", ");
                     } else if (Extrusion.class.isAssignableFrom(context)) {
                        classesInvolved.append("Extrusion - ").append(context.getName()).append(", ");
                     } else {
                        classesInvolved.append("Class - ").append(context.getName()).append(", ");
                     }
                  }

                  VSPE.getInstancedLogger().warning("Classes: " + classesInvolved + " Were involved please check them");
               } catch (Exception var20) {
                  VSPE.getInstancedLogger().warning("An unexpected error occurred: " + var20.getMessage());
                  var20.printStackTrace();
               }
            } catch (Exception var21) {
               Bukkit.getLogger().severe("Failed to load generator: " + clazz.getName());
               var21.printStackTrace();
            }
         }
      }
   }

   public BaseDimension getPlayerDimension(Player player) {
      for (BaseDimension dimension : this.LOADED_DIMENSIONS) {
         if (dimension.isPlayerInDimension(player)) {
            return dimension;
         }
      }

      return null;
   }

   static {
      DIMENSION_PACKAGES.add("org.vicky.vspe.systems.Dimension.Dimensions");
   }
}
