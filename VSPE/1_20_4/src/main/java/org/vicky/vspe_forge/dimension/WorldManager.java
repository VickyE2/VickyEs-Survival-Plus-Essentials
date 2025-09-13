package org.vicky.vspe_forge.dimension;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import org.vicky.vspe.platform.systems.dimension.DimensionDescriptor;
import org.vicky.vspe_forge.VspeForge;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.vicky.vspe_forge.VspeForge.server;
import static org.vicky.vspe_forge.forgeplatform.AwsomeForgeHacks.getLevelFromKey;

/**
 * WorldManager
 * SAFE usage:
 * - call registerAtStartup(id, generatorSupplier, dimensionTypeKey) during mod init
 * - WorldManager will register LevelStem into BuiltinRegistries.LEVEL_STEM on ServerAboutToStartEvent
 * NOTE: Runtime ServerLevel creation is intentionally omitted (dangerous). Use datapack reload / server restart
 * after registration if you want the world to be fully available immediately.
 */
public final class WorldManager {
    // pending registrations that will be applied on server start
    private static final Map<ResourceLocation, PendingEntry> PENDING = new LinkedHashMap<>();
    private static final Set<ResourceLocation> REGISTERED = new HashSet<>();
    private static final Map<ResourceLocation, Function<ServerLevel, Void>> PENDING_WORLDS = new HashMap<>();
    private static final Map<ResourceLocation, Holder.Reference<DimensionType>> REFRENCES = new HashMap<>();
    private static final Map<ResourceKey<Level>, DimensionDescriptor> DESCRIPTORS = new HashMap<>();
    private WorldManager() {
    }

    /**
     * Register a dimension LevelStem to be created on server startup.
     *
     * @param id                dimension id (e.g. new ResourceLocation(MODID, "my_dim"))
     * @param generatorSupplier supplier producing your generator (lazy)
     */
    public static void registerAtStartup(ResourceLocation id,
                                         DimensionType type,
                                         Supplier<ChunkGenerator> generatorSupplier,
                                         Function<ServerLevel, Void> supplier) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(generatorSupplier, "generatorSupplier");
        final ResourceKey<LevelStem> LEVEL_STEM = ResourceKey.create(Registries.LEVEL_STEM, id);
        final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, id);
        final ResourceKey<DimensionType> DIMENSION_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, id);

        if (PENDING.containsKey(id) || REGISTERED.contains(id)) return;
        PENDING.put(id, new PendingEntry(generatorSupplier, DIMENSION_TYPE, LEVEL_STEM, LEVEL, type));
        if (supplier != null) PENDING_WORLDS.put(id, supplier);
    }

    /**
     * Register a dimension LevelStem to be created on server startup.
     *
     * @param id                dimension id (e.g. new ResourceLocation(MODID, "my_dim"))
     * @param generatorSupplier supplier producing your generator (lazy)
     * @param descriptor        A dimension that's controlled by a descriptor
     */
    public static void registerAtStartup(ResourceLocation id,
                                         DimensionType type,
                                         Supplier<ChunkGenerator> generatorSupplier,
                                         Function<ServerLevel, Void> supplier,
                                         DimensionDescriptor descriptor) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(generatorSupplier, "generatorSupplier");
        final ResourceKey<LevelStem> LEVEL_STEM = ResourceKey.create(Registries.LEVEL_STEM, id);
        final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, id);
        final ResourceKey<DimensionType> DIMENSION_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, id);

        if (PENDING.containsKey(id) || REGISTERED.contains(id)) return;
        PENDING.put(id, new PendingEntry(generatorSupplier, DIMENSION_TYPE, LEVEL_STEM, LEVEL, type));
        if (supplier != null) PENDING_WORLDS.put(id, supplier);
        DESCRIPTORS.put(ResourceKey.create(Registries.DIMENSION, id), descriptor);
    }

    /**
     * Called automatically on GenerateDataEvent. Registers DimensionType objects.
     */
    public static void applyDimensionTypes(BootstapContext<DimensionType> context) {
        if (PENDING.isEmpty()) return;
        List<Map.Entry<ResourceLocation, PendingEntry>> toApply = new ArrayList<>(PENDING.entrySet());

        for (var entry : toApply) {
            VspeForge.LOGGER.info("Registering DimensionType: {}", entry.getValue().dimensionTypeKey);
            PendingEntry pending = entry.getValue();
            REFRENCES.put(
                    entry.getKey(),
                    context.register(pending.dimensionTypeKey, pending.dimensionType)
            );
        }
    }

    /**
     * Called automatically on GenerateDataEvent. Registers LevelStem objects.
     */
    public static void applyLevelStems(BootstapContext<LevelStem> context) {
        if (PENDING.isEmpty()) return;

        // iterate over a copy so callers can modify PENDING inside generatorSupplier
        List<Map.Entry<ResourceLocation, PendingEntry>> toApply = new ArrayList<>(PENDING.entrySet());

        for (var entry : toApply) {
            ResourceLocation id = entry.getKey();
            if (REGISTERED.contains(id)) {
                PENDING.remove(id);
                continue;
            }

            PendingEntry pending = entry.getValue();
            try {
                // Data-gen path: we may not have a server; do not use server.registryAccess() here.
                Holder.Reference<DimensionType> dimHolder = REFRENCES.get(entry.getKey());

                // create the chunk generator lazily
                ChunkGenerator generator = pending.generatorSupplier.get();

                // create LevelStem and register it into this BootstapContext
                LevelStem stem = new LevelStem(dimHolder, generator);
                context.register(pending.dimKey, stem);

                REGISTERED.add(id);

                if (server != null) {
                    server.sendSystemMessage(Component.literal("[WorldManager] Registered dimension: " + id));
                } else {
                    System.out.println("[WorldManager] Registered dimension (data-gen/no-server): " + id);
                }

                // DO NOT try to create runtime ServerLevel here during data-gen.
                // Only call PENDING_WORLDS if server != null
                if (server != null && PENDING_WORLDS.containsKey(id)) {
                    var sup = PENDING_WORLDS.remove(id);
                    sup.apply(getLevelFromKey(pending.levelKey));
                }
            } catch (Exception ex) {
                if (server != null)
                    server.sendSystemMessage(Component.literal("[WorldManager] Failed to register " + id + ": " + ex.getMessage()));
                ex.printStackTrace();
            }
        }
    }

    /**
     * Is this id already registered via this manager?
     */
    public static boolean isRegistered(ResourceLocation id) {
        return REGISTERED.contains(id);
    }

    /**
     * Remove a pending registration (or forget a registered one locally).
     * NOTE: this does NOT safely remove entries from BuiltinRegistries — do not attempt to remove builtin registry entries at runtime.
     */
    public static boolean unregister(ResourceLocation id) {
        if (PENDING.remove(id) != null) return true;
        return REGISTERED.remove(id);
    }

    private static class PendingEntry {
        public final DimensionType dimensionType;
        final Supplier<ChunkGenerator> generatorSupplier;
        final ResourceKey<DimensionType> dimensionTypeKey;
        private final ResourceKey<LevelStem> dimKey;
        private final ResourceKey<Level> levelKey;

        PendingEntry(Supplier<ChunkGenerator> genSupplier,
                     ResourceKey<DimensionType> dimTypeKey,
                     ResourceKey<LevelStem> dimKey,
                     ResourceKey<Level> levelKey,
                     DimensionType dimensionType
        ) {
            this.generatorSupplier = genSupplier;
            this.dimensionTypeKey = dimTypeKey;
            this.dimKey = dimKey;
            this.levelKey = levelKey;
            this.dimensionType = dimensionType;
        }
    }

    public static DimensionDescriptor getDescriptor(ResourceKey<Level> location) {
        if (DESCRIPTORS.containsKey(location)) {
            return DESCRIPTORS.get(location);
        }
        return null;
    }
}
