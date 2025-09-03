package org.vicky.vspe.forge.forgeplatform;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.List;

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
}
