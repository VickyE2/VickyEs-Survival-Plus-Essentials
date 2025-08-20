package org.vicky.vspe.platform.features.advancement;

import java.util.UUID;

public interface AdvancementStorage {
    void save(UUID playerId, String advancementId);
    void load(UUID playerId);
}
