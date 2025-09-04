package org.vicky.vspe.forge.dimension;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vicky.platform.PlatformItem;
import org.vicky.platform.PlatformPlayer;
import org.vicky.platform.world.PlatformLocation;
import org.vicky.vspe.forge.advancements.ForgeAdvancement;
import org.vicky.vspe.forge.forgeplatform.ForgeDimensionManager;
import org.vicky.vspe.forge.forgeplatform.useables.Descriptored;
import org.vicky.vspe.platform.defaults.SimpleWorldType;
import org.vicky.vspe.platform.systems.dimension.DimensionDescriptor;
import org.vicky.vspe.platform.systems.dimension.Exceptions.NoGeneratorException;
import org.vicky.vspe.platform.systems.dimension.Exceptions.WorldNotExistsException;
import org.vicky.vspe.platform.utilities.Manager.ManagerNotFoundException;
import org.vicky.vspe.systems.dimension.DimensionSpawnStrategy;
import org.vicky.vspe.systems.dimension.PortalContext;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.List;

import static org.vicky.vspe.forge.forgeplatform.AwsomeForgeHacks.fromDescriptor;
import static org.vicky.vspe.forge.forgeplatform.ForgeDimensionManager.cleanNamespace;

public class ForgeDescriptorBasedDimension extends ForgeBaseDimension implements Descriptored {
    private final DimensionDescriptor descriptor;

    public ForgeDescriptorBasedDimension(DimensionDescriptor descriptor, String seed) throws WorldNotExistsException, NoGeneratorException, ManagerNotFoundException {
        super(
                descriptor.name(),
                cleanNamespace(descriptor.name()),
                descriptor.dimensionTypes(),
                seed,
                new SimpleWorldType("NORMAL"),
                descriptor.shouldGenerateStructures(),
                ForgeDimensionManager.GENERATORS.get(cleanNamespace(descriptor.name()))
        );
        this.descriptor = descriptor.clone();
    }

    @Override
    public DimensionType getDimensionType() {
        return fromDescriptor(descriptor);
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
    public void dimensionAdvancementGainProcedures(PlatformPlayer player) {

    }

    @Override
    protected boolean dimensionJoinCondition(ServerPlayer player) {
        return false;
    }

    @Override
    public void applyMechanics(PlatformPlayer var1) {

    }

    @Override
    public void disableMechanics(PlatformPlayer var1) {

    }

    @Override
    public void applyJoinMechanics(PlatformPlayer var1) {

    }

    @Override
    protected void dimensionAdvancementGainProcedures(ServerPlayer player) {

    }

    @Override
    public void applyMechanics(ServerPlayer var1) {

    }

    @Override
    public void disableMechanics(ServerPlayer var1) {

    }

    @Override
    public void applyJoinMechanics(ServerPlayer var1) {

    }

    @Override
    public ForgeAdvancement getDimensionJoinAdvancement() {
        return null;
    }

    @Override
    public boolean dimensionJoinCondition(PlatformPlayer player) {
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
    public @NotNull DimensionSpawnStrategy<BlockState, Level> getStrategy() {
        return (DimensionSpawnStrategy<BlockState, Level>) descriptor.dimensionStrategy();
    }

    @Override
    public @Nullable PortalContext<BlockState, Level> createPortalContext(PlatformPlayer platformPlayer) {
        return (PortalContext<BlockState, Level>) descriptor.portalContext().apply(platformPlayer);
    }

    @Override
    public @NotNull DimensionDescriptor getDescriptor() {
        return descriptor;
    }
}
