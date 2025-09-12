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
     * Called automatically on ServerAboutToStartEvent. Registers LevelStem objects into BuiltinRegistries.LEVEL_STEM.
     */
    public static void applyDimensionTypes(BootstapContext<DimensionType> context) {
        if (PENDING.isEmpty()) return;
        List<Map.Entry<ResourceLocation, PendingEntry>> toApply = new ArrayList<>(PENDING.entrySet());

        for (var entry : toApply) {
            PendingEntry pending = entry.getValue();
            context.register(pending.dimensionTypeKey, pending.dimensionType);
        }
    }

    /**
     * Called automatically on ServerAboutToStartEvent. Registers LevelStem objects into BuiltinRegistries.LEVEL_STEM.
     */
    public static void applyLevelStems(BootstapContext<LevelStem> context) {
        if (PENDING.isEmpty()) return;

        var registryAccess = server.registryAccess();
        var dimTypeRegistry = registryAccess.registryOrThrow(Registries.DIMENSION_TYPE);

        // We iterate over a copy so callers can modify PENDING inside generatorSupplier if needed
        List<Map.Entry<ResourceLocation, PendingEntry>> toApply = new ArrayList<>(PENDING.entrySet());

        for (var entry : toApply) {
            ResourceLocation id = entry.getKey();
            if (REGISTERED.contains(id)) {
                PENDING.remove(id);
                continue;
            }

            PendingEntry pending = entry.getValue();
            try {
                // obtain a Holder<DimensionType> from the server registry access
                Holder<DimensionType> dimTypeHolder = dimTypeRegistry.getHolderOrThrow(pending.dimensionTypeKey);

                // create the chunk generator (lazy)
                ChunkGenerator generator = pending.generatorSupplier.get();
                // create LevelStem and register it into builtin registries so it becomes visible to Dimension system
                LevelStem stem = new LevelStem(dimTypeHolder, generator);

                // Register into BuiltinRegistries.LEVEL_STEM (this makes it known to vanilla's "builtin" stems)
                context.register(pending.dimKey, stem);

                REGISTERED.add(id);

                if (server != null) {
                    server.sendSystemMessage(Component.literal("[WorldManager] Registered dimension: " + id));
                } else {
                    System.out.println("[WorldManager] Registered dimension (no server): " + id);
                }

                if (PENDING_WORLDS.containsKey(id)) {
                    var sup = PENDING_WORLDS.remove(id);
                    sup.apply(getLevelFromKey(pending.levelKey));
                }
            } catch (Exception ex) {
                server.sendSystemMessage(net.minecraft.network.chat.Component.literal("[WorldManager] Failed to register " + id + ": " + ex.getMessage()));
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
