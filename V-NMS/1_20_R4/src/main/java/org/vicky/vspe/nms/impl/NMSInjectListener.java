package org.vicky.vspe.nms.impl;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vicky.vspe.paper.BukkitChunkGeneratorWrapper;
import org.vicky.vspe.platform.VSPEPlatformPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

public class NMSInjectListener implements Listener {
    private static final Logger LOGGER = LoggerFactory.getLogger(NMSInjectListener.class);
    private static final Set<World> INJECTED = new HashSet<>();
    private static final ReentrantLock INJECT_LOCK = new ReentrantLock();

    @EventHandler
    public void onWorldInit(WorldInitEvent event) {
        World bukkitWorld = event.getWorld();

        // only inject once
        if (INJECTED.contains(bukkitWorld)) return;

        // check generator type guard
        if (!(bukkitWorld.getGenerator() instanceof BukkitChunkGeneratorWrapper wrapper)) return;

        INJECT_LOCK.lock();
        try {
            if (INJECTED.contains(bukkitWorld)) return;
            INJECTED.add(bukkitWorld);

            LOGGER.info("Preparing to inject NMS chunk generator into world: {}", bukkitWorld.getName());

            CraftWorld craftWorld = (CraftWorld) bukkitWorld;
            ServerLevel serverWorld = craftWorld.getHandle();

            // acquire chunkMap from chunk source (search for a field whose value is a ChunkMap)
            Object chunkSource = serverWorld.getChunkSource();
            Optional<Field> chunkMapFieldOpt = ReflectionUtil.findFieldByType(chunkSource, ChunkMap.class);
            if (chunkMapFieldOpt.isEmpty()) {
                LOGGER.error("Failed to find ChunkMap field on chunkSource for world {}", bukkitWorld.getName());
                return;
            }
            Field chunkMapField = chunkMapFieldOpt.get();
            chunkMapField.setAccessible(true);
            ChunkMap chunkMap = (ChunkMap) ReflectionUtil.getFieldValue(chunkMapField, chunkSource);

            if (chunkMap == null) {
                LOGGER.error("ChunkMap was null for world {}", bukkitWorld.getName());
                return;
            }

            // find the existing "WorldGenContext" object inside ChunkMap: search for a field whose class name contains "WorldGenContext"
            Optional<Field> worldGenContextFieldOpt = ReflectionUtil.findFieldByPredicate(chunkMap, f -> {
                Class<?> t = f.getType();
                return t.getSimpleName().toLowerCase().contains("worldgencontext") || t.getSimpleName().toLowerCase().contains("worldgenerationcontext");
            });

            if (worldGenContextFieldOpt.isEmpty()) {
                // Fallback: search for any non-primitive field that has "level" and "structureManager" methods on its class (heuristic)
                Optional<Field> heuristic = ReflectionUtil.findFieldByPredicate(chunkMap, f -> {
                    try {
                        Class<?> t = f.getType();
                        Method[] methods = t.getMethods();
                        boolean hasLevel = false, hasStructure = false;
                        for (Method m : methods) {
                            String name = m.getName().toLowerCase();
                            if (name.contains("level")) hasLevel = true;
                            if (name.contains("structuremanager")) hasStructure = true;
                        }
                        return hasLevel && hasStructure;
                    } catch (Throwable ex) {
                        return false;
                    }
                });
                if (heuristic.isEmpty()) {
                    LOGGER.error("Failed to find WorldGenContext field inside ChunkMap for world {}", bukkitWorld.getName());
                    return;
                } else {
                    worldGenContextFieldOpt = heuristic;
                }
            }

            Field worldGenContextField = worldGenContextFieldOpt.get();
            worldGenContextField.setAccessible(true);
            Object oldContext = ReflectionUtil.getFieldValue(worldGenContextField, chunkMap);
            if (oldContext == null) {
                LOGGER.error("Found worldGenContext field but it was null for {}", bukkitWorld.getName());
                return;
            }

            Class<?> worldGenContextClass = oldContext.getClass();
            // extract important components by calling accessor methods reflectively
            Method levelAccessor = ReflectionUtil.findMethodByNameContains(worldGenContextClass, "level")
                    .orElseThrow(() -> new IllegalStateException("worldGenContext.level() not found"));
            Optional<Method> structAccessor = ReflectionUtil.findMethodByNameContains(worldGenContextClass, "structure");
            Optional<Method> lightAccessor = ReflectionUtil.findMethodByNameContains(worldGenContextClass, "light");
            Optional<Method> executorAccessor = ReflectionUtil.findMethodByNameContains(worldGenContextClass, "executor");
            Optional<Method> unsavedAccessor = ReflectionUtil.findMethodByNameContains(worldGenContextClass, "unsaved");

            Object levelObj = levelAccessor.invoke(oldContext);
            Object structureManagerObj = structAccessor.isEmpty() ? null : structAccessor.get().invoke(oldContext);
            Object lightEngineObj = lightAccessor.isEmpty() ? null : lightAccessor.get().invoke(oldContext);
            Object mainThreadExecutorObj = executorAccessor.isEmpty() ? null : executorAccessor.get().invoke(oldContext);
            Object unsavedListenerObj = unsavedAccessor.isEmpty() ? null : unsavedAccessor.get().invoke(oldContext);

            // create your NMSChunkGeneratorDelegate instance (the delegate must take 'vanilla' chunk generator and other args).
            // Grab the vanilla generator from serverWorld
            ChunkGenerator vanilla = serverWorld.getChunkSource().getGenerator();

            // Construct new NMSChunkGeneratorDelegate
            // NOTE: ensure you have an appropriate constructor signature in NMSChunkGeneratorDelegate
            NMSChunkGenerator delegate = new NMSChunkGenerator(
                    new NMSBiomeSource(wrapper.getResolver()),
                    bukkitWorld.getSeed()
            );

            // find a constructor of WorldGenContext-like class that takes 6 args (level, chunkGenerator, structureManager, lightEngine, executor, unsavedListener)
            Optional<Constructor<?>> ctorOpt = ReflectionUtil.findConstructorWithParamCount(worldGenContextClass, 6);
            if (ctorOpt.isEmpty()) {
                LOGGER.error("Could not find a 6-arg constructor for WorldGenContext class {}; you'll need to adapt this code.", worldGenContextClass.getName());
                return;
            }
            Constructor<?> constructor = ctorOpt.get();
            constructor.setAccessible(true);

            // Build the new worldGenContext instance. Pass null for any component that we couldn't find; many versions accept null for some args.
            Object newContext = constructor.newInstance(
                    levelObj,
                    delegate,                // ChunkGenerator
                    structureManagerObj,
                    lightEngineObj,
                    mainThreadExecutorObj,
                    unsavedListenerObj
            );

            // replace the field on chunkMap with the new context
            ReflectionUtil.setFieldValue(worldGenContextField, chunkMap, newContext);

            LOGGER.info("Successfully injected NMSChunkGeneratorDelegate into world {}", bukkitWorld.getName());
        } catch (Throwable ex) {
            VSPEPlatformPlugin.platformLogger().error("Failed to inject NMS generator into world " + bukkitWorld.getName() + ": " + ex.getMessage(), ex);
            INJECTED.remove(bukkitWorld);
        } finally {
            INJECT_LOCK.unlock();
        }
    }
}