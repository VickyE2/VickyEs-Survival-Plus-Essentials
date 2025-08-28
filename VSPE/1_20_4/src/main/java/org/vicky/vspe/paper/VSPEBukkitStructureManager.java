package org.vicky.vspe.paper;

import kotlin.Pair;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vicky.platform.utils.ResourceLocation;
import org.vicky.vspe.platform.PlatformStructureManager;
import org.vicky.vspe.platform.systems.dimension.globalDimensions.StructureResolvers;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.NBTBasedStructure;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.NbtStructure;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.PlatformStructure;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.StructureRule;

import java.util.HashMap;
import java.util.Map;

public class VSPEBukkitStructureManager implements PlatformStructureManager<BlockData> {

    private static final Map<ResourceLocation, Pair<PlatformStructure<BlockData>, StructureRule>> structures
            = new HashMap<>();

    static {
        structures.putAll(initStructures());
    }

    private static Map<ResourceLocation, Pair<PlatformStructure<BlockData>, StructureRule>> initStructures() {
        Map<ResourceLocation, Pair<PlatformStructure<BlockData>, StructureRule>> result = new HashMap<>();
        new StructureResolvers<BlockData>().structures.forEach(it -> {
            result.put(it.getSecond().getResource(), it);
        });
        return result;
    }

    @Override
    public @NotNull Map<ResourceLocation, Pair<PlatformStructure<BlockData>, StructureRule>> getStructures() {
        return structures;
    }

    @Override
    public @Nullable NbtStructure<BlockData> getNBTStructure(ResourceLocation resourceLocation) {
        var file = Bukkit.getStructureManager().getStructureFile(NamespacedKey.fromString(resourceLocation.asString()));
        return new NbtStructure<>(file);
    }

    @Override
    public @Nullable PlatformStructure<BlockData> getStructure(ResourceLocation id) {
        var struct = structures.get(id).getFirst();
        return struct != null ? struct : new NBTBasedStructure<>(id);
    }

    @Override
    public void addStructure(@NotNull ResourceLocation id, @NotNull PlatformStructure<BlockData> structure, @NotNull StructureRule rule) {
        structures.put(id, new Pair<>(structure, rule));
    }
}
