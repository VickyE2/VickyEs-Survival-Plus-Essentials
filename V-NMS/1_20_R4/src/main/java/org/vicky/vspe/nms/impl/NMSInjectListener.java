package org.vicky.vspe.nms.impl;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vicky.vspe.paper.BukkitChunkGeneratorWrapper;
import org.vicky.vspe.platform.VSPEPlatformPlugin;

import java.util.HashSet;
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
            ChunkMap chunkSource = serverWorld.getChunkSource().chunkMap;
            chunkSource.generator = new NMSChunkGenerator(new NMSBiomeSource(wrapper.getResolver()), serverWorld.getSeed(), wrapper.getDescriptor());

            LOGGER.info("Successfully injected NMSChunkGeneratorDelegate into world {}", bukkitWorld.getName());
        } catch (Throwable ex) {
            VSPEPlatformPlugin.platformLogger().error("Failed to inject NMS generator into world " + bukkitWorld.getName() + ": " + ex.getMessage(), ex);
            INJECTED.remove(bukkitWorld);
        } finally {
            INJECT_LOCK.unlock();
        }
    }
}