package org.vicky.vspe_forge.dimension;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vicky.platform.PlatformItem;
import org.vicky.platform.PlatformPlayer;
import org.vicky.platform.world.PlatformLocation;
import org.vicky.vspe.platform.defaults.SimpleWorldType;
import org.vicky.vspe.platform.systems.dimension.DimensionDescriptor;
import org.vicky.vspe.platform.systems.dimension.Exceptions.NoGeneratorException;
import org.vicky.vspe.platform.systems.dimension.Exceptions.WorldNotExistsException;
import org.vicky.vspe.platform.utilities.Manager.ManagerNotFoundException;
import org.vicky.vspe.systems.dimension.DimensionSpawnStrategy;
import org.vicky.vspe.systems.dimension.PortalContext;
import org.vicky.vspe_forge.advancements.ForgeAdvancement;
import org.vicky.vspe_forge.forgeplatform.ForgeDimensionManager;
import org.vicky.vspe_forge.forgeplatform.useables.Descriptored;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.List;

import static org.vicky.vspe_forge.forgeplatform.AwsomeForgeHacks.fromDescriptor;
import static org.vicky.vspe_forge.forgeplatform.ForgeDimensionManager.cleanNamespace;

public class ForgeDescriptorBasedDimension extends ForgeBaseDimension implements Descriptored {
    private DimensionDescriptor descriptor;

    public ForgeDescriptorBasedDimension(DimensionDescriptor inputtedDescriptor, String seed) throws WorldNotExistsException, NoGeneratorException, ManagerNotFoundException {
        super(
                inputtedDescriptor.name(),
                cleanNamespace(inputtedDescriptor.name()),
                inputtedDescriptor.dimensionTypes(),
                seed,
                new SimpleWorldType("NORMAL"),
                inputtedDescriptor.shouldGenerateStructures(),
                ForgeDimensionManager.GENERATORS.get(cleanNamespace(inputtedDescriptor.name())),
                inputtedDescriptor
        );
    }

    @Override
    public @Nullable Runnable toRuns() {
        return () -> {
            Object p = this.passable;
            if (p == null) {
                throw new IllegalStateException("Expected a DimensionDescriptor in passable, but passable is null");
            }
            if (!(p instanceof DimensionDescriptor dd)) {
                throw new IllegalStateException("Expected passable to be DimensionDescriptor but got: " + p.getClass());
            }
            // defensive copy
            this.descriptor = dd.copy();
        };
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
