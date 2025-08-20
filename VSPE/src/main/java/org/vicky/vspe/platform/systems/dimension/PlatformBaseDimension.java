package org.vicky.vspe.platform.systems.dimension;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vicky.platform.PlatformItem;
import org.vicky.platform.PlatformPlayer;
import org.vicky.platform.PlatformPlugin;
import org.vicky.platform.world.PlatformLocation;
import org.vicky.platform.world.PlatformWorld;
import org.vicky.utilities.Identifiable;
import org.vicky.vspe.platform.PlatformEnvironment;
import org.vicky.vspe.platform.PlatformWorldType;
import org.vicky.vspe.platform.features.advancement.PlatformAdvancement;
import org.vicky.vspe.platform.systems.dimension.Events.PlatformDimensionWarpEvent;
import org.vicky.vspe.platform.systems.dimension.Exceptions.NoGeneratorException;
import org.vicky.vspe.platform.systems.dimension.Exceptions.WorldNotExistsException;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.BaseGenerator;
import org.vicky.vspe.systems.dimension.DimensionSpawnStrategy;
import org.vicky.vspe.systems.dimension.PlatformDimensionTickHandler;
import org.vicky.vspe.systems.dimension.PortalContext;

import java.util.List;

public interface PlatformBaseDimension<T, N> extends Identifiable {
    String getName();
    List<DimensionType> getDimensionTypes();
    PlatformEnvironment getEnvironmentType();
    String getSeed();
    PlatformWorldType getWorldType();
    boolean generatesStructures();
    BaseGenerator getGenerator();
    String getDescription();
    boolean dimensionExists();
    @Nullable PlatformLocation getGlobalSpawnLocation();
    PlatformDimensionTickHandler getTickHandler();

    List<PlatformItem> dimensionAdvancementGainItems();
    void dimensionAdvancementGainProcedures(PlatformPlayer player);

    PlatformWorld<T, N> checkWorld() throws WorldNotExistsException, NoGeneratorException;

    PlatformWorld<T, N> createWorld(String name) throws NoGeneratorException;

    PlatformDimensionWarpEvent createWarpEvent(PlatformPlayer player);

    @Nullable PlatformWorld<T, N> getWorld();

    boolean isPlayerInDimension(PlatformPlayer player);

    @NotNull DimensionSpawnStrategy<T, N> getStrategy();
    @Nullable PortalContext<T, N> createPortalContext(PlatformPlayer player);

    /**
     * This should fire an even that can be cancelled and implements {@link PlatformDimensionWarpEvent}
     *
     * @param player The entity trying to warp
     * @return If the teleport was successful or not
     */
    default boolean takePlayerToDimension(PlatformPlayer player) {
        PlatformDimensionWarpEvent event = PlatformPlugin.get().getEventFactory().firePlatformEvent(createWarpEvent(player));

        if (event.eventIsCancelled()) {
            player.sendMessage(Component.text("You cannot enter this dimension right now.")
                    .color(TextColor.fromHexString("#FF0000")));
            return false;
        }

        if (dimensionExists()) {
            var loc = getStrategy().resolveSpawn(player, this, createPortalContext(player));
            if (loc == null) {
                player.sendMessage(Component.text("[err: NSS] There was an issue trying to get you to that world").color(TextColor.fromHexString("#440000")).append(Component.text("[err: NSS]").decorate(TextDecoration.ITALIC, TextDecoration.BOLD)));
                return false;
            }
            else {
                player.teleport(loc);
                return true;
            }
        } else {
            player.sendMessage(Component.text("There was an issue trying to get you to that world").color(TextColor.fromHexString("#440000")).append(Component.text("[err: WNX]").decorate(TextDecoration.ITALIC, TextDecoration.BOLD)));
            return false;
        }
    }

    void removePlayerFromDimension(PlatformPlayer var1);

    void applyMechanics(PlatformPlayer var1);

    void disableMechanics(PlatformPlayer var1);

    void applyJoinMechanics(PlatformPlayer var1);

    void disableDimension();
    void enableDimension();

    PlatformItem getDimensionIcon(int position);
    // public PlatformItem getDimensionIconForPlayer(AdvanceablePlayer player);
    PlatformAdvancement getDimensionJoinAdvancement();

    void setTickHandler(PlatformDimensionTickHandler handler);
    void tick();

    boolean dimensionJoinCondition(PlatformPlayer player);

    default @Nullable PlatformLocation locationAt(double x, double y, double z) {
        return new PlatformLocation(getWorld(), x, y, z);
    }

    default @Nullable Double findGroundYAt(int x, int z) {
        return (double) getWorld().getHighestBlockYAt(x, z);
    }
    boolean isSafeSpawnLocation(@Nullable PlatformLocation location);
}
