package org.vicky.vspe.platform.systems.dimension.globalDimensions.Crymorra;

import org.jetbrains.annotations.NotNull;
import org.vicky.platform.world.PlatformLocation;
import org.vicky.vspe.platform.systems.dimension.DimensionType;
import org.vicky.vspe.platform.systems.dimension.PlatformBaseDimension;

import java.util.List;

public abstract class CrymorraDimensionManager<T, N> implements PlatformBaseDimension<T, N> {
    @Override
    public final String getName() {
        return "Crymorra";
    }

    @Override
    public final @NotNull Double findGroundYAt(int x, int z) {
        return (double) getWorld().getHighestBlockYAt(x, z);
    }

    @Override
    public final String getDescription() {
        return "A fallen dimension of cold war...";
    }

    @Override
    public final boolean generatesStructures() {
        return true;
    }

    @Override
    public final List<DimensionType> getDimensionTypes() {
        return List.of(DimensionType.FROZEN_WORLD, DimensionType.ELEMENTAL_WORLD);
    }

    @Override
    public final String getIdentifier() {
        return "vspe:crymorra"   ;
    }

    @Override
    public final PlatformLocation locationAt(double x, double y, double z) {
        return new PlatformLocation(getWorld(), x, y, z);
    }
}
