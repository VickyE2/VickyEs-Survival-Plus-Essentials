package org.vicky.vspe.forge.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import org.jetbrains.annotations.Nullable;
import org.vicky.vspe.forge.dimension.ForgeBaseDimension;

@Cancelable
public class DimensionWarpEvent extends Event {
    private final ForgeBaseDimension dimension;
    private final ServerPlayer player;

    public DimensionWarpEvent(ServerPlayer handle, ForgeBaseDimension forgeBaseDimension) {
        this.player = handle;
        this.dimension = forgeBaseDimension;
    }

    @Override
    public @Nullable EventPriority getPhase() {
        return EventPriority.HIGH;
    }

    public ForgeBaseDimension getDimension() {
        return dimension;
    }

    public ServerPlayer getPlayer() {
        return player;
    }
}
