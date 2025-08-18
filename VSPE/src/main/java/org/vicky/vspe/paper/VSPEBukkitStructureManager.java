package org.vicky.vspe.paper;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.Nullable;
import org.vicky.platform.utils.ResourceLocation;
import org.vicky.vspe.platform.PlatformStructureManager;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.NbtStructure;

public class VSPEBukkitStructureManager implements PlatformStructureManager<BlockData> {
    @Override
    public @Nullable NbtStructure<BlockData> getStructure(ResourceLocation resourceLocation) {
        var file = Bukkit.getStructureManager().getStructureFile(NamespacedKey.fromString(resourceLocation.asString()));
        return new NbtStructure<>(file, Bukkit::createBlockData);
    }
}
