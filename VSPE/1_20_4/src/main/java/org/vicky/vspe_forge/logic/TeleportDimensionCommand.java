package org.vicky.vspe_forge.logic;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class TeleportDimensionCommand {

    // Suggestion provider that lists all loaded dimension ids
    private static final SuggestionProvider<CommandSourceStack> DIM_SUGGEST = (context, builder) -> {
        MinecraftServer server = context.getSource().getServer();
        // Suggest all currently loaded levels (server-side)
        try {
            for (ServerLevel lvl : server.getAllLevels()) {
                ResourceKey<Level> key = lvl.dimension();
                builder.suggest(key.location().toString()); // e.g. "minecraft:the_nether" or "vspe:crymorra"
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
                .then(Commands.argument("dimension", ResourceLocationArgument.id())
                        .suggests(DIM_SUGGEST)
                        .executes(TeleportDimensionCommand::executeTeleport)
                );

        event.getDispatcher().register(cmd);
    }

    private static int executeTeleport(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();

        // sender must be a player
        ServerPlayer player;
        try {
            player = src.getPlayerOrException();
        } catch (Exception ex) {
            src.sendFailure(Component.literal("Only players can use this command."));
            return 0;
        }

        // get the resource location argument (no manual parsing needed)
        ResourceLocation rl;
        try {
            rl = ResourceLocationArgument.getId(ctx, "dimension");
        } catch (Exception ex) {
            src.sendFailure(Component.literal("Invalid dimension id."));
            return 0;
        }

        ResourceKey<Level> targetKey = ResourceKey.create(Registries.DIMENSION, rl);
        MinecraftServer server = src.getServer();
        ServerLevel targetLevel = server.getLevel(targetKey);
        if (targetLevel == null) {
            src.sendFailure(Component.literal("Dimension not found / not loaded: " + rl));
            return 0;
        }

        // Determine a safe target position: use the level spawn X/Z and find height via heightmap.
        BlockPos sharedSpawn = targetLevel.getSharedSpawnPos();
        int safeY = targetLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sharedSpawn.getX(), sharedSpawn.getZ());
        double tx = sharedSpawn.getX() + 0.5;
        double ty = safeY + 0.1; // small offset above ground
        double tz = sharedSpawn.getZ() + 0.5;

        try {
            // preserve player rotation
            float yaw = player.getYRot();
            float pitch = player.getXRot();

            // teleportTo(ServerLevel, x,y,z, yaw, pitch)
            player.teleportTo(targetLevel, tx, ty, tz, yaw, pitch);

            src.sendSuccess(() -> Component.literal("Teleported to " + rl), true);
            return 1;
        } catch (Exception ex) {
            ex.printStackTrace();
            src.sendFailure(Component.literal("Teleport failed: " + ex.getMessage()));
            return 0;
        }
    }
}
