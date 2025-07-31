package org.vicky.vspe.systems.dimension.Generator.utils.Biome.type.subEnums;

import org.vicky.vspe.systems.dimension.Generator.utils.Biome.type.BiomeType;
import org.vicky.vspe.systems.dimension.Generator.utils.Biome.type.Land;

public enum Flat implements BiomeType, Land {
    BOREAL_COAST(true),
    BOREAL(false),
    POLAR_COAST(true),
    POLAR(true),
    SUBTROPICAL(false),
    TEMPERATE(false),
    TROPICAL(false),
    TROPICAL_COAST(true);

    private final boolean isCoast;

    Flat(boolean isCoast) {
        this.isCoast = isCoast;
    }

    @Override
    public String getTemperate() {
        return this.name().split("_")[0];
    }

    @Override
    public String getName() {
        return "FLAT_" + this.name();
    }

    @Override
    public boolean isCoast() {
        return this.isCoast;
    }
}
