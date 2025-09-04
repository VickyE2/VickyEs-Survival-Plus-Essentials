package org.vicky.vspe.platform.systems.dimension;

import org.vicky.platform.PlatformPlayer;
import org.vicky.platform.world.PlatformBlockState;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.BiomeResolver;
import org.vicky.vspe.systems.dimension.DimensionSpawnStrategy;
import org.vicky.vspe.systems.dimension.PortalContext;

import java.util.List;
import java.util.function.Function;

public record DimensionDescriptor(
        String name,
        String description,
        boolean shouldGenerateStructures,
        List<DimensionType> dimensionTypes,
        String identifier,
        BiomeResolver<?> resolver,
        int oceanLevel,
        PlatformBlockState<?> water,
        long worldTime,
        boolean hasSkyLight,
        boolean hasCeiling,
        boolean ambientAlways,
        boolean canUseAnchor,
        boolean canSleep,
        boolean natural,
        boolean ultraWarm,
        float ambientLight,
        float worldScale,
        int monsterLight,
        int monsterLightThreshold,
        int logicalHeight,
        int minimumY,
        int maximumY,
        Function<PlatformPlayer, PortalContext<?, ?>> portalContext,
        DimensionSpawnStrategy<?, ?> dimensionStrategy,
        TimeCurve worldTimeCurve
) implements Cloneable {

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
