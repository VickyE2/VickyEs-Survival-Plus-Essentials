package org.vicky.vspe.paper;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Nullable;
import org.vicky.platform.utils.ResourceLocation;
import org.vicky.vspe.platform.PlatformStructureManager;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.NbtStructure;

public class VSPEStructureManager implements PlatformStructureManager {
    @Override
    public @Nullable <T> NbtStructure<T> getStructure(ResourceLocation resourceLocation) {
        var file = Bukkit.getStructureManager().getStructureFile(NamespacedKey.fromString(resourceLocation.asString()));
        return new NbtStructure<T>(file, (t) -> Bukkit.createBlockData(t));
    }
}
