package org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Biome.type.subEnums;

import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Biome.type.BiomeType;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Biome.type.Land;

public enum Mountains_Large implements BiomeType, Land {
    BOREAL_COASTAL(true),
    POLAR_COASTAL(true),
    SUBTROPICAL_COASTAL(true),
    SUBTROPICAL(false),
    TEMPERATE_COAST(true),
    TEMPERATE(false),
    TROPICAL(false),
    TROPICAL_COAST(true);

    private final boolean isCoast;

    Mountains_Large(boolean isCoast) {
        this.isCoast = isCoast;
    }

    @Override
    public String getTemperate() {
        return this.name().split("_")[0];
    }

    @Override
    public String getName() {
        return "MOUNTAINS_LARGE_" + this.name();
    }

    @Override
    public boolean isCoast() {
        return this.isCoast;
    }
}
