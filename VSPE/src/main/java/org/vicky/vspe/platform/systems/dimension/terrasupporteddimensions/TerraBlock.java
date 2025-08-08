package org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions;

import org.vicky.platform.utils.Vec3;
import org.vicky.platform.world.PlatformBlock;
import org.vicky.platform.world.PlatformBlockState;
import org.vicky.platform.world.PlatformLocation;
import org.vicky.platform.world.PlatformMaterial;

public class TerraBlock implements PlatformBlock<String> {

    private final TerraBlockState type;
    private final PlatformLocation location;

    public TerraBlock(TerraBlockState type, PlatformLocation location) {
        this.type = type;
        this.location = location;
    }

    @Override
    public boolean isSolid() {
        // If TerraBlockState can answer solidity consult it; default to true
        return type.getMaterial().isSolid();
    }

    @Override
    public PlatformMaterial getMaterial() {
        return this.type.getMaterial();
    }

    @Override
    public PlatformLocation getLocation() {
        return location;
    }

    @Override
    public PlatformBlockState<String> getBlockState() {
        return type;
    }

    @Override
    public void setBlockState(PlatformBlockState<String> platformBlockState) {
        this.location.getWorld().setPlatformBlockState(new Vec3(location.x, location.y, location.z), platformBlockState);
    }
}
