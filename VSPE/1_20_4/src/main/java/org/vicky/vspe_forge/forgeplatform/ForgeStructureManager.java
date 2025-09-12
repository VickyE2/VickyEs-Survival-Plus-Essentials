package org.vicky.vspe_forge.forgeplatform;

import kotlin.Pair;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vicky.platform.utils.ResourceLocation;
import org.vicky.vspe.platform.PlatformStructureManager;
import org.vicky.vspe.platform.systems.dimension.globalDimensions.StructureResolvers;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.NBTBasedStructure;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.NbtStructure;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.PlatformStructure;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.StructureRule;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import static org.vicky.forge.forgeplatform.useables.ForgeHacks.fromVicky;

public class ForgeStructureManager implements PlatformStructureManager<BlockState> {
    private static final Map<ResourceLocation, Pair<PlatformStructure<BlockState>, StructureRule>> structures
            = new HashMap<>();
    private static ForgeStructureManager INSTANCE;

    static {
        structures.putAll(initStructures());
    }

    private final ResourceManager manager;
    private final Map<ResourceLocation, File> fileCache = new HashMap<>();

    private ForgeStructureManager(ResourceManager manager) {
        this.manager = manager;
    }

    public static void createInstance(ResourceManager manager) {
        if (INSTANCE == null) {
            INSTANCE = new ForgeStructureManager(manager);
        } else {
            throw new IllegalStateException("ForgeStructureManager is already initialized");
        }
    }

    public static ForgeStructureManager getInstance() {
        return INSTANCE;
    }

    private static Map<ResourceLocation, Pair<PlatformStructure<BlockState>, StructureRule>> initStructures() {
        Map<ResourceLocation, Pair<PlatformStructure<BlockState>, StructureRule>> result = new HashMap<>();
        new StructureResolvers<BlockState>().structures.forEach(it -> {
            result.put(it.getSecond().getResource(), it);
        });
        return result;
    }

    @Override
    public @NotNull Map<ResourceLocation, Pair<PlatformStructure<BlockState>, StructureRule>> getStructures() {
        return structures;
    }

    @Override
    public @Nullable NbtStructure<BlockState> getNBTStructure(ResourceLocation resourceLocation) {
        try {
            return new NbtStructure<>(getOrCacheFile(resourceLocation));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @Nullable PlatformStructure<BlockState> getStructure(ResourceLocation id) {
        var struct = structures.get(id).getFirst();
        return struct != null ? struct : new NBTBasedStructure<>(id);
    }

    @Override
    public void addStructure(@NotNull ResourceLocation id, @NotNull PlatformStructure<BlockState> structure, @NotNull StructureRule rule) {
        structures.put(id, new Pair<>(structure, rule));
    }

    private File getOrCacheFile(ResourceLocation id) throws IOException {
        if (fileCache.containsKey(id)) {
            return fileCache.get(id);
        }

        Resource resource = manager.getResourceOrThrow(fromVicky(id));
        File temp = File.createTempFile(id.getPath().replace('/', '_'), ".nbt");
        temp.deleteOnExit();

        try (InputStream in = resource.open();
             OutputStream out = new FileOutputStream(temp)) {
            in.transferTo(out);
        }

        fileCache.put(id, temp);
        return temp;
    }
}
