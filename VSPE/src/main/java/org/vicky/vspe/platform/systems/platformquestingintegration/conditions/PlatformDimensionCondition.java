package org.vicky.vspe.platform.systems.platformquestingintegration.conditions;

import org.vicky.vspe.platform.systems.platformquestingintegration.interfaces.QuestCondition;

public interface PlatformDimensionCondition extends QuestCondition {
    String[] getDimensions();
    boolean doNotBeIn();
}
