package org.vicky.vspe.platform.systems.dimension;

import org.jetbrains.annotations.NotNull;
import org.vicky.platform.PlatformPlayer;
import org.vicky.platform.world.PlatformBlockState;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.BiomeResolver;
import org.vicky.vspe.systems.dimension.DimensionSpawnStrategy;
import org.vicky.vspe.systems.dimension.PortalContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public record DimensionDescriptor(
        @NotNull String name,
        @NotNull String description,
        boolean shouldGenerateStructures,
        @NotNull List<DimensionType> dimensionTypes,
        @NotNull String identifier,
        @NotNull BiomeResolver<?> resolver,
        int oceanLevel,
        @NotNull PlatformBlockState<?> water,
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
        @NotNull Function<PlatformPlayer, PortalContext<?, ?>> portalContext,
        @NotNull DimensionSpawnStrategy<?, ?> dimensionStrategy,
        @NotNull TimeCurve worldTimeCurve
) {

    /**
     * Shallow copy; defensive-copy for mutable collections.
     */
    public DimensionDescriptor copy() {
        List<DimensionType> dimTypesCopy = this.dimensionTypes == null
                ? null
                : new ArrayList<>(this.dimensionTypes);

        return new DimensionDescriptor(
                name,
                description,
                shouldGenerateStructures,
                dimTypesCopy,
                identifier,
                resolver,
                oceanLevel,
                water,
                worldTime,
                hasSkyLight,
                hasCeiling,
                ambientAlways,
                canUseAnchor,
                canSleep,
                natural,
                ultraWarm,
                ambientLight,
                worldScale,
                monsterLight,
                monsterLightThreshold,
                logicalHeight,
                minimumY,
                maximumY,
                portalContext,
                dimensionStrategy,
                worldTimeCurve
        );
    }
}
