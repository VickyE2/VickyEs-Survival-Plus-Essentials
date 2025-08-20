package org.vicky.vspe.platform;

import org.vicky.platform.PlatformConfig;
import org.vicky.platform.PlatformLogger;
import org.vicky.platform.PlatformScheduler;
import org.vicky.vspe.platform.features.CharmsAndTrinkets.PlatformTrinketManager;
import org.vicky.vspe.platform.features.advancement.PlatformAdvancementManager;
import org.vicky.vspe.platform.systems.dimension.PlatformDimensionManager;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.PlatformBiome;
import org.vicky.vspe.platform.systems.platformquestingintegration.QuestProductionFactory;

import java.io.File;

public interface VSPEPlatformPlugin {
    static VSPEPlatformPlugin get() {
        if (VSPEPlatformPlugin.Holder.INSTANCE == null) {
            throw new IllegalStateException("VSPEPlatformPlugin has not been initialized!");
        } else {
            return VSPEPlatformPlugin.Holder.INSTANCE;
        }
    }

    static void set(VSPEPlatformPlugin instance) {
        if (VSPEPlatformPlugin.Holder.INSTANCE == null) {
            VSPEPlatformPlugin.Holder.INSTANCE = instance;
        } else {
            throw new IllegalStateException("Cannot set VSPEPlatformPlugin after its already been set.");
        }
    }

    /**
     * Only the instance that was registered can unregister itself.
     */
    default void unregister() {
        if (VSPEPlatformPlugin.Holder.INSTANCE == this) {
            VSPEPlatformPlugin.Holder.INSTANCE = null;
        } else {
            throw new IllegalStateException("Only the registered VSPEPlatformPlugin instance can unregister itself!");
        }
    }

    static PlatformScheduler scheduler() {
        return get().getPlatformScheduler();
    }
    static File dataFolder() {
        return get().getPlatformDataFolder();
    }
    static PlatformConfig config() {
        return get().getPlatformConfig();
    }
    static ClassLoader classLoader() {
        return get().getClass().getClassLoader();
    }
    static PlatformLogger platformLogger() {
        return get().getPlatformLogger();
    }
    static PlatformDimensionManager<?, ?> dimensionManager() {
        return get().getDimensionManager();
    }
    static PlatformTrinketManager<?> trinketManager() {
        return get().getPlatformTrinketManager();
    }
    static PlatformStructureManager<?> structureManager() {
        return get().getPlatformStructureManager();
    }
    static PlatformBlockDataRegistry<?> blockStateCreator() {
        return get().getPlatformBlockDataRegistry();
    }
    static QuestProductionFactory questFactory() {
        return get().getQuestProductionFactory();
    }
    static PlatformAdvancementManager<?> advancementManager() {
        return get().getPlatformAdvancementManager();
    }
    static <B extends PlatformBiome> PlatformBiomeFactory<B> biomeFactory() {
        return get().getPlatformBiomeFactory();
    }

    PlatformScheduler getPlatformScheduler();
    PlatformStructureManager<?> getPlatformStructureManager();
    PlatformBlockDataRegistry<?> getPlatformBlockDataRegistry();
    PlatformConfig getPlatformConfig();
    File getPlatformDataFolder();
    PlatformLogger getPlatformLogger();
    PlatformDimensionManager<?, ?> getDimensionManager();
    PlatformTrinketManager<?> getPlatformTrinketManager();
    QuestProductionFactory getQuestProductionFactory();
    PlatformAdvancementManager<?> getPlatformAdvancementManager();
    <B extends PlatformBiome> PlatformBiomeFactory<B> getPlatformBiomeFactory();

    class Holder {
        private static volatile VSPEPlatformPlugin INSTANCE;

        public Holder() {
        }
    }
}
