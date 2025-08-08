package org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils;

public enum Colorable {
    FOG,
    WATER,
    WATER_FOG,
    SKY,
    GRASS,
    FOLIAGE;

    @Override
    public String toString() {
        return this.name().toLowerCase().replace("_", "-");
    }
}
