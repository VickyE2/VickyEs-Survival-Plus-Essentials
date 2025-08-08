package org.vicky.vspe.platform.systems.platformquestingintegration.Objectives;
import org.vicky.vspe.platform.systems.platformquestingintegration.interfaces.QuestObjective;

import java.util.Set;

/**
 * DimensionObjective is an objective that requires players to visit a set of target dimensions.
 */
public interface PlatformDimensionObjective<T> extends QuestObjective {
    Set<String> getTargetDimensions();
}
