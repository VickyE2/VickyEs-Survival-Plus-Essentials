package org.vicky.vspe.platform.systems.dimension;

import org.vicky.platform.PlatformPlayer;
import org.vicky.platform.world.PlatformWorld;
import org.vicky.utilities.ANSIColor;
import org.vicky.utilities.ContextLogger.ContextLogger;
import org.vicky.utilities.Version;
import org.vicky.vspe.platform.systems.dimension.Exceptions.ExceptionContext;
import org.vicky.vspe.platform.utilities.ExceptionDerivator;
import org.vicky.vspe.platform.utilities.Manager.EntityNotFoundException;
import org.vicky.vspe.platform.utilities.Manager.IdentifiableManager;
import org.vicky.vspe.systems.dimension.PlatformDimensionTickHandler;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public interface PlatformDimensionManager<T, N> extends IdentifiableManager {
    ContextLogger logger = new ContextLogger(ContextLogger.ContextType.SYSTEM, "DIMENSIONS");
    String YAML_FILE = "pack.yml";

    default void loadDimensions() {
        logger.print("Starting dimension load sequence...", false);

        // 1) Prepare / process generator packs (clean=false is conservative; change if you want)
        try {
            processDimensionGenerators(false);
        } catch (Exception e) {
            handleException(e, "processDimensionGenerators", ExceptionContext.GENERATOR);
            // Continue — we still attempt to process any already-available dimension definitions
        }

        // 2) Process dimension definitions (register metadata, etc.)
        try {
            processDimensions();
        } catch (Exception e) {
            handleException(e, "processDimensions", ExceptionContext.DIMENSION);
        }

        // 3) Attempt to create any unloaded dimensions (world creation)
        List<PlatformBaseDimension<T, N>> toLoad = getUnLoadedDimensions();
        if (toLoad == null || toLoad.isEmpty()) {
            logger.print("No unloaded dimensions found.", false);
        } else {
            for (PlatformBaseDimension<T, N> dim : toLoad) {
                try {
                    String name = dim.getName();
                    logger.print("Preparing dimension: " + name, false);

                    // Skip if already exists (double-check)
                    if (dim.dimensionExists()) {
                        logger.print("Dimension already exists, enabling: " + name, false);
                        try {
                            dim.enableDimension();
                        } catch (Exception e) {
                            handleException(e, name, ExceptionContext.DIMENSION);
                        }
                        continue;
                    }

                    // Create world. Implementations of createWorld may require main-thread context
                    try {
                        PlatformWorld<T, N> created = dim.createWorld(name);
                        if (created == null) {
                            handleException("createWorld returned null for " + name);
                            continue;
                        }

                        // Optionally enable mechanics immediately after creation
                        try {
                            dim.enableDimension();
                        } catch (Exception e) {
                            handleException(e, name, ExceptionContext.ENABLE_DIMENSION);
                        }

                        // Set up tick handler if available
                        try {
                            PlatformDimensionTickHandler handler = dim.getTickHandler();
                            if (handler != null) {
                                dim.setTickHandler(handler);
                            }
                        } catch (Exception e) {
                            // Not fatal — just log
                            handleException(e, name, ExceptionContext.DIMENSION);
                        }

                        logger.print("Successfully created & enabled dimension: " + name, false);

                    } catch (Exception e) {
                        handleException(e, name, ExceptionContext.CREATE_DIMENSION_WORLD);
                    }

                } catch (Exception e) {
                    // Defensive: log any unexpected failure for this dimension but continue
                    handleException(e, "unknown-dimension", ExceptionContext.DIMENSION);
                }
            }
        }

        logger.print("Dimension load sequence finished.", false);
    }
    List<PlatformBaseDimension<T, N>> getLoadedDimensions();
    List<PlatformBaseDimension<T, N>> getUnLoadedDimensions();

    static boolean shouldOverwrite(Path currentPack, Version newVersion) throws IOException {
        String nullable = extractVersionFromYaml(currentPack);
        if (nullable == null) return true;
        Version currentVersion = Version.parse(nullable);

        // Compare versions
        return isNewerThan(currentVersion, newVersion);
    }
    static boolean isNewerThan(Version thiz, Version other) {
        return thiz.compareTo(other) > 0;
    }
    /**
     * Extracts the version from the YAML file inside the ZIP.
     */
    static String extractVersionFromYaml(Path pack) {
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

    void processDimensionGenerators(boolean clean);
    void processDimensions();

    PlatformBaseDimension<T, N> getPlayerDimension(PlatformPlayer player);

    Optional<PlatformBaseDimension<T, N>> getDimension(String dimensionId);

    static void handleException(Exception e, String generatorName, ExceptionContext c) {
        String error = ExceptionDerivator.parseException(e);
        String errorMessage = ANSIColor.colorize("red[In " + c +" " + generatorName + " " + (error != null ? error : "An Unexpected error occurred.") + " You might wana report this...]");

        logger.print(errorMessage, true);
    }

    static void handleException(String message) {
        logger.print(message, true);
    }


    @Override
    default String getManagerId() {
        return "dimension_manager";
    }

    @Override
    default void disableEntity(String namespace) throws EntityNotFoundException {
        Optional<PlatformBaseDimension<T, N>> optional = getLoadedDimensions().stream().filter(k -> k.getIdentifier().equals(namespace)).findAny();
        if (optional.isPresent()) {
            PlatformBaseDimension<T, N> context = optional.get();
            context.disableDimension();
        } else {
            throw new EntityNotFoundException("Failed to locate entity with id: " + namespace);
        }
    }

    @Override
    default void enableEntity(String namespace) throws EntityNotFoundException {
        Optional<PlatformBaseDimension<T, N>> optional = getLoadedDimensions().stream().filter(k -> k.getIdentifier().equals(namespace)).findAny();
        if (optional.isPresent()) {
            PlatformBaseDimension<T, N> context = optional.get();
            context.enableDimension();
        } else {
            throw new EntityNotFoundException("Failed to locate entity with id: " + namespace);
        }
    }

    void openDimensionsGUI(PlatformPlayer player);
}