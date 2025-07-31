package org.vicky.vspe.systems.dimension.Generator.utils.Biome.type.subEnums;

import org.vicky.vspe.systems.dimension.Generator.utils.Biome.type.BiomeType;

public enum River implements BiomeType {
    POLAR,
    TEMPERATE,
    TROPICAL;

    @Override
    public String getTemperate() {
        return this.name();
    }

    @Override
    public String getName() {
        return "RIVER_" + this.name();
    }

    @Override
    public boolean isCoast() {
        return false;
    }
}
