package org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions;

import org.vicky.platform.utils.ResourceLocation;
import org.vicky.platform.world.PlatformMaterial;

public class TerraMaterial implements PlatformMaterial {

    private final String name;

    public TerraMaterial(String name) {
        this.name = name;
    }

    @Override
    public boolean isSolid() {
        return true;
    }

    @Override
    public boolean isAir() {
        return false;
    }

    @Override
    public ResourceLocation getResourceLocation() {
        return ResourceLocation.from(this.name);
    }
}
