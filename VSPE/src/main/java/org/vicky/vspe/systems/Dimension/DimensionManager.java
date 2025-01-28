package org.vicky.vspe.systems.Dimension;

import io.lumine.mythic.bukkit.utils.lib.jooq.exception.IOException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.reflections.Reflections;
import org.vicky.utilities.ANSIColor;
import org.vicky.vspe.VSPE;
import org.vicky.vspe.addon.util.BaseStructure;
import org.vicky.vspe.systems.ContextLogger.ContextLogger;
import org.vicky.vspe.systems.Dimension.Generator.BaseGenerator;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.BaseBiome;
import org.vicky.vspe.systems.Dimension.Generator.utils.Extrusion.Extrusion;
import org.vicky.vspe.systems.Dimension.Generator.utils.Feature.Feature;
import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locator;
import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.BaseClass;
import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.MetaClass;
import org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSampler.NoiseSampler;
import org.vicky.vspe.systems.Dimension.Generator.utils.Palette.Palette;
import org.vicky.vspe.systems.Dimension.Generator.utils.progressbar.progressbars.NullProgressBar;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class DimensionManager {
    public static final Set<String> DIMENSION_PACKAGES = new HashSet<>();
    public static final Set<String> DIMENSION_ZIP_NAMES = new HashSet<>();
    private static final String YAML_FILE = "pack.yml";
    private ContextLogger logger = new ContextLogger(ContextLogger.ContextType.SYSTEM, "DIMENSIONS");

    static {
        DIMENSION_PACKAGES.add("org.vicky.vspe.systems.Dimension.Dimensions");
    }

    public final Set<BaseDimension> LOADED_DIMENSIONS = new HashSet<>();

    public void processDimensionGenerators(boolean clean) {
        for (String packagePath : DIMENSION_PACKAGES) {
            Reflections reflections = new Reflections(packagePath);
            Set<Class<? extends BaseGenerator>> dimensionGenerators = reflections.getSubTypesOf(BaseGenerator.class);

            for (Class<? extends BaseGenerator> clazz : dimensionGenerators) {
                try {
                    Constructor<? extends BaseGenerator> constructor = clazz.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    BaseGenerator generator = constructor.newInstance();
                    DIMENSION_ZIP_NAMES.add(generator.getPackID() + "-" + generator.getPackVersion() + ".zip");
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                         NoSuchMethodException var18) {
                    logger.printBukkit("Exception encountered: " + var18.getMessage(), true);
                }
            }

            if (clean) {
                File[] files = new File("./plugins/Terra/packs/").listFiles(File::isFile);
                if (files != null) {
                    for (File file : files) {
                        if (DIMENSION_ZIP_NAMES.stream().anyMatch(name -> name.equals(file.getName()))) {
                            try {
                                file.delete();
                            }catch (Exception e) {
                                logger.printBukkit("Exception encountered:" + e.getMessage(), true);
                            }
                        }
                    }
                }
            }

            for (Class<? extends BaseGenerator> clazz : dimensionGenerators) {
                try {
                    Constructor<? extends BaseGenerator> constructor = clazz.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    BaseGenerator generator = constructor.newInstance();
                    String packName = generator.getPackName();
                    if (shouldOverwrite(Paths.get("./plugins/Terra/packs/" + packName), generator.getPackVersion())) {
                        logger.printBukkit(ANSIColor.colorize("purple[Updating pack " + packName + "]"));
                        try {
                            generator.generatePack(new NullProgressBar());
                        } catch (StackOverflowError var19) {
                            StackTraceElement[] stackTrace = var19.getStackTrace();
                            StringBuilder classesInvolved = new StringBuilder();
                            List<Class<?>> classesAdded = new ArrayList<>();
                            logger.printBukkit(ANSIColor.colorize("Dimension " + generator.getPackID().toLowerCase().replace("_", " ") + " Has a dependency cycle..."), true);

                            for (StackTraceElement element : stackTrace) {
                                Class<?> context = Class.forName(element.getClassName());
                                Map<Class<?>, String> classDescriptors = Map.of(
                                        BaseStructure.class, "Structure",
                                        BaseGenerator.class, "Generator",
                                        Feature.class, "Feature",
                                        BaseBiome.class, "Biome",
                                        MetaClass.class, "MetaClass",
                                        BaseClass.class, "BaseClass",
                                        Locator.class, "Locator",
                                        Palette.class, "Palette",
                                        Extrusion.class, "Extrusion",
                                        NoiseSampler.class, "NoiseSampler"
                                );

                                boolean matched = false;
                                for (Map.Entry<Class<?>, String> entry : classDescriptors.entrySet()) {
                                    if (entry.getKey().isAssignableFrom(context)) {
                                        if (entry.getKey() == BaseStructure.class) {
                                            if (classesAdded.stream().noneMatch(c -> c.isAssignableFrom(context))) {
                                                classesInvolved.append(entry.getValue()).append(" - ")
                                                        .append(context.getSimpleName()).append(" Package: ")
                                                        .append(context.getPackage().getName());
                                                if (stackTrace.length > 1) {
                                                    StackTraceElement caller = stackTrace[1];
                                                    String callingClass = caller.getClassName();
                                                    String callingMethod = caller.getMethodName();

                                                    classesInvolved.append(" Calling class: ").append(callingClass)
                                                            .append(" Calling method: ").append(callingMethod);
                                                }
                                                classesAdded.add(context);
                                            }
                                        } else {
                                            if (classesAdded.stream().noneMatch(c -> c.isAssignableFrom(context))) {
                                                classesInvolved.append(entry.getValue()).append(" - ")
                                                        .append(context.getSimpleName()).append(" Package: ")
                                                        .append(context.getPackage().getName()).append(", \n");
                                                classesAdded.add(context);
                                            }
                                        }
                                        matched = true;
                                    }
                                }
                                if (!matched) {
                                    if (classesAdded.stream().noneMatch(c -> c.isAssignableFrom(context))) {
                                        classesInvolved.append("Class - ").append(context.getName()).append(", \n");
                                        classesAdded.add(context);
                                    }
                                }

                            }

                            logger.printBukkit(ANSIColor.colorize("yellow[Involved: \n" + classesInvolved + "Were involved please check them]"), true);
                        } catch (Exception var20) {
                            handleException(var20, clazz.getSimpleName());
                        }
                    }else {
                        logger.printBukkit(ANSIColor.colorize("green[Pack " + packName + " is up-to-date]"));
                    }
                } catch (Exception var21) {
                    logger.printBukkit(ANSIColor.colorize("red[Failed to load generator: " + var21.getCause() + "]"), true);
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

    private static boolean shouldOverwrite(Path currentPack, String newVersion) throws IOException {
        String currentVersion = extractVersionFromYaml(currentPack);

        // If the current version is missing, assume overwrite is needed
        if (currentVersion == null) {
            return true;
        }

        // Compare versions
        return !currentVersion.equals(newVersion);
    }

    /**
     * Extracts the version from the YAML file inside the ZIP.
     */
    private static String extractVersionFromYaml(Path pack) throws IOException {
        if (!Files.exists(pack)) {
            return null;
        }

        try (ZipFile zipFile = new ZipFile(pack.toFile())) {
            ZipEntry entry = zipFile.getEntry(YAML_FILE);
            if (entry == null) {
                return null; // YAML file not found
            }

            try (InputStream is = zipFile.getInputStream(entry)) {
                Yaml yaml = new Yaml();
                Map<String, Object> data = yaml.load(is);
                return (String) data.get("version");
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleException(Exception e, String generatorName) {
        StringBuilder errorMessage = new StringBuilder();
        errorMessage.append("An error occurred during dimension generation");
        if (generatorName != null) {
            errorMessage.append(" for generator: ").append(generatorName);
        }
        errorMessage.append("\n");

        Throwable cause = e.getCause();
        if (cause != null) {
            errorMessage.append("[").append(ANSIColor.colorize("red[SYSTEMS-DIMENSIONS]")).append("] ").append("Cause: ").append(cause.getMessage()).append("\n");
        }

        for (StackTraceElement element : e.getStackTrace()) {
            errorMessage.append("[").append(ANSIColor.colorize("red[SYSTEMS-DIMENSIONS]")).append("] ").append("Class: ").append(element.getClassName())
                    .append(" | Method: ").append(element.getMethodName())
                    .append(" | Line: ").append(element.getLineNumber());

            try {
                // Check for null fields in the current class (if applicable)
                Class<?> clazz = Class.forName(element.getClassName());
                if (clazz != null) {
                    Field[] fields = clazz.getDeclaredFields();
                    for (Field field : fields) {
                        field.setAccessible(true);
                        if (field.getType() != null && field.get(clazz) == null) {
                            errorMessage.append(" | Field '").append(field.getName())
                                    .append("' in ").append(clazz.getSimpleName())
                                    .append(" is null.\n");
                        }
                    }
                } else {
                    errorMessage.append("\n");
                }
            } catch (Exception reflectionException) {
                errorMessage.append("[").append(ANSIColor.colorize("red[SYSTEMS-DIMENSIONS]")).append("] ").append("Error during reflection: ").append(reflectionException.getMessage()).append("\n");
            }
        }

        logger.printBukkit(errorMessage.toString(), true);
    }
}
