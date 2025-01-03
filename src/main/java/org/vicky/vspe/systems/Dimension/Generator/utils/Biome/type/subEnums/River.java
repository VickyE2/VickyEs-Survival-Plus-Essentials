package org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.subEnums;

import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.BiomeType;

public enum River implements BiomeType {
    POLAR,
    TEMPERATE,
    TROPICAL;

    @Override
    public String getTemperate() {
        return name();
    }

    @Override
    public String getName() {
        return "RIVER_" + name();
    }

    @Override
    public boolean isCoast() {
        return false;
    }
}
