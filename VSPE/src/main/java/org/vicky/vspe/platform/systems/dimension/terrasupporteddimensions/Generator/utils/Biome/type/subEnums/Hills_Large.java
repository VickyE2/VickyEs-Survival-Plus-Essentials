package org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Biome.type.subEnums;

import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Biome.type.BiomeType;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Biome.type.Land;

public enum Hills_Large implements BiomeType, Land {
    BOREAL_COAST(true),
    BOREAL_HUMID(false),
    BOREAL_SEMI_HUMID(false),
    POLAR_COAST(true),
    SUBTROPICAL_ARID(false),
    SUBTROPICAL_HUMID(false),
    SUBTROPICAL_SEMI_ARID(false),
    SUBTROPICAL_SEMI_HUMID(false),
    TEMPERATE_COAST(true),
    TEMPERATE_SEMI_ARID(false),
    TEMPERATE_SEMI_HUMID(false),
    TROPICAL_ARID(false),
    TROPICAL_COAST(true),
    TROPICAL_HUMID(false),
    TROPICAL_SEMI_ARID(false),
    TROPICAL_SEMI_HUMID(false);

    private final boolean isCoast;

    Hills_Large(boolean isCoast) {
        this.isCoast = isCoast;
    }

    @Override
    public String getTemperate() {
        return this.name().split("_")[0];
    }

    @Override
    public String getName() {
        return "HILLS_LARGE_" + this.name();
    }

    @Override
    public boolean isCoast() {
        return this.isCoast;
    }
}
