package org.vicky.vspe_forge.forgeplatform;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.vicky.vspe.platform.systems.dimension.DimensionDescriptor;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.RandomSource;
import org.vicky.vspe_forge.forgeplatform.useables.VSPEDimensionEffects;

import java.util.List;
import java.util.OptionalLong;

import static org.vicky.vspe_forge.VspeForge.server;

public class AwsomeForgeHacks {
    public static ServerLevel getLevel(String dimensionName) {
        ResourceLocation id = new ResourceLocation(dimensionName);
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
        return server.getLevel(key);
    }

    public static RandomSource fromForge(net.minecraft.util.RandomSource forge) {
        return new RandomSource() {
            @Override
            public int nextInt(int i) {
                return forge.nextInt(i);
            }

            @Override
            public int nextInt(int i, int i1) {
                return forge.nextInt(i, i1);
            }

            @Override
            public double nextDouble() {
                return forge.nextDouble();
            }

            @Override
            public float nextFloat() {
                return forge.nextFloat();
            }

            @Override
            public boolean nextBoolean() {
                return forge.nextBoolean();
            }

            @Override
            public long nextLong() {
                return forge.nextLong();
            }

            @Override
            public long getSeed() {
                return forge.nextLong();
            }

            @Override
            public void setSeed(long l) {
                forge.setSeed(l);
            }

            @Override
            public @NotNull RandomSource fork(long l) {
                return fromForge(forge.fork());
            }
        };
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
                Math.abs(descriptor.minimumY()) + descriptor.maximumY() + 1,
                descriptor.logicalHeight(),
                BlockTags.INFINIBURN_OVERWORLD,
                VSPEDimensionEffects.registerSpecialEffect(descriptor),
                descriptor.ambientLight(),
                new net.minecraft.world.level.dimension.DimensionType.MonsterSettings(
                        false,
                        false,
                        ConstantInt.of(descriptor.monsterLight()),
                        descriptor.monsterLightThreshold()
                )
        );
    }
}
