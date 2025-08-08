package org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Biome.type.subEnums;

import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Biome.type.BiomeType;

public enum Deep_Ocean_Hilly implements BiomeType {
    BOREAL,
    POLAR,
    SUBTROPICAL,
    TEMPERATE,
    TROPICAL;

    @Override
    public String getTemperate() {
        return this.name();
    }

    @Override
    public String getName() {
        return "DEEP_OCEAN_HILLY_" + this.name();
    }

    @Override
    public boolean isCoast() {
        return false;
    }
}
