package org.vicky.vspe.platform.systems.platformquestingintegration.interfaces;

import org.vicky.platform.PlatformPlayer;

public interface QuestObjective {
    void startObjective(PlatformPlayer player);
    void stopObjective(PlatformPlayer player);
    boolean isCompleted(PlatformPlayer player);
}
