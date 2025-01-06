package org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.subEnums;

import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.BiomeType;

public enum Cave implements BiomeType {
    SMALL_BOREAL,
    SMALL_POLAR,
    SMALL_TROPICAL,
    SMALL_SUBTROPICAL,
    SMALL_TEMPERATE,
    LARGE_BOREAL,
    LARGE_POLAR,
    LARGE_TEMPERATE,
    LARGE_SUBTROPICAL,
    LARGE_TROPICAL;
    @Override
    public String getTemperate() {
        return return name().split("_")[1];
    }

    @Override
    public String getName() {
        return "CAVE_" + name();
    }

    @Override
    public boolean isCoast() {
        return false;
    }
}
