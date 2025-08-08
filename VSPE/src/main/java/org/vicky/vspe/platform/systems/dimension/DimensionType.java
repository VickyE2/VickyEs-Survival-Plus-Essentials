package org.vicky.vspe.platform.systems.dimension;

import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.PlatformDimension;

import java.util.ArrayList;
import java.util.List;

public enum DimensionType {

    DISTANT_WORLD(new ArrayList<>(), "8A2BE2"),
    CLOSE_WORLD(new ArrayList<>(), "FF4500"),
    HOLLOW_WORLD(new ArrayList<>(), "808080"),
    NORMAL_WORLD(new ArrayList<>(), "00FF00"),
    AQUATIC_WORLD(new ArrayList<>(), "27a7ff"),
    FROZEN_WORLD(new ArrayList<>(), "ADD8E6"),
    NETHER_WORLD(new ArrayList<>(), "8B0000"),
    DARK_WORLD(new ArrayList<>(), "2F4F4F"),
    BRIGHT_WORLD(new ArrayList<>(), "FFFF00"),
    BARREN_WORLD(new ArrayList<>(), "DEB887"),
    ARBOREAL_WORLD(new ArrayList<>(), "228B22"),
    ELEMENTAL_WORLD(new ArrayList<>(), "FF8C00"),
    DREAM_WORLD(new ArrayList<>(), "FFC0CB"),
    ABSTRACT_WORLD(new ArrayList<>(), "DDA0DD"),
    ALIEN_WORLD(new ArrayList<>(), "00FFFF"),
    FUTURISTIC_WORLD(new ArrayList<>(), "86009d"),
    ANCIENT_WORLD(new ArrayList<>(), "aadd88"),
    AETHER_WORLD(new ArrayList<>(), "ddddee");

    private final List<PlatformDimension<?, ?>> dimensions;
    private final String hexCode;

    DimensionType(List<PlatformDimension<?, ?>> dimensions, String hexCode) {
        this.dimensions = dimensions;
        this.hexCode = hexCode;
    }

    public void addDimension(PlatformDimension<?, ?> dimension) {
        this.dimensions.add(dimension);
    }

    public String getHexCode() {
        return "#" + hexCode;
    }

    public void removeDimension(PlatformDimension<?, ?> dimension) {
        this.dimensions.remove(dimension);
    }

    public boolean hasDimension(PlatformDimension<?, ?> dimension) {
        return this.dimensions.stream().anyMatch(dimension1 -> dimension1.equals(dimension));
    }
}
