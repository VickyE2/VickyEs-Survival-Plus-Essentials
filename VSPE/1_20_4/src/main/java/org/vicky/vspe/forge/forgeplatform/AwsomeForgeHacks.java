package org.vicky.vspe.forge.forgeplatform;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import org.vicky.vspe.forge.forgeplatform.useables.ForgeDescriptorDispensableDimensionEffects;
import org.vicky.vspe.platform.systems.dimension.DimensionDescriptor;

import java.util.List;
import java.util.OptionalLong;

import static org.vicky.vspe.VspeForge.server;

public class AwsomeForgeHacks {
    public static ServerLevel getLevel(String dimensionName) {
        ResourceLocation id = new ResourceLocation(dimensionName);
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
        return server.getLevel(key);
    }

    public static void moveAllPlayersToOverworld(ServerLevel fromLevel) {
        var server = fromLevel.getServer();
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);

        for (ServerPlayer player : List.copyOf(fromLevel.players())) {
            player.teleportTo(overworld,
                    overworld.getSharedSpawnPos().getX() + 0.5,
                    overworld.getSharedSpawnPos().getY(),
                    overworld.getSharedSpawnPos().getZ() + 0.5,
                    player.getYRot(),
                    player.getXRot()
            );
        }
    }

    public static ServerLevel getLevelFromKey(ResourceKey<Level> levelKey) {
        return server.getLevel(levelKey); // returns null if the dimension isn’t loaded
    }

    public static DimensionType fromDescriptor(DimensionDescriptor descriptor) {
        return new DimensionType(
                OptionalLong.empty(),
                descriptor.hasSkyLight(),
                descriptor.hasCeiling(),
                descriptor.ultraWarm(),
                descriptor.natural(),
                descriptor.worldScale(),
                descriptor.canSleep(),
                descriptor.canUseAnchor(),
                descriptor.minimumY(),
                Math.abs(descriptor.minimumY()) + descriptor.maximumY(),
                descriptor.logicalHeight(),
                BlockTags.INFINIBURN_OVERWORLD,
                new ForgeDescriptorDispensableDimensionEffects(descriptor),
                descriptor.ambientLight(),
                new net.minecraft.world.level.dimension.DimensionType.MonsterSettings(
                        false,
                        false,
                        ConstantInt.of(descriptor.monsterLight()),
                        descriptor.monsterLightThreshold()
                )
        ) {
            @Override
            public float timeOfDay(long dayTime) {
                TimeCurve curve = descriptor.worldTimeCurve();
                float cycle = curve.apply(dayTime, descriptor.worldTime());
                float shifted = cycle - 0.25f;
                if (shifted < 0f) shifted += 1f;
                return shifted;
            }
        };
    }
}
