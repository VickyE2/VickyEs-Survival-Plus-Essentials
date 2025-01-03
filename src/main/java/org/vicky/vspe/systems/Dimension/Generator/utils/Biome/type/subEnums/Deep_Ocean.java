package org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.subEnums;

import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.BiomeType;

public enum Deep_Ocean implements BiomeType {
    BOREAL,
    POLAR,
    SUBTROPICAL,
    TEMPERATE,
    TROPICAL;

    @Override
    public String getTemperate() {
        return name();
    }

    @Override
    public String getName() {
        return "DEEP_OCEAN_" + name();
    }

    @Override
    public boolean isCoast() {
        return false;
    }
}
