package org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.subEnums;

import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.BiomeType;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.Land;

public enum Mountains_Small implements BiomeType, Land {
    BOREAL(false),
    POLAR(false),
    SUBTROPICAL(false),
    SUBTROPICAL_COAST(true),
    TEMPERATE(false),
    TEMPERATE_COAST(true),
    TROPICAL(false),
    TROPICAL_COAST(true);

    private final boolean isCoast;

    Mountains_Small(boolean isCoast) {
        this.isCoast = isCoast;
    }

    @Override
    public String getTemperate() {
        return name().split("_")[0];
    }

    @Override
    public String getName() {
        return "MOUNTAINS_SMALL_" + name();
    }

    public boolean isCoast() {
        return isCoast;
    }
}
