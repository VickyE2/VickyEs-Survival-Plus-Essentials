package org.vicky.vspe.platform.systems.dimension;

import org.jetbrains.annotations.NotNull;
import org.vicky.platform.PlatformPlayer;
import org.vicky.platform.world.PlatformBlockState;
import org.vicky.utilities.Identifiable;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.BiomeResolver;
import org.vicky.vspe.systems.dimension.DimensionSpawnStrategy;
import org.vicky.vspe.systems.dimension.PortalContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @param name
 * @param description
 * @param shouldGenerateStructures
 * @param dimensionTypes
 * @param identifier
 * @param resolver
 * @param oceanLevel
 * @param water
 * @param worldTime
 * @param hasSkyLight
 * @param hasCeiling
 * @param ambientAlways
 * @param canUseAnchor
 * @param canSleep
 * @param natural
 * @param ultraWarm
 * @param ambientLight
 * @param worldScale
 * @param monsterLight
 * @param monsterLightThreshold
 * @param logicalHeight
 * @param minimumY                 when added to maximum y and 1 it MUST be a multiple of 16.
 * @param maximumY                 when added to minimum y and 1 it MUST be a multiple of 16.
 * @param portalContext
 * @param dimensionStrategy
 * @param worldTimeCurve
 */
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
) implements Identifiable {

    public DimensionDescriptor {
        if (minimumY > maximumY) {
            throw new IllegalArgumentException("minimumY cannot be greater than maximumY");
        }

        int height = maximumY - minimumY + 1;
        if (height % 16 != 0) {
            throw new IllegalArgumentException("Height (" + height + ") must be a multiple of 16");
        }

        if (logicalHeight > height) {
            throw new IllegalArgumentException("logicalHeight cannot exceed total height");
        }
    }

    private static int roundDownToMultipleOf16(int v) {
        return (v / 16) * 16;
    }

    @Override
    public int logicalHeight() {
        return roundDownToMultipleOf16(logicalHeight);
    }

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

    @Override
    public String getIdentifier() {
        return identifier;
    }
}
