package org.vicky.vspe.nms

import net.minecraft.core.BlockPos
import net.minecraft.util.RandomSource
import org.bukkit.Bukkit
import org.bukkit.Material

typealias PaletteFunction = (
    pos: BlockPos,
    height: Int,
    distanceFromCenter: Int,
    random: RandomSource
) -> Material

// Put inside your NMS init or call from onEnable for one run
fun dumpServerIntrospection() {
    try {
        val serverClass = net.minecraft.server.MinecraftServer::class.java
        val logger = org.bukkit.Bukkit.getLogger()

        logger.info("=== NMS Introspection START ===")
        logger.info("MinecraftServer class: ${serverClass.name}")

        logger.info("-- Declared methods on MinecraftServer --")
        serverClass.declaredMethods.sortedBy { it.name }.forEach { m ->
            logger.info("M: ${m.name} -> returns=${m.returnType.name} params=${m.parameterCount} modifiers=${m.modifiers}")
        }

        logger.info("-- Public methods on MinecraftServer --")
        serverClass.methods.sortedBy { it.name }.forEach { m ->
            logger.info("PM: ${m.name} -> returns=${m.returnType.name} params=${m.parameterCount}")
        }

        logger.info("-- Declared fields on MinecraftServer --")
        serverClass.declaredFields.sortedBy { it.name }.forEach { f ->
            f.isAccessible = true
            logger.info("F: ${f.name} -> type=${f.type.name} static=${java.lang.reflect.Modifier.isStatic(f.modifiers)}")
        }

        // Try to obtain a server instance a few ways and inspect instance methods
        val serverInstanceCandidates = mutableListOf<Any?>()

        // try getServer() static method
        try {
            serverClass.getDeclaredMethod("getServer").let { m ->
                m.isAccessible = true
                val inst = m.invoke(null)
                logger.info("Called static getServer(): got instance=${inst?.javaClass?.name}")
                serverInstanceCandidates += inst
            }
        } catch (t: Throwable) {
            logger.info("getServer() not present or failed: ${t.message}")
        }

        // try to call any zero-arg static method that returns MinecraftServer (fallback)
        serverClass.methods.filter { it.parameterCount == 0 && serverClass.isAssignableFrom(it.returnType) }
            .forEach { m ->
                try {
                    val inst = m.invoke(null)
                    logger.info("Static candidate ${m.name}() returned ${inst?.javaClass?.name}")
                    serverInstanceCandidates += inst
                } catch (t: Throwable) {
                    logger.info("Static candidate ${m.name}() failed: ${t.message}")
                }
            }

        // try looking at static fields that may hold a server
        serverClass.declaredFields.forEach { f ->
            try {
                f.isAccessible = true
                val valObj = f.get(null)
                if (valObj != null && serverClass.isAssignableFrom(valObj.javaClass)) {
                    logger.info("Static field ${f.name} appears to hold server instance: ${valObj.javaClass.name}")
                    serverInstanceCandidates += valObj
                }
            } catch (_: Throwable) {
            }
        }

        serverInstanceCandidates.distinct().forEach { inst ->
            if (inst == null) return@forEach
            logger.info("--- Inspecting instance ${inst.javaClass.name} methods ---")
            inst.javaClass.methods.sortedBy { it.name }.forEach { m ->
                logger.info("I: ${m.name} -> returns=${m.returnType.name} params=${m.parameterCount}")
            }
            // look for registryAccess-like methods on instance
            val candidates =
                inst.javaClass.methods.filter { it.parameterCount == 0 && it.name.equals("registryAccess", true) }
            logger.info("Found registryAccess-like instance methods: ${candidates.map { it.name }}")
            // find any method that returns an object with a 'registry' method
            inst.javaClass.methods.forEach { m ->
                try {
                    val res = m.invoke(inst)
                    if (res != null) {
                        val found =
                            res.javaClass.methods.any { it.name.equals("registry", true) && it.parameterCount == 1 }
                        if (found) {
                            logger.info("Method ${m.name} returned a type ${res.javaClass.name} that has a registry(...) method")
                        }
                    }
                } catch (_: Throwable) {
                }
            }
        }

        // Look for Registries class and BIOME field
        val registriesCandidates = listOf(
            "net.minecraft.core.registries.Registries",
            "net.minecraft.core.registries.RegistryData",
            "net.minecraft.core.registries.Registry"
        )
        for (p in registriesCandidates) {
            try {
                val cls = Class.forName(p)
                logger.info("Found Registries class: $p, fields: ${cls.declaredFields.map { it.name }}")
                try {
                    val field = cls.getField("BIOME")
                    logger.info("Found Registries.BIOME field type=${field.type.name}")
                } catch (nf: NoSuchFieldException) {
                    logger.info("No BIOME field on $p")
                }
            } catch (_: ClassNotFoundException) {
                logger.info("Registries class not found at $p")
            }
        }

        logger.info("=== NMS Introspection END ===")
    } catch (t: Throwable) {
        Bukkit.getLogger().severe("Introspection failed: ${t.message}")
    }
}