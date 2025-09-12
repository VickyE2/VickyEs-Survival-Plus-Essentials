package org.vicky.vspe_forge.dimension;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import org.jetbrains.annotations.Nullable;
import org.vicky.vspe.platform.systems.dimension.DimensionDescriptor;

import java.util.List;
import java.util.concurrent.Executor;

public class DescriptorWorldable extends ServerLevel {
    private final DimensionDescriptor descriptor;

    public DescriptorWorldable(
            DimensionDescriptor descriptor,
            MinecraftServer server,
            Executor executor,
            LevelStorageSource.LevelStorageAccess access,
            ServerLevelData levelData,
            ResourceKey<Level> dimensionKey,
            LevelStem levelStem,
            ChunkProgressListener chunkProgressListener,
            boolean debug,
            long seed,
            List<CustomSpawner> spawners,
            boolean something,
            @Nullable RandomSequences randomSequences
    ) {
        super(server, executor, access, levelData, dimensionKey, levelStem,
                chunkProgressListener, debug, seed, spawners, something, randomSequences);
        this.descriptor = descriptor;
    }

    /**
     * Return a monotonic world tick value that maps the server's raw ticks into
     * vanilla-style 24000-based day chunks but with a per-cycle position driven
     * by your custom TimeCurve and custom day length (descriptor.worldTime()).
     */
    @Override
    public long getDayTime() {
        long rawTicks = super.getDayTime();
        long customDayLength = Math.max(1L, descriptor.worldTime());
        long cycles = rawTicks / customDayLength;
        long ticksIntoCycle = rawTicks % customDayLength;
        float cycleNormalized = descriptor.worldTimeCurve().apply(ticksIntoCycle, customDayLength);

        float shifted = cycleNormalized - 0.25f;
        if (shifted < 0f) shifted += 1f;

        long tickWithinVanillaDay = (long) (shifted * 24000.0F) & 0xFFFF_FFFFL;

        return cycles * 24000L + (tickWithinVanillaDay % 24000L);
    }
}
