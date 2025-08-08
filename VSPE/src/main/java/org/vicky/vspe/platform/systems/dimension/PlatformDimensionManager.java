package org.vicky.vspe.platform.systems.dimension;

import org.vicky.platform.PlatformPlayer;
import org.vicky.utilities.ANSIColor;
import org.vicky.utilities.ContextLogger.ContextLogger;
import org.vicky.utilities.Version;
import org.vicky.vspe.platform.systems.dimension.Exceptions.ExceptionContext;
import org.vicky.vspe.platform.utilities.ExceptionDerivator;
import org.vicky.vspe.platform.utilities.Manager.EntityNotFoundException;
import org.vicky.vspe.platform.utilities.Manager.IdentifiableManager;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public interface PlatformDimensionManager extends IdentifiableManager {
    ContextLogger logger = new ContextLogger(ContextLogger.ContextType.SYSTEM, "DIMENSIONS");
    String YAML_FILE = "pack.yml";

    List<PlatformBaseDimension> getLoadedDimensions();
    List<PlatformBaseDimension> getUnLoadedDimensions();


    private static boolean shouldOverwrite(Path currentPack, Version newVersion) throws IOException {
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

    void processDimensionGenerators(boolean clean);
    void processDimensions();

    public PlatformBaseDimension getPlayerDimension(PlatformPlayer player);

    public Optional<PlatformBaseDimension> getDimension(String dimensionId);

    private void handleException(Exception e, String generatorName, ExceptionContext c) {
        String error = ExceptionDerivator.parseException(e);
        String errorMessage = ANSIColor.colorize("red[In " + c +" " + generatorName + " " + (error != null ? error : "An Unexpected error occurred.") + " You might wana report this...]");

        logger.print(errorMessage, true);
    }

    private void handleException(String message) {
        logger.print(message, true);
    }


    @Override
    default String getManagerId() {
        return "dimension_manager";
    }

    @Override
    default void disableEntity(String namespace) throws EntityNotFoundException {
        Optional<PlatformBaseDimension> optional = getLoadedDimensions().stream().filter(k -> k.getIdentifier().equals(namespace)).findAny();
        if (optional.isPresent()) {
            PlatformBaseDimension context = optional.get();
            context.disableDimension();
        } else {
            throw new EntityNotFoundException("Failed to locate entity with id: " + namespace);
        }
    }

    @Override
    default void enableEntity(String namespace) throws EntityNotFoundException {
        Optional<PlatformBaseDimension> optional = getLoadedDimensions().stream().filter(k -> k.getIdentifier().equals(namespace)).findAny();
        if (optional.isPresent()) {
            PlatformBaseDimension context = optional.get();
            context.enableDimension();
        } else {
            throw new EntityNotFoundException("Failed to locate entity with id: " + namespace);
        }
    }

    void openDimensionsGUI();
}