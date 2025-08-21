package org.vicky.vspe.paper;

import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;
import org.vicky.bukkitplatform.useables.BukkitWorldAdapter;
import org.vicky.platform.world.PlatformWorld;
import org.vicky.vspe.platform.systems.dimension.DimensionDescriptor;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.*;
import org.vicky.vspe.systems.dimension.VSPEBukkitDimensionManager;

import static org.vicky.vspe.systems.dimension.VSPEBukkitDimensionManager.cleanNamespace;

public class BukkitPlatformDimension implements PlatformDimension<BlockData, BukkitBiome> {
    private final DimensionDescriptor descriptor;
    private final PlatformWorld<BlockData, World> world;

    public BukkitPlatformDimension(DimensionDescriptor descriptor, World nativeWorld) {
        this.descriptor = descriptor;
        this.world = new BukkitWorldAdapter(nativeWorld);
    }

    @Override
    public @NotNull String getId() {
        return descriptor.identifier();
    }

    @Override
    public @NotNull PlatformWorld<BlockData, World> getWorld() {
        return world;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull PlatformChunkGenerator<BlockData, BukkitBiome> getChunkGenerator() {
        return (PlatformChunkGenerator<BlockData, BukkitBiome>) VSPEBukkitDimensionManager.GENERATORS.get(cleanNamespace(descriptor.name()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull BiomeResolver<BukkitBiome> getBiomeResolver() {
        return (BiomeResolver<BukkitBiome>) descriptor.resolver();
    }

    @Override
    public @NotNull StructurePlacer<BlockData> getStructurePlacer() {
        return new WeightedStructurePlacer<>();
    }

    @Override
    public @NotNull RandomSource getRandom() {
        return new SeededRandomSource(world.getNative().getSeed());
    }
}

