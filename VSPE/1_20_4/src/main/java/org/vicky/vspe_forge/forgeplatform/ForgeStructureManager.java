package org.vicky.vspe_forge.forgeplatform;

import kotlin.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vicky.platform.utils.ResourceLocation;
import org.vicky.vspe.StructureTag;
import org.vicky.vspe.platform.PlatformStructureManager;
import org.vicky.vspe.platform.systems.dimension.globalDimensions.StructureResolvers;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.NBTBasedStructure;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.NbtStructure;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.PlatformStructure;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.StructureRule;
import org.vicky.vspe_forge.VspeForge;
import org.vicky.vspe_forge.dimension.ForgeTypePlatformStructure;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.vicky.forge.forgeplatform.useables.ForgeHacks.fromVicky;
import static org.vicky.vspe_forge.VspeForge.datagenBiomeAccess;
import static org.vicky.vspe_forge.dimension.ForgeDescriptorBasedDimension.stringToSeed;

public class ForgeStructureManager implements PlatformStructureManager<BlockState> {
    private static final Map<ResourceLocation, Pair<PlatformStructure<BlockState>, StructureRule>> structures
            = new HashMap<>();
    private static final Map<ResourceLocation, RegistryObject<Structure>> structureHolders = new HashMap<>();
    private static final List<Holder<StructureSet>> HOLDERS = new ArrayList<>();
    private static final Map<Pair<String, StructureTag>, List<StructureSet.StructureSelectionEntry>> STRUCTURE_SETS = new HashMap<>();
    private static ForgeStructureManager INSTANCE;

    static {
        structures.putAll(initStructures());
    }

    private final Supplier<ResourceManager> manager;
    private final Map<ResourceLocation, File> fileCache = new HashMap<>();

    private ForgeStructureManager(Supplier<ResourceManager> manager) {
        this.manager = manager;
    }

    public static void createInstance(Supplier<ResourceManager> manager) {
        if (INSTANCE == null) {
            INSTANCE = new ForgeStructureManager(manager);
        } else {
            throw new IllegalStateException("ForgeStructureManager is already initialized");
        }
    }

    public static void destroyInstance() {
        INSTANCE = null;
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

    public static void registerPlatformStructure(BootstapContext<Structure> ctx) {
        structures.values().forEach(it -> {
            datagenBiomeAccess = ctx.lookup(Registries.BIOME);
            var struct = new ForgeTypePlatformStructure(it.getFirst(), it.getSecond());
            ctx.register(
                    ResourceKey.create(Registries.STRUCTURE, fromVicky(it.getSecond().getResource())),
                    struct
            );
        });
    }

    public static void boostrap(BootstapContext<StructureSet> context) {
        structures.values().forEach(it -> {
            HolderGetter<Structure> structureLookup = context.lookup(Registries.STRUCTURE);
            Holder<Structure> structureType = structureLookup.getOrThrow(
                    ResourceKey.create(Registries.STRUCTURE, new net.minecraft.resources.ResourceLocation(it.getSecond().getResource().asString()))
            );

            createStructureSet(it, structureType);
            VspeForge.LOGGER.info("We created this structure: {}", structureType.unwrapKey().orElse(null));
        });

        STRUCTURE_SETS.forEach((key, sets) -> {
            var oop = getRecommendedSpacing(key.getSecond());
            HOLDERS.add(
                    context.register(
                            ResourceKey.create(Registries.STRUCTURE_SET, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(key.getFirst(), key.getSecond().name().toLowerCase())),
                            new StructureSet(sets, new RandomSpreadStructurePlacement(
                                    oop.spacing,
                                    oop.separation,
                                    RandomSpreadType.LINEAR,
                                    Math.abs(Math.round(stringToSeed(key.getSecond().name())))
                            ))
                    )
            );
        });
        STRUCTURE_SETS.clear();
    }

    private static void createStructureSet(Pair<PlatformStructure<BlockState>, StructureRule> it, Holder<Structure> myStructure) {
        StructureSet.StructureSelectionEntry entry =
                new StructureSet.StructureSelectionEntry(myStructure, it.getSecond().getWeight());
        STRUCTURE_SETS.computeIfAbsent(
                new Pair<>(it.getSecond().getResource().getNamespace(), it.getSecond().getTags()), us -> new ArrayList<>()).add(entry);
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

        Resource resource = manager.get().getResourceOrThrow(fromVicky(id));
        File temp = File.createTempFile(id.getPath().replace('/', '_'), ".nbt");
        temp.deleteOnExit();

        try (InputStream in = resource.open();
             OutputStream out = new FileOutputStream(temp)) {
            in.transferTo(out);
        }

        fileCache.put(id, temp);
        return temp;
    }

    public static StructureSpacing getRecommendedSpacing(StructureTag type) {
        return switch (type) {
            case TREELIKE -> new StructureSpacing(8, 3);       // Common, small features
            case HOUSE -> new StructureSpacing(10, 4);          // Small overworld buildings
            case RUINS -> new StructureSpacing(20, 6);          // Scattered ruins
            case DUNGEON -> new StructureSpacing(24, 6);        // Underground, medium rarity
            case FROZEN -> new StructureSpacing(28, 7);         // Special biome substructures
            case VILLAGE -> new StructureSpacing(40, 8);        // Large population centers
            case OCEAN -> new StructureSpacing(48, 10);         // Big oceanic structures
            case SKY -> new StructureSpacing(56, 12);           // Floating, should feel rare
            case NETHER -> new StructureSpacing(64, 12);        // Harsh environment, spaced apart
            case ANCIENT -> new StructureSpacing(80, 16);       // Very rare, major lore structures
            case EMPTY -> new StructureSpacing(120, 20);        // Placeholder or unused category
        };
    }

    public record StructureSpacing(int spacing, int separation) {
    }
}
