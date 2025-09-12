package org.vicky.vspe_forge.logic;


import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.CompletableFuture;

/**
 * Command: /tpdim <dimension>
 *
 * Example usage:
 *   /tpdim minecraft:the_nether
 *   /tpdim yourmod:my_dimension
 *
 * Permission: requires op (permission level 2).
 */
@Mod.EventBusSubscriber
public class TeleportDimensionCommand {

    // Suggestion provider that lists all loaded dimension ids
    private static final SuggestionProvider<CommandSourceStack> DIM_SUGGEST = (context, builder) -> {
        MinecraftServer server = context.getSource().getServer();
        // Server API: either getAllLevels() or iterate known ResourceKeys.
        // We'll use server.getAllLevels() if available, otherwise iterate registry keys.
        try {
            for (ServerLevel lvl : server.getAllLevels()) {
                ResourceKey<Level> key = lvl.dimension();
                builder.suggest(key.location().toString());
            }
        } catch (NoSuchMethodError e) {
            // fallback: iterate dimension registry keys from server registryAccess
            server.registryAccess().registryOrThrow(Registries.DIMENSION)
                    .keySet()
                    .forEach(loc -> builder.suggest(loc.toString()));
        }
        return builder.buildFuture();
    };

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> cmd = Commands.literal("tpdim")
                .requires(src -> src.hasPermission(2)) // op level 2 required
                .then(Commands.argument("dimension", StringArgumentType.word())
                        .suggests(DIM_SUGGEST)
                        .executes(TeleportDimensionCommand::executeTeleport)
                );

        event.getDispatcher().register(cmd);
    }

    private static int executeTeleport(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        MinecraftServer server = src.getServer();

        // sender must be a player
        ServerPlayer player;
        try {
            player = src.getPlayerOrException();
        } catch (Exception ex) {
            src.sendFailure(Component.literal("Only players can use this command."));
            return 0;
        }

        String dimId = StringArgumentType.getString(ctx, "dimension");
        ResourceLocation rl;
        try {
            rl = new ResourceLocation(dimId);
        } catch (Exception ex) {
            src.sendFailure(Component.literal("Invalid dimension id: " + dimId));
            return 0;
        }

        ResourceKey<Level> targetKey = ResourceKey.create(Registries.DIMENSION, rl);
        ServerLevel targetLevel = server.getLevel(targetKey);
        if (targetLevel == null) {
            src.sendFailure(Component.literal("Dimension not found / not loaded: " + dimId));
            return 0;
        }

        // Determine a safe target position: use the level spawn X/Z and find height via heightmap.
        BlockPos sharedSpawn = targetLevel.getSharedSpawnPos();
        int safeY = targetLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sharedSpawn.getX(), sharedSpawn.getZ());
        double tx = sharedSpawn.getX() + 0.5;
        double ty = safeY + 0.1; // small offset above ground
        double tz = sharedSpawn.getZ() + 0.5;

        // If the target is the same dimension, just teleport locally; otherwise use the cross-dimension teleport.
        try {
            // preserve player rotation
            float yaw = player.getYRot();
            float pitch = player.getXRot();

            // ServerPlayer.teleportTo(ServerLevel, x,y,z, yaw, pitch)
            player.teleportTo(targetLevel, tx, ty, tz, yaw, pitch);

            src.sendSuccess(() -> Component.literal("Teleported to " + dimId), true);
            return 1;
        } catch (Exception ex) {
            ex.printStackTrace();
            src.sendFailure(Component.literal("Teleport failed: " + ex.getMessage()));
            return 0;
        }
    }
}