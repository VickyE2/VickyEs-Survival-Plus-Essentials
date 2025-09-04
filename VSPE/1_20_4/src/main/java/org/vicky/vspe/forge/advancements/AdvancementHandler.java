package org.vicky.vspe.forge.advancements;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles dynamic advancement registration/unregistration.
 */
@Mod.EventBusSubscriber
public class AdvancementHandler {
    public static final Map<ResourceLocation, AdvancementHolder> REGISTERED = new HashMap<>();
    public static final Map<ResourceLocation, AdvancementHolder> UNREGISTERED = new HashMap<>();

    /**
     * Build and register a new advancement dynamically.
     */
    public static AdvancementHolder register(ResourceLocation id, Advancement.Builder builder) {
        AdvancementHolder adv = builder.build(id);
        UNREGISTERED.remove(id);
        REGISTERED.put(id, adv);
        return adv;
    }

    /**
     * Build and register a new advancement dynamically.
     */
    public static AdvancementHolder register(AdvancementHolder holder) {
        UNREGISTERED.remove(holder.id());
        REGISTERED.put(holder.id(), holder);
        return holder;
    }

    /**
     * Build and register a new advancement dynamically.
     */
    public static AdvancementHolder unregister(ResourceLocation id) {
        var holder = REGISTERED.remove(id);
        UNREGISTERED.put(id, holder);
        return holder;
    }

    /**
     * Called whenever datapacks are (re)loaded — push our advancements to players.
     */
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        syncToAll(event.getPlayerList().getPlayers());
    }

    /**
     * Called when a player joins (to give them any custom advancements too).
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncToPlayer(player);
        }
    }

    private static void syncToAll(List<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            syncToPlayer(player);
        }
    }

    private static void syncToPlayer(@NotNull ServerPlayer player) {
        var manager = player.getServer().getAdvancements();
        for (AdvancementHolder adv : REGISTERED.values()) {
            // Ensure the advancement exists in the server's advancement data
            manager.getAllAdvancements().add(adv);

            // Optionally auto-grant for testing
            var progress = player.getAdvancements().getOrStartProgress(adv);
            for (String criterion : progress.getRemainingCriteria()) {
                player.getAdvancements().award(adv, criterion);
            }
        }
    }
}
