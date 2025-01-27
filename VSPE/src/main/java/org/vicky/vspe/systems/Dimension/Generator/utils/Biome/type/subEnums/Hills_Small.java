package org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.subEnums;

import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.BiomeType;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.Land;

public enum Hills_Small implements BiomeType, Land {
    BOREAL_COAST(true),
    BOREAL(false),
    POLAR_COAST(true),
    POLAR(false),
    SUBTROPICAL(false),
    TEMPERATE_COAST(true),
    TEMPERATE(false),
    TROPICAL(false),
    TROPICAL_COAST(true);

    private final boolean isCoast;

    Hills_Small(boolean isCoast) {
        this.isCoast = isCoast;
    }

    @Override
    public String getTemperate() {
        return this.name().split("_")[0];
    }

    @Override
    public String getName() {
        return "HILLS_SMALL_" + this.name();
    }

    @Override
    public boolean isCoast() {
        return this.isCoast;
    }
}
