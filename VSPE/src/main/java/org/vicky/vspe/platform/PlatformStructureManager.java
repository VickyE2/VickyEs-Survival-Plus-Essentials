package org.vicky.vspe.platform;

import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vicky.platform.utils.ResourceLocation;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.NbtStructure;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.PlatformStructure;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.StructureRule;

import java.util.Map;

public interface PlatformStructureManager<T> {
    @NotNull Map<ResourceLocation, Pair<PlatformStructure<T>, StructureRule>> getStructures();

    @Nullable NbtStructure<T> getNBTStructure(ResourceLocation id);

    @Nullable PlatformStructure<T> getStructure(ResourceLocation id);

    void addStructure(@NotNull ResourceLocation id, @NotNull PlatformStructure<T> structure, @NotNull StructureRule rule);
}