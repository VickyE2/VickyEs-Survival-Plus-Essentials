package org.vicky.vspe.platform.systems.platformquestingintegration.interfaces;

import org.vicky.platform.PlatformPlayer;

public interface QuestVariable<T> {
    T resolve(PlatformPlayer player) throws Exception;
}
