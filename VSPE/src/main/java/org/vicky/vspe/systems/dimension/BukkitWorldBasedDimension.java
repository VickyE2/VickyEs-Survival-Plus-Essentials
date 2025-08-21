package org.vicky.vspe.systems.dimension;

import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vicky.bukkitplatform.useables.BukkitWorldAdapter;
import org.vicky.platform.PlatformItem;
import org.vicky.platform.PlatformPlayer;
import org.vicky.platform.world.PlatformLocation;
import org.vicky.vspe.platform.defaults.SimpleWorldType;
import org.vicky.vspe.platform.systems.dimension.DimensionType;
import org.vicky.vspe.platform.systems.dimension.Exceptions.NoGeneratorException;
import org.vicky.vspe.platform.systems.dimension.Exceptions.WorldNotExistsException;
import org.vicky.vspe.platform.utilities.Manager.ManagerNotFoundException;

import java.util.List;

public class BukkitWorldBasedDimension extends BukkitBaseDimension {
    private final World world;

    public BukkitWorldBasedDimension(World world) throws WorldNotExistsException, NoGeneratorException, ManagerNotFoundException {
        super(
                world.getName(),
                world.getName(),
                List.of(DimensionType.NORMAL_WORLD),
                world.getEnvironment(),
                world.getSeed() + "",
                new SimpleWorldType("NORMAL"),
                world.canGenerateStructures(),
                null
        );
        this.world = world;
    }

    @Override
    public BukkitWorldAdapter createWorld() {
        return new BukkitWorldAdapter(world);
    }

    @Override
    protected void dimensionAdvancementGainProcedures(Player player) {

    }

    @Override
    public void applyMechanics(Player var1) {

    }

    @Override
    public void disableMechanics(Player var1) {

    }

    @Override
    public void applyJoinMechanics(Player var1) {

    }

    @Override
    protected boolean dimensionJoinCondition(Player player) {
        return true;
    }

    @Override
    public @Nullable PlatformLocation getGlobalSpawnLocation() {
        return null;
    }

    @Override
    public List<PlatformItem> dimensionAdvancementGainItems() {
        return List.of();
    }

    @Override
    public @NotNull DimensionSpawnStrategy<BlockData, World> getStrategy() {
        return new PortalLinkedStrategy<>();
    }

    @Override
    public @Nullable PortalContext<BlockData, World> createPortalContext(PlatformPlayer platformPlayer) {
        return null;
    }
}
