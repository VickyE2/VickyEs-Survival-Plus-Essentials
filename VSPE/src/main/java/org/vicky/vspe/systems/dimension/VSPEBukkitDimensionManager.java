package org.vicky.vspe.systems.dimension;

import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.reflections.Reflections;
import org.vicky.platform.PlatformPlayer;
import org.vicky.utilities.ANSIColor;
import org.vicky.utilities.ContextLogger.ContextLogger;
import org.vicky.utilities.Identifiable;
import org.vicky.vspe.addon.util.BaseStructure;
import org.vicky.vspe.platform.systems.dimension.Exceptions.MissingConfigrationException;
import org.vicky.vspe.platform.systems.dimension.PlatformBaseDimension;
import org.vicky.vspe.platform.systems.dimension.PlatformDimensionManager;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.BaseGenerator;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Biome.BaseBiome;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Extrusion.Extrusion;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Feature.Feature;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Locator.Locator;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Meta.BaseClass;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Meta.MetaClass;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.NoiseSampler.NoiseSampler;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Palette.Palette;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.progressbar.progressbars.NullProgressBar;
import org.vicky.vspe.platform.utilities.Manager.EntityNotFoundException;
import org.vicky.vspe.platform.utilities.Manager.IdentifiableManager;
import org.vicky.vspe.platform.utilities.Manager.ManagerRegistry;
import org.vicky.vspe.utilities.ExceptionDerivator;
import org.vicky.vspe.utilities.Pair;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class VSPEBukkitDimensionManager implements PlatformDimensionManager<BlockData, World> {
    public static final Set<String> DIMENSION_PACKAGES = new HashSet<>();
    public static final Set<String> DIMENSION_ZIP_NAMES = new HashSet<>();

    static {
        DIMENSION_PACKAGES.add("org.vicky.vspe.systems.Dimension.Dimensions");
    }

    public final List<PlatformBaseDimension<BlockData, World>> LOADED_DIMENSIONS = new ArrayList<>();
    public final List<PlatformBaseDimension<BlockData, World>> UNLOADED_DIMENSIONS = new ArrayList<>();
    public final Set<BaseGenerator> LOADED_GENERATORS = new HashSet<>();
    private final ContextLogger logger = new ContextLogger(ContextLogger.ContextType.SYSTEM, "DIMENSIONS");

    public VSPEBukkitDimensionManager() {
        ManagerRegistry.register(this);
    }

    @Override
    public List<PlatformBaseDimension<BlockData, World>> getLoadedDimensions() {
        return LOADED_DIMENSIONS;
    }

    @Override
    public List<PlatformBaseDimension<BlockData, World>> getUnLoadedDimensions() {
        return UNLOADED_DIMENSIONS;
    }

    public void processDimensionGenerators(boolean clean) {
        logger.print("Starting Generators Processing...", ContextLogger.LogType.PENDING);
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
                    logger.print("Exception encountered: " + var18.getMessage(), true);
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
                                logger.print("Exception encountered:" + e.getMessage(), true);
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
                        logger.print(ANSIColor.colorize("purple[Updating pack " + packName + "]"));

                        CompletableFuture<Void> future =
                                CompletableFuture.supplyAsync(() -> {
                                    try {
                                        Duration timeTaken = generator.generatePack(new NullProgressBar());
                                        return new Pair<>(generator, timeTaken);
                                    }
                                    catch (StackOverflowError var19) {
                                        StackTraceElement[] stackTrace = var19.getStackTrace();
                                        StringBuilder classesInvolved = new StringBuilder();
                                        List<Class<?>> classesAdded = new ArrayList<>();
                                        logger.print(ANSIColor.colorize("Dimension " + generator.getPackID().toLowerCase().replace("_", " ") + " Has a dependency cycle..."), true);

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

                                        logger.print(ANSIColor.colorize("yellow[Involved: \n" + classesInvolved + "Were involved please check them]"), true);
                                        return new Pair<BaseGenerator, Integer>(null, null);
                                    }
                                    catch (Exception var20) {
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
                                    Duration time = (Duration) pair.second();
                                    if (gen != null) {
                                        logger.print(String.format("Pack %s has been successfully generated " + ANSIColor.ORANGE + "{in %sms}" + ANSIColor.RESET, gen.getPackName(), time.toMillisPart()), ContextLogger.LogType.SUCCESS);
                                        LOADED_GENERATORS.add(gen);
                                    }
                                })
                                .exceptionally(e -> {
                                    handleException((Exception) e, "An Unexpected error occurred: " + e.getCause() + " -> " + e.getMessage());
                                    return null;
                                });

                        future.join();
                    } else {
                        LOADED_GENERATORS.add(generator);
                        logger.print(ANSIColor.colorize("green[Pack " + packName + " is up-to-date]"));
                    }
                } catch (Exception var21) {
                    handleException(var21, clazz.getSimpleName());
                }
            }
        }
    }

    public void processDimensions() {
        logger.print("Starting Dimensions Processing...", ContextLogger.LogType.PENDING);
        for (String packagePath : DIMENSION_PACKAGES) {
            Reflections reflections = new Reflections(packagePath);
            Set<Class<? extends BukkitBaseDimension>> dimensions = reflections.getSubTypesOf(BukkitBaseDimension.class);

            for (Class<? extends BukkitBaseDimension> clazz : dimensions) {
                try {
                    Constructor<? extends BukkitBaseDimension> constructor = clazz.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    BukkitBaseDimension dimension = constructor.newInstance();
                    LOADED_DIMENSIONS.add(dimension);
                    logger.print(ANSIColor.colorize("purple[Added dimension " + dimension.getName() + "]"));
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                         NoSuchMethodException var18) {
                    logger.print("Exception encountered on dimension load: " + var18.getMessage(), true);
                    handleExceptionD(var18, "-> Unloaded Context <-");
                }
            }
        }
    }

    @Override
    public PlatformBaseDimension<BlockData, World> getPlayerDimension(PlatformPlayer platformPlayer) {
        return null;
    }

    public BukkitBaseDimension getPlayerDimension(Player player) {
        return LOADED_DIMENSIONS.stream()
                .filter(BukkitBaseDimension.class::isInstance)             // keep only BukkitBaseDimension
                .map(BukkitBaseDimension.class::cast)                      // cast safely
                .filter(dimension -> dimension.isPlayerInDimension(player)) // check if player is in it
                .findFirst()                                               // get the first match
                .orElse(null);                                             // null if none found
    }

    public Optional<PlatformBaseDimension<BlockData, World>> getDimension(String dimensionId) {
        return LOADED_DIMENSIONS.stream()
                .filter(BukkitBaseDimension.class::isInstance)              // keep only BukkitBaseDimension
                .map(BukkitBaseDimension.class::cast)                       // cast safely
                .filter(dimension -> dimension.getIdentifier().equals(dimensionId))
                .map(d -> (PlatformBaseDimension<BlockData, World>) d)                  // cast back to base type
                .findAny();
    }

    private void handleException(Exception e, String generatorName) {
        String error = ExceptionDerivator.parseException(e);
        String errorMessage = ANSIColor.colorize("red[In generator " + generatorName + " " + (error != null ? error : "An Unexpected error occurred.") + " You might wana report this...]");

        logger.print(errorMessage, true);
    }

    private void handleExceptionD(Exception e, String dimensionName) {
        String error = ExceptionDerivator.parseException(e);
        String errorMessage = ANSIColor.colorize("red[In dimension " + dimensionName + " " + (error != null ? error : "An Unexpected error occurred.") + " You might wana report this...]");

        logger.print(errorMessage, true);
    }

    private void handleException(String message) {
        logger.print(message, true);
    }

    @Override
    public String getManagerId() {
        return "dimension_manager";
    }

    @Override
    public void removeEntity(String namespace) throws EntityNotFoundException {
        Optional<BukkitBaseDimension> optional = LOADED_DIMENSIONS.stream().filter(BukkitBaseDimension.class::isInstance).map(BukkitBaseDimension.class::cast).filter(k -> k.getIdentifier().equals(namespace)).findAny();
        if (optional.isPresent()) {
            BukkitBaseDimension context = optional.get();
            context.deleteDimension();
        } else {
            throw new EntityNotFoundException("Failed to locate entity with id: " + namespace);
        }

    }

    @Override
    public void disableEntity(String namespace) throws EntityNotFoundException {
        Optional<BukkitBaseDimension> optional = LOADED_DIMENSIONS.stream().filter(BukkitBaseDimension.class::isInstance).map(BukkitBaseDimension.class::cast).filter(k -> k.getIdentifier().equals(namespace)).findAny();
        if (optional.isPresent()) {
            BukkitBaseDimension context = optional.get();
            context.disableDimension();
        } else {
            throw new EntityNotFoundException("Failed to locate entity with id: " + namespace);
        }
    }

    @Override
    public void enableEntity(String namespace) throws EntityNotFoundException {
        Optional<BukkitBaseDimension> optional = LOADED_DIMENSIONS.stream().filter(BukkitBaseDimension.class::isInstance).map(BukkitBaseDimension.class::cast).filter(k -> k.getIdentifier().equals(namespace)).findAny();
        if (optional.isPresent()) {
            BukkitBaseDimension context = optional.get();
            context.enableDimension();
        } else {
            throw new EntityNotFoundException("Failed to locate entity with id: " + namespace);
        }
    }

    @Override
    public void openDimensionsGUI(PlatformPlayer platformPlayer) {

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