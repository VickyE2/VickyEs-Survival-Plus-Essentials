package org.vicky.vspe.platform.features.advancement;

import org.vicky.platform.PlatformPlayer;
import org.vicky.utilities.ContextLogger.ContextLogger;
import org.vicky.vspe.platform.features.advancement.Exceptions.AdvancementProcessingFailureException;
import org.vicky.vspe.platform.utilities.Manager.IdentifiableManager;

public interface AdvancementManager<T extends PlatformAdvancement> extends IdentifiableManager {
    ContextLogger logger = new ContextLogger(ContextLogger.ContextType.FEATURE, "ADVANCEMENT");

    void processAdvancements() throws AdvancementProcessingFailureException;

    boolean grantAdvancement(Class<? extends T> advancementClass, PlatformPlayer player);
    void grantAdvancement(String advancementId, PlatformPlayer player);

    void addAdvancement(T advancement);
    T getAdvancement(Class<? extends T> advancementClass);

    static ContextLogger getLogger() {
        return logger;
    }
    void setStorage(AdvancementStorage storage);
}