package org.vicky.vspe.systems.Dimension;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.reflections.Reflections;
import org.vicky.vspe.systems.Dimension.Generator.BaseGenerator;
import org.vicky.vspe.systems.Dimension.Generator.utils.progressbar.progressbars.ConsoleProgressBar;

import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Set;

public class DimensionManager {

    public static final Set<String> DIMENSION_PACKAGES = new HashSet<>();

    static {
        DIMENSION_PACKAGES.add("org.vicky.vspe.systems.Dimension.Dimensions");
    }

    public final Set<BaseDimension> LOADED_DIMENSIONS;

    public DimensionManager() {
        LOADED_DIMENSIONS = new HashSet<>();
    }

    public void processDimensionGenerators() {
        for (String packagePath : DIMENSION_PACKAGES) {
            Reflections reflections = new Reflections(packagePath);
            Set<Class<? extends BaseGenerator>> dimensionGenerators = reflections.getSubTypesOf(BaseGenerator.class);

            for (Class<? extends BaseGenerator> clazz : dimensionGenerators) {
                try {
                    // Assuming all advancements have a no-args constructor
                    Constructor<? extends BaseGenerator> constructor = clazz.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    BaseGenerator generator = constructor.newInstance();

                    generator.generatePack(new ConsoleProgressBar());
                } catch (Exception e) {
                    Bukkit.getLogger().severe("Failed to load generator: " + clazz.getName());
                    e.printStackTrace();
                }
            }
        }
    }

    public BaseDimension getPlayerDimension(Player player) {
        for (BaseDimension dimension : LOADED_DIMENSIONS) {
            if (dimension.isPlayerInDimension(player)) {
                return dimension;
            }
        }
        return null;
    }
}
