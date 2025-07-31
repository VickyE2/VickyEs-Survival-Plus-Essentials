package org.vicky.vspe.systems.dimension.Generator.utils.Biome.type.subEnums;

import org.vicky.vspe.systems.dimension.Generator.utils.Biome.type.BiomeType;

public enum Deep_Ocean_Flat implements BiomeType {
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
        return "DEEP_OCEAN_FLAT_" + this.name();
    }

    @Override
    public boolean isCoast() {
        return false;
    }
}
