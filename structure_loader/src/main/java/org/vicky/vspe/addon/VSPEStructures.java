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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.vicky.utilities.ANSIColor;
import org.vicky.vspe.addon.util.BaseStructure;
import org.vicky.vspe.addon.util.StructureLoader;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class VSPEStructures implements AddonInitializer  {
    @Inject
    private Logger logger;
    @Inject
    private Platform platform;
    @Inject
    private BaseAddon addon;
    @Inject
    private JavaPlugin plugin;

    private static final List<Class<? extends BaseStructure>> structures = new ArrayList<>();

    private static final HandlerList handlers = new HandlerList();

    public static void addStructures(List<Class<? extends BaseStructure>> structures) {
    }

    @Override
    public void initialize() {
        String directory = platform.getDataFolder().getAbsolutePath();
        String fileName = "structureJars.yml";
        String yamlContent = """
                Jars:
                  - name: "VSPE-0.0.1-ARI"
                    packages:
                      - "org.vicky.vspe.systems.Dimension.Dimensions.Structures"
                """;

        StructureLoader loader = new StructureLoader(logger);

        try {
            createYamlFile(directory, fileName, yamlContent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        FileConfiguration configuration = getYamlFile(directory, fileName);
        if (configuration != null) {
            logger.info(ANSIColor.colorize("purple[Processing structures...]"));
            List<Map<?, ?>> jarsList = configuration.getMapList("Jars");
            Map<String, List<String>> includedJars = getIncludedJars(jarsList);
            for (Map.Entry<String, List<String>> jar : includedJars.entrySet()) {
                logger.info(ANSIColor.colorize("green[Processing Jar: " + jar.getKey() + "]"));
                File jarFile = getJarFile(platform.getDataFolder().getParentFile().getAbsolutePath(), jar.getKey() + ".jar");
                if (jarFile != null) {
                    loader.scanPackagesInJar(jarFile, jar.getValue());
                }
                else {
                    logger.warn("Null jar file for file in yml: " + jar.getKey());
                }
            }
        }else {
            logger.warn("Configuration is null ;-;");
        }
        structures.addAll(loader.getLoadedClasses());

        platform.getEventManager()
                .getHandler(FunctionalEventHandler.class)
                .register(addon, ConfigPackPreLoadEvent.class)
                .then(event -> {
                    logger.info("The VSPE structures addon for terra is loading instanced structures");
                    ConfigPack pack = event.getPack();
                    CheckedRegistry<Structure> structureRegistry = pack.getOrCreateRegistry(Structure.class);
                    for (Class<? extends BaseStructure> clazz : structures) {
                        Constructor<? extends BaseStructure> constructor = null;
                        try {
                            constructor = clazz.getDeclaredConstructor();
                            constructor.setAccessible(true);
                            BaseStructure instance = constructor.newInstance();
                            instance.setPlatform(platform);
                            logger.info(ANSIColor.colorize("purple[Added structure: " + instance.getId() + "]"));
                            structureRegistry.register(instance);
                        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException |
                                 InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    }
                })
                .global();


    }

    @NotNull
    private static Map<String, List<String>> getIncludedJars(List<Map<?, ?>> jarsList) {
        Map<String, List<String>> includedJars = new HashMap<>();

        for (Map<?, ?> jarEntry : jarsList) {
            String jarName = (String) jarEntry.get("name"); // Get the JAR name
            List<String> jarPaths = (List<String>) jarEntry.get("packages"); // Get the list of packages

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
                        logger.info("Collected Jar as File");
                        return file;
                    }
                }
            }
        } else {
            logger.error("Folder does not exist or is not a directory: " + folderPath);
        }

        return null;
    }

    public void createYamlFile(String directoryPath, String fileName, String content) throws IOException {
        // Create the directory if it doesn't exist
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new IOException("Failed to create directory: " + directoryPath);
            }
        }

        // Create a File object for the YAML file
        File yamlFile = new File(directory, fileName);

        // Check if the file already exists
        if (yamlFile.exists()) {
            System.out.println("File already exists: " + yamlFile.getPath());
            return; // Do nothing if the file exists
        }

        // Create the YAML file
        if (yamlFile.createNewFile()) {
            // Write content to the file
            try (FileWriter writer = new FileWriter(yamlFile)) {
                writer.write(content);
            }
            logger.info("File created successfully: " + yamlFile.getPath());
        } else {
            throw new IOException("Failed to create file: " + yamlFile.getPath());
        }
    }

    public FileConfiguration getYamlFile(String directoryPath, String fileName) {
        // Create a File object for the YAML file
        File yamlFile = new File(directoryPath, fileName);

        // Check if the file exists
        if (!yamlFile.exists()) {
            logger.warn("YAML file not found: " + yamlFile.getPath());


            String directory = platform.getDataFolder().getAbsolutePath();
            String ymlName = "structureJars.yml";
            String yamlContent = """
                Jars:
                  - VSPE-0.0.1-ARI.jar
                """;

            try {
                createYamlFile(directory, ymlName, yamlContent);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return null; // Return null if the file doesn't exist
        }

        // Load the YAML file into a FileConfiguration object
        return YamlConfiguration.loadConfiguration(yamlFile);
    }

    public static void addStructureClass(Class<? extends BaseStructure> clazz) {
        structures.add(clazz);
    }
}
