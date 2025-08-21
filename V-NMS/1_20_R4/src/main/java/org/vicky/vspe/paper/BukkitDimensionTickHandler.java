package org.vicky.vspe.paper;

import org.jetbrains.annotations.NotNull;
import org.vicky.bukkitplatform.useables.BukkitPlatformPlayer;
import org.vicky.bukkitplatform.useables.BukkitWorldAdapter;
import org.vicky.platform.PlatformPlayer;
import org.vicky.platform.world.PlatformWorld;
import org.vicky.vspe.systems.dimension.PlatformDimensionTickHandler;

import java.util.ArrayList;
import java.util.List;

public abstract class BukkitDimensionTickHandler implements PlatformDimensionTickHandler {

    @Override
    public final void tick(@NotNull List<? extends PlatformPlayer> list,
                           @NotNull PlatformWorld<?, ?> platformWorld) {
        if (platformWorld instanceof BukkitWorldAdapter worldAdapter) {
            // Convert to Bukkit players
            List<BukkitPlatformPlayer> bukkitPlayers = new ArrayList<>();
            for (PlatformPlayer player : list) {
                if (player instanceof BukkitPlatformPlayer bukkitPlayer) {
                    bukkitPlayers.add(bukkitPlayer);
                } else {
                    throw new IllegalArgumentException(
                            "Got non-BukkitPlatformPlayer in tick list: " + player.getClass()
                    );
                }
            }

            // Call your platform-specific tick logic
            performTicking(bukkitPlayers, worldAdapter);
        }
    }

    public abstract void performTicking(List<BukkitPlatformPlayer> players,
                                        BukkitWorldAdapter world);
}

