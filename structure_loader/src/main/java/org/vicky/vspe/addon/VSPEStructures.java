package org.vicky.vspe.addon;

import com.dfsek.terra.addons.manifest.api.AddonInitializer;
import com.dfsek.terra.api.Platform;
import com.dfsek.terra.api.addon.BaseAddon;
import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.api.event.events.config.pack.ConfigPackPreLoadEvent;
import com.dfsek.terra.api.event.functional.FunctionalEventHandler;
import com.dfsek.terra.api.inject.annotations.Inject;
import com.dfsek.terra.api.registry.CheckedRegistry;
import com.dfsek.terra.api.structure.Structure;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.vicky.utilities.ANSIColor;
import org.vicky.vspe.addon.util.BaseStructure;
import org.vicky.vspe.addon.util.StructureLoader;

public class VSPEStructures implements AddonInitializer {
   @Inject
   private Logger logger;
   @Inject
   private Platform platform;
   @Inject
   private BaseAddon addon;
   @Inject
   private JavaPlugin plugin;
   private static final Map<String, List<Class<? extends BaseStructure>>> structures = new HashMap<>();
   private static final HandlerList handlers = new HandlerList();

   public static void addStructures(List<Class<? extends BaseStructure>> structures) {
   }

   public void initialize() {
      String directory = this.platform.getDataFolder().getAbsolutePath();
      String fileName = "structureJars.yml";
      String yamlContent = "Jars:\n  - name: \"VSPE-0.0.1-ARI\"\n    packages:\n      - \"org.vicky.vspe.systems.Dimension.Dimensions.Structures\"\n";
      StructureLoader loader = new StructureLoader(this.logger);

      try {
         this.createYamlFile(directory, fileName, yamlContent);
      } catch (IOException var11) {
         throw new RuntimeException(var11);
      }

      FileConfiguration configuration = this.getYamlFile(directory, fileName);
      if (configuration != null) {
         this.logger.info(ANSIColor.colorize("purple[Processing structures...]"));
         List<Map<?, ?>> jarsList = configuration.getMapList("Jars");
         Map<String, List<String>> includedJars = getIncludedJars(jarsList);

         for (Entry<String, List<String>> jar : includedJars.entrySet()) {
            this.logger.info(ANSIColor.colorize("green[Processing Jar: " + jar.getKey() + "]"));
            File jarFile = this.getJarFile(this.platform.getDataFolder().getParentFile().getAbsolutePath(), jar.getKey() + ".jar");
            if (jarFile != null) {
               loader.scanPackagesInJar(jarFile, jar.getValue());
            } else {
               this.logger.warn("Null jar file for file in yml: " + jar.getKey());
            }
         }
      } else {
         this.logger.warn("Configuration is null ;-;");
      }

      structures.putAll(loader.getLoadedClasses());
      this.platform.getEventManager().getHandler(FunctionalEventHandler.class)
         .register(this.addon, ConfigPackPreLoadEvent.class)
         .then(event -> {
            ConfigPack pack = event.getPack();
            this.logger.info(ANSIColor.colorize("The VSPE structures addon for terra is purple[loading instanced structures]"));
            CheckedRegistry<Structure> structureRegistry = pack.getOrCreateRegistry(Structure.class);
            StringBuilder structureBuilder = new StringBuilder(ANSIColor.colorize("yellow[Added Structures:]") + "[");
            for (Entry<String, List<Class<? extends BaseStructure>>> structures : VSPEStructures.structures.entrySet()) {
               for (Class<? extends BaseStructure> clazz : structures.getValue()) {
                  try {
                     Constructor<? extends BaseStructure> constructor = clazz.getDeclaredConstructor();
                     constructor.setAccessible(true);
                     BaseStructure instance = constructor.newInstance();
                     instance.setPlatform(this.platform);
                     structureBuilder.append(ANSIColor.colorize(" green[" + instance.getId() + "],"));
                     structureRegistry.register(instance);
                  } catch (NoSuchMethodException var10x) {
                     this.logger.error("No default constructor found for class: " + clazz.getName(), var10x);
                     throw new RuntimeException("No default constructor found for class: " + clazz.getName(), var10x);
                  } catch (IllegalAccessException var11x) {
                     this.logger.error("Constructor of class " + clazz.getName() + " is not accessible", var11x);
                     throw new RuntimeException("Constructor of class " + clazz.getName() + " is not accessible", var11x);
                  } catch (InstantiationException var12) {
                     this.logger.error("Failed to instantiate class: " + clazz.getName(), var12);
                     throw new RuntimeException("Failed to instantiate class: " + clazz.getName(), var12);
                  } catch (InvocationTargetException var13) {
                     this.logger.error("Constructor of class " + clazz.getName() + " threw an exception", var13.getCause());
                     throw new RuntimeException("Constructor of class " + clazz.getName() + " threw an exception", var13.getCause());
                  } catch (Exception var14) {
                     this.logger.error("Unexpected error occurred while instantiating class: " + clazz.getName(), var14);
                     throw new RuntimeException("Unexpected error occurred while instantiating class: " + clazz.getName(), var14);
                  }
               }
            }
            structureBuilder.append("]");
            this.logger.info(structureBuilder.toString());
         });
   }

   @NotNull
   private static Map<String, List<String>> getIncludedJars(List<Map<?, ?>> jarsList) {
      Map<String, List<String>> includedJars = new HashMap<>();

      for (Map<?, ?> jarEntry : jarsList) {
         String jarName = (String)jarEntry.get("name");
         List<String> jarPaths = (List<String>)jarEntry.get("packages");
         if (jarName != null && jarPaths != null) {
            includedJars.put(jarName, jarPaths);
         }
      }

      return includedJars;
   }

   public File getJarFile(String folderPath, String jarName) {
      File folder = new File(folderPath);
      if (folder.exists() && folder.isDirectory()) {
         File[] files = folder.listFiles((dir, name) -> name.endsWith(".jar"));
         if (files != null) {
            for (File file : files) {
               if (file.getName().equals(jarName)) {
                  this.logger.info("Collected Jar as File");
                  return file;
               }
            }
         }
      } else {
         this.logger.error("Folder does not exist or is not a directory: " + folderPath);
      }

      return null;
   }

   public void createYamlFile(String directoryPath, String fileName, String content) throws IOException {
      File directory = new File(directoryPath);
      if (!directory.exists() && !directory.mkdirs()) {
         throw new IOException("Failed to create directory: " + directoryPath);
      } else {
         File yamlFile = new File(directory, fileName);
         if (yamlFile.exists()) {
            System.out.println("File already exists: " + yamlFile.getPath());
         } else if (yamlFile.createNewFile()) {
            try (FileWriter writer = new FileWriter(yamlFile)) {
               writer.write(content);
            }

            this.logger.info("File created successfully: " + yamlFile.getPath());
         } else {
            throw new IOException("Failed to create file: " + yamlFile.getPath());
         }
      }
   }

   public FileConfiguration getYamlFile(String directoryPath, String fileName) {
      File yamlFile = new File(directoryPath, fileName);
      if (!yamlFile.exists()) {
         this.logger.warn("YAML file not found: " + yamlFile.getPath());
         String directory = this.platform.getDataFolder().getAbsolutePath();
         String ymlName = "structureJars.yml";
         String yamlContent = "Jars:\n  - VSPE-0.0.1-ARI.jar\n";

         try {
            this.createYamlFile(directory, ymlName, yamlContent);
            return null;
         } catch (IOException var8) {
            throw new RuntimeException(var8);
         }
      } else {
         return YamlConfiguration.loadConfiguration(yamlFile);
      }
   }
}
