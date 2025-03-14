package org.vicky.vspe.systems.Dimension;

import io.lumine.mythic.bukkit.utils.lib.jooq.exception.IOException;
import org.bukkit.entity.Player;
import org.reflections.Reflections;
import org.vicky.utilities.ANSIColor;
import org.vicky.utilities.ContextLogger.ContextLogger;
import org.vicky.utilities.Identifiable;
import org.vicky.vspe.addon.util.BaseStructure;
import org.vicky.vspe.systems.Dimension.Exceptions.MissingConfigrationException;
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
import org.vicky.vspe.utilities.ExceptionDerivator;
import org.vicky.vspe.utilities.Manager.EntityNotFoundException;
import org.vicky.vspe.utilities.Manager.IdentifiableManager;
import org.vicky.vspe.utilities.Manager.ManagerRegistry;
import org.vicky.vspe.utilities.Pair;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class DimensionManager implements IdentifiableManager {
    public static final Set<String> DIMENSION_PACKAGES = new HashSet<>();
    public static final Set<String> DIMENSION_ZIP_NAMES = new HashSet<>();
    private static final String YAML_FILE = "pack.yml";

    static {
        DIMENSION_PACKAGES.add("org.vicky.vspe.systems.Dimension.Dimensions");
    }

    public final List<BaseDimension> LOADED_DIMENSIONS = new ArrayList<>();
    public final List<BaseDimension> UNLOADED_DIMENSIONS = new ArrayList<>();
    public final Set<BaseGenerator> LOADED_GENERATORS = new HashSet<>();
    private final ContextLogger logger = new ContextLogger(ContextLogger.ContextType.SYSTEM, "DIMENSIONS");

    public DimensionManager() {
        ManagerRegistry.register(this);
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

    public void processDimensionGenerators(boolean clean) {
        logger.printBukkit("Starting Generators Processing...", ContextLogger.LogType.PENDING);
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
                            } catch (Exception e) {
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

                        CompletableFuture<Void> future =
                                CompletableFuture.supplyAsync(() -> {
                                    try {
                                        int timeTaken = generator.generatePack(new NullProgressBar());
                                        return new Pair<>(generator, timeTaken);
                                    } catch (StackOverflowError var19) {
                                        StackTraceElement[] stackTrace = var19.getStackTrace();
                                        StringBuilder classesInvolved = new StringBuilder();
                                        List<Class<?>> classesAdded = new ArrayList<>();
                                        logger.printBukkit(ANSIColor.colorize("Dimension " + generator.getPackID().toLowerCase().replace("_", " ") + " Has a dependency cycle..."), true);

                                        for (StackTraceElement element : stackTrace) {
                                            Class<?> context = null;
                                            try {
                                                context = Class.forName(element.getClassName());
                                            } catch (ClassNotFoundException e) {
                                                throw new RuntimeException(e);
                                            }
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
                                                        Class<?> finalContext1 = context;
                                                        if (classesAdded.stream().noneMatch(c -> c.isAssignableFrom(finalContext1))) {
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
                                                        Class<?> finalContext = context;
                                                        if (classesAdded.stream().noneMatch(c -> c.isAssignableFrom(finalContext))) {
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
                                                Class<?> finalContext2 = context;
                                                if (classesAdded.stream().noneMatch(c -> c.isAssignableFrom(finalContext2))) {
                                                    classesInvolved.append("Class - ").append(context.getName()).append(", \n");
                                                    classesAdded.add(context);
                                                }
                                            }

                                        }

                                        logger.printBukkit(ANSIColor.colorize("yellow[Involved: \n" + classesInvolved + "Were involved please check them]"), true);
                                        return new Pair<BaseGenerator, Integer>(null, null);
                                    } catch (Exception var20) {
                                        handleException(var20, clazz.getSimpleName());
                                        try {
                                            throw var20;
                                        } catch (java.io.IOException | MissingConfigrationException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                })
                                .thenAccept(pair -> {
                                    BaseGenerator gen = pair.first();
                                    Integer time = pair.second();
                                    if (gen != null) {
                                        logger.printBukkit(String.format("Pack %s has been successfully generated {%s}", gen.getPackName(), time), ContextLogger.LogType.SUCCESS);
                                        LOADED_GENERATORS.add(gen);
                                    }
                                })
                                .exceptionally(e -> {
                                    handleException("An Unexpected error occurred.");
                                    return null;
                                });

                        future.join();
                    } else {
                        LOADED_GENERATORS.add(generator);
                        logger.printBukkit(ANSIColor.colorize("green[Pack " + packName + " is up-to-date]"));
                    }
                } catch (Exception var21) {
                    handleException(var21, clazz.getSimpleName());
                }
            }
        }
    }

    public void processDimensions() {
        logger.printBukkit("Starting Dimensions Processing...", ContextLogger.LogType.PENDING);
        for (String packagePath : DIMENSION_PACKAGES) {
            Reflections reflections = new Reflections(packagePath);
            Set<Class<? extends BaseDimension>> dimensions = reflections.getSubTypesOf(BaseDimension.class);

            for (Class<? extends BaseDimension> clazz : dimensions) {
                try {
                    Constructor<? extends BaseDimension> constructor = clazz.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    BaseDimension dimension = constructor.newInstance();
                    LOADED_DIMENSIONS.add(dimension);
                    logger.printBukkit(ANSIColor.colorize("purple[Added dimension " + dimension.getMainName() + "]"));
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                         NoSuchMethodException var18) {
                    logger.printBukkit("Exception encountered on dimension load: " + var18.getMessage(), true);
                    handleExceptionD(var18, "-> Unloaded Context <-");
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

    private void handleException(Exception e, String generatorName) {
        String error = ExceptionDerivator.parseException(e);
        String errorMessage = ANSIColor.colorize("red[In generator " + generatorName + " " + (error != null ? error : "An Unexpected error occurred.") + " You might wana report this...]");

        logger.printBukkit(errorMessage, true);
    }

    private void handleExceptionD(Exception e, String dimensionName) {
        String error = ExceptionDerivator.parseException(e);
        String errorMessage = ANSIColor.colorize("red[In dimension " + dimensionName + " " + (error != null ? error : "An Unexpected error occurred.") + " You might wana report this...]");

        logger.printBukkit(errorMessage, true);
    }

    private void handleException(String message) {
        logger.printBukkit(message, true);
    }

    @Override
    public String getManagerId() {
        return "dimension_manager";
    }

    @Override
    public void removeEntity(String namespace) throws EntityNotFoundException {
        Optional<BaseDimension> optional = LOADED_DIMENSIONS.stream().filter(k -> k.getIdentifier().equals(namespace)).findAny();
        if (optional.isPresent()) {
            BaseDimension context = optional.get();
            context.deleteDimension();
        } else {
            throw new EntityNotFoundException("Failed to locate entity with id: " + namespace);
        }

    }

    @Override
    public void disableEntity(String namespace) throws EntityNotFoundException {
        Optional<BaseDimension> optional = LOADED_DIMENSIONS.stream().filter(k -> k.getIdentifier().equals(namespace)).findAny();
        if (optional.isPresent()) {
            BaseDimension context = optional.get();
            context.disableDimension();
        } else {
            throw new EntityNotFoundException("Failed to locate entity with id: " + namespace);
        }
    }

    @Override
    public void enableEntity(String namespace) throws EntityNotFoundException {
        Optional<BaseDimension> optional = LOADED_DIMENSIONS.stream().filter(k -> k.getIdentifier().equals(namespace)).findAny();
        if (optional.isPresent()) {
            BaseDimension context = optional.get();
            context.enableDimension();
        } else {
            throw new EntityNotFoundException("Failed to locate entity with id: " + namespace);
        }
    }

    public void openDimensionsGUI() {

    }

    @Override
    public List<Identifiable> getRegisteredEntities() {
        return new ArrayList<>(LOADED_DIMENSIONS);
    }

    @Override
    public List<Identifiable> getUnregisteredEntities() {
        return new ArrayList<>(LOADED_DIMENSIONS);
    }
}