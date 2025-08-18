package org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.dimensions.ChromaticUnderWater;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vicky.platform.PlatformItem;
import org.vicky.platform.PlatformPlayer;
import org.vicky.platform.world.PlatformLocation;
import org.vicky.platform.world.PlatformWorld;
import org.vicky.vspe.platform.PlatformEnvironment;
import org.vicky.vspe.platform.PlatformWorldType;
import org.vicky.vspe.platform.features.advancement.PlatformAdvancement;
import org.vicky.vspe.platform.systems.dimension.DimensionType;
import org.vicky.vspe.platform.systems.dimension.Events.PlatformDimensionWarpEvent;
import org.vicky.vspe.platform.systems.dimension.Exceptions.NoGeneratorException;
import org.vicky.vspe.platform.systems.dimension.Exceptions.WorldNotExistsException;
import org.vicky.vspe.platform.systems.dimension.PlatformBaseDimension;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.BaseGenerator;
import org.vicky.vspe.systems.dimension.DimensionSpawnStrategy;
import org.vicky.vspe.systems.dimension.PlatformDimensionTickHandler;
import org.vicky.vspe.systems.dimension.PortalContext;

import java.util.List;

public class ChromaticUnderwaterDimension<T> implements PlatformBaseDimension<String, T> {

    @Override
    public String getName() {
        return "";
    }

    @Override
    public List<DimensionType> getDimensionTypes() {
        return List.of();
    }

    @Override
    public PlatformEnvironment getEnvironmentType() {
        return null;
    }

    @Override
    public String getSeed() {
        return "";
    }

    @Override
    public PlatformWorldType getWorldType() {
        return null;
    }

    @Override
    public boolean generatesStructures() {
        return false;
    }

    @Override
    public BaseGenerator getGenerator() {
        return null;
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public boolean dimensionExists() {
        return false;
    }

    @Override
    public @Nullable PlatformLocation getGlobalSpawnLocation() {
        return null;
    }

    @Override
    public PlatformDimensionTickHandler getTickHandler() {
        return null;
    }

    @Override
    public List<PlatformItem> dimensionAdvancementGainItems() {
        return List.of();
    }

    @Override
    public void dimensionAdvancementGainProcedures(org.vicky.platform.PlatformPlayer player) {

    }

    @Override
    public PlatformWorld<String, T> checkWorld() throws WorldNotExistsException, NoGeneratorException {
        return null;
    }

    @Override
    public PlatformWorld<String, T> createWorld(String name) throws NoGeneratorException {
        return null;
    }

    @Override
    public PlatformDimensionWarpEvent createWarpEvent(org.vicky.platform.PlatformPlayer player) {
        return null;
    }

    @Override
    public @Nullable PlatformWorld<String, T> getWorld() {
        return null;
    }

    @Override
    public boolean isPlayerInDimension(org.vicky.platform.PlatformPlayer player) {
        return false;
    }

    @Override
    public @NotNull DimensionSpawnStrategy<String, T> getStrategy() {
        return null;
    }

    @Override
    public @Nullable PortalContext<String, T> createPortalContext(PlatformPlayer player) {
        return null;
    }

    @Override
    public void removePlayerFromDimension(org.vicky.platform.PlatformPlayer var1) {

    }

    @Override
    public void applyMechanics(org.vicky.platform.PlatformPlayer var1) {

    }

    @Override
    public void disableMechanics(org.vicky.platform.PlatformPlayer var1) {

    }

    @Override
    public void applyJoinMechanics(org.vicky.platform.PlatformPlayer var1) {

    }

    @Override
    public void disableDimension() {

    }

    @Override
    public void enableDimension() {

    }

    @Override
    public PlatformItem getDimensionIcon(int position) {
        return null;
    }

    @Override
    public PlatformAdvancement getDimensionJoinAdvancement() {
        return null;
    }

    @Override
    public void setTickHandler(PlatformDimensionTickHandler handler) {

    }

    @Override
    public void tick() {

    }

    @Override
    public boolean dimensionJoinCondition(org.vicky.platform.PlatformPlayer player) {
        return false;
    }

    @Override
    public @Nullable PlatformLocation locationAt(double d, double d1, double d2) {
        return null;
    }

    @Override
    public @Nullable Double findGroundYAt(int i, int i1) {
        return 0.0;
    }

    @Override
    public boolean isSafeSpawnLocation(@Nullable PlatformLocation location) {
        return false;
    }

    @Override
    public String getIdentifier() {
        return "";
    }
}
