package org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Biome.type.subEnums;

import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Biome.type.BiomeType;

public enum Coasts implements BiomeType {
    COAST_SMALL_BOREAL,
    COAST_SMALL_POLAR,
    COAST_SMALL_TROPICAL,
    COAST_SMALL_SUBTROPICAL,
    COAST_SMALL_TEMPERATE,
    COAST_LARGE_BOREAL,
    COAST_LARGE_POLAR,
    COAST_LARGE_TEMPERATE,
    COAST_LARGE_SUBTROPICAL,
    COAST_LARGE_TROPICAL;

    @Override
    public String getTemperate() {
        return this.name().split("_")[2];
    }

    @Override
    public String getName() {
        return this.name();
    }

    @Override
    public boolean isCoast() {
        return true;
    }
}
