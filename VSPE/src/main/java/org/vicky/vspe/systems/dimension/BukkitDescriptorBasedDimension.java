package org.vicky.vspe.systems.dimension;

import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vicky.bukkitplatform.useables.BukkitWorldAdapter;
import org.vicky.platform.PlatformItem;
import org.vicky.platform.PlatformPlayer;
import org.vicky.platform.world.PlatformLocation;
import org.vicky.vspe.platform.defaults.SimpleWorldType;
import org.vicky.vspe.platform.systems.dimension.DimensionDescriptor;
import org.vicky.vspe.platform.systems.dimension.Exceptions.NoGeneratorException;
import org.vicky.vspe.platform.systems.dimension.Exceptions.WorldNotExistsException;
import org.vicky.vspe.platform.utilities.Manager.ManagerNotFoundException;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.List;

import static org.vicky.vspe.systems.dimension.VSPEBukkitDimensionManager.cleanNamespace;

public class BukkitDescriptorBasedDimension extends BukkitBaseDimension {
    private final DimensionDescriptor descriptor;

    public BukkitDescriptorBasedDimension(DimensionDescriptor descriptor, String seed) throws WorldNotExistsException, NoGeneratorException, ManagerNotFoundException {
        super(
                cleanNamespace(descriptor.name()),
                descriptor.name(),
                descriptor.dimensionTypes(),
                World.Environment.NORMAL,
                seed,
                new SimpleWorldType("NORMAL"),
                descriptor.shouldGenerateStructures(),
                null
        );
        this.descriptor = new DimensionDescriptor(
                descriptor.name(),
                descriptor.description(),
                descriptor.shouldGenerateStructures(),
                descriptor.dimensionTypes(),
                descriptor.identifier(),
                descriptor.resolver(),
                descriptor.oceanLevel()
        );
        // BukkitPlatformDimension platformDim = new BukkitPlatformDimension(descriptor, getWorld().getBukkitWorld());
        VSPEBukkitDimensionManager.GENERATORS.get(cleanNamespace(descriptor.name())); //.attachDimension(platformDim);
    }

    public static long stringToSeed(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            // Take first 8 bytes for a 64-bit seed
            return ByteBuffer.wrap(hash).getLong();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public BukkitWorldAdapter createWorld() {
        WorldCreator creator = new WorldCreator(this.getName());
        creator.environment(getEnvironmentType().getNative());
        creator.generateStructures(true);
        creator.seed(stringToSeed(getSeed()));
        creator.generator("VSPE:" + cleanNamespace(getName()).toUpperCase());

        World created = creator.createWorld();
        if (created == null) {
            throw new RuntimeException("Bukkit returned null while creating world " + getName());
        }
        return new BukkitWorldAdapter(created);
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
        return false;
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
        try {
            return new PortalContext<>(
                    platformPlayer.getLocation(),
                    new BukkitWorldBasedDimension((World) platformPlayer.getLocation().getWorld().getNative()),
                    this,
                    platformPlayer,
                    () -> null
            );
        } catch (WorldNotExistsException | NoGeneratorException | ManagerNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public DimensionDescriptor getDescriptor() {
        return descriptor;
    }
}
