package org.vicky.vspe.systems.Dimension.Generator.utils;

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
