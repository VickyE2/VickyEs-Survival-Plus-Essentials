package org.vicky.vspe.nms.impl

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.serialization.Lifecycle
import net.minecraft.commands.arguments.ParticleArgument
import net.minecraft.core.*
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.Music
import net.minecraft.sounds.SoundEvent
import net.minecraft.util.RandomSource
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.biome.*
import net.minecraft.world.level.block.entity.BarrelBlockEntity
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity
import net.minecraft.world.level.levelgen.WorldgenRandom
import net.minecraft.world.level.levelgen.structure.BoundingBox
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.block.structure.Mirror
import org.bukkit.craftbukkit.v1_20_R3.CraftServer
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld
import org.bukkit.craftbukkit.v1_20_R3.util.CraftMagicNumbers
import org.vicky.utilities.ContextLogger.ContextLogger
import org.vicky.vspe.nms.*
import org.vicky.vspe.nms.utils.EnumUtil
import org.vicky.vspe.nms.utils.ReflectionUtil.*
import java.lang.reflect.Field
import java.util.*
import net.minecraft.world.level.block.Mirror as NMSMirror
import net.minecraft.world.level.block.Rotation as NMSRotation
import org.bukkit.block.structure.Mirror as BukkitMirror
import org.bukkit.block.structure.StructureRotation as BukkitRotation

class v1_20_R4 : BiomeCompatibility {
    override fun getRegistry(): Registry<Biome> =
        (Bukkit.getServer() as CraftServer).server.registryAccess()
            .registryOrThrow<Biome>(Registries.BIOME)

    private val biomes: Registry<Biome> by lazy {
        MinecraftServer.getServer().registryAccess().registry(Registries.BIOME).orElseThrow()
    }

    fun initialize() {
        for (biome in biomes) {
            val nmsKey = biomes.getKey(biome)
            if (nmsKey == null) {
                NMS.logger?.print("[DEBUG] Could not find key for: $biome", true)
                continue
            }

            try {
                val key = NamespacedKey(nmsKey.namespace, nmsKey.path)
                BiomeRegistry.instance.add(key, BiomeWrapper_1_20_R4(biome))
            } catch (ex: Throwable) {
                NMS.logger?.print("Failed to load biome: $nmsKey")
                NMS.logger?.print(ex.message, true)
            }
        }
    }

    private fun getBiome(key: NamespacedKey): Biome? =
        biomes.get(ResourceLocation(key.namespace, key.key))

    override fun createBiome(key: NamespacedKey, base: BiomeWrapper): BiomeWrapper =
        BiomeWrapper_1_20_R4(key, base as BiomeWrapper_1_20_R4)

    override fun getBiomeAt(block: Block): BiomeWrapper? {
        val world: ServerLevel = (block.world as CraftWorld).handle
        val pos = BlockPos(block.x, block.y, block.z)
        world.getChunkIfLoaded(pos) ?: return null

        val biome = world.getBiome(pos).value()
        val location = biomes.getResourceKey(biome).orElseThrow()
        val key = NamespacedKey(location.location().namespace, location.location().path)

        return BiomeRegistry.instance[key]
            ?: BiomeWrapper_1_20_R4(getBiome(key)!!)
    }
}

/**
 * Compatibility for Paper 1.20.4 "v1_20_R3" builds.
 *
 * This uses reflection to find the Registry<Biome> in a few different ways to be resilient
 * to small signature/mapping differences across Paper snapshots/dev-bundles.
 */
class v1_20_R3 : BiomeCompatibility {

    override fun getRegistry(): Registry<Biome> =
        (Bukkit.getServer() as CraftServer).server.registryAccess()
            .registryOrThrow<Biome>(Registries.BIOME)

    // lazy load the Registry<Biome> via multiple reflection fallbacks
    private val biomes: Registry<Biome> by lazy {
        try {
            try {
                (Bukkit.getServer() as CraftServer).server.registryAccess()
                    .registryOrThrow<Biome>(Registries.BIOME)
            } catch (ex: Throwable) {
                NMS.logger?.print("Failed to locate CraftBukkit Registry Access: ${ex.message}", true)
                ex.printStackTrace()
                lookupBiomeRegistry()
            }
        } catch (ex: Throwable) {
            NMS.logger?.print("Failed to locate biome registry reflectively: ${ex.message}", true)
            throw ex
        }
    }

    /** MAIN initialize method – iterate biomes and register wrappers */
    fun initialize() {
        for (biome in biomes) {
            val nmsKey = biomes.getKey(biome)
            if (nmsKey == null) {
                NMS.logger?.print("[DEBUG] Could not find key for: $biome", true)
                continue
            }

            try {
                val key = NamespacedKey(nmsKey.namespace, nmsKey.path)
                BiomeRegistry.instance.add(key, BiomeWrapper_1_20_R4(biome))
            } catch (ex: Throwable) {
                NMS.logger?.print("Failed to load biome: $nmsKey", true)
                NMS.logger?.print(ex.message, true)
            }
        }
    }

    private fun getBiome(key: NamespacedKey): Biome? =
        biomes.get(ResourceLocation(key.namespace, key.key))

    override fun createBiome(key: NamespacedKey, base: BiomeWrapper): BiomeWrapper {
        return BiomeWrapper_1_20_R4(key, base as BiomeWrapper_1_20_R4)
    }

    override fun getBiomeAt(block: Block): BiomeWrapper? {
        val world: ServerLevel = (block.world as CraftWorld).handle
        val pos = BlockPos(block.x, block.y, block.z)
        world.getChunkIfLoaded(pos) ?: return null

        val biome = world.getBiome(pos).value()
        val location = biomes.getResourceKey(biome).orElseThrow()
        val key = NamespacedKey(location.location().namespace, location.location().path)

        return BiomeRegistry.instance[key] ?: BiomeWrapper_1_20_R4(getBiome(key)!!)
    }

    // --- reflection helpers ---

    /**
     * Tries several strategies to obtain Registry<Biome>:
     *  1) static MinecraftServer.registryAccess()
     *  2) instance MinecraftServer.getServer().registryAccess()
     *  3) search for any static method returning MinecraftServer and call instance.registryAccess()
     *  4) search for any method on server instance that looks like registryAccess
     *
     * Throws if none succeed.
     */
    @Suppress("UNCHECKED_CAST")
    private fun lookupBiomeRegistry(): Registry<Biome> {
        val serverClass = MinecraftServer::class.java
        val logger = NMS.logger

        // Candidate return-type name fragments we've observed (includes obfuscated names)
        val returnNameHints = listOf(
            "LayeredRegistryAccess",
            "IRegistryCustom",
            "RegistryAccess",
            "DynamicRegistries",
            "RegistryData",
            "IRegistry" // extra fuzz
        )

        // find zero-arg methods whose return type name matches any hint
        val candidates = serverClass.methods.filter { m ->
            m.parameterCount == 0 && returnNameHints.any { hint ->
                m.returnType.name.contains(hint, ignoreCase = true)
            }
        }

        logger?.print("[NMS] registry candidates: ${candidates.map { it.name }}", false)

        fun findRegistriesBiomeKey(): Any? {
            val candidatesNames = listOf(
                "net.minecraft.core.registries.Registries",
                "net.minecraft.core.registries.RegistryData",
                "net.minecraft.core.registries.DynamicRegistries",
                "net.minecraft.core.registries.RegistryOps"
            )
            for (clsName in candidatesNames) {
                try {
                    val cls = Class.forName(clsName)
                    // try uppercase then lowercase field names
                    try {
                        val field = cls.getField("BIOME")
                        field.isAccessible = true
                        return field.get(null)
                    } catch (_: NoSuchFieldException) {
                        // try case-insensitive search
                        val f = cls.declaredFields.firstOrNull {
                            it.name.equals("biome", ignoreCase = true) || it.name.equals("BIOME", ignoreCase = true)
                        }
                        if (f != null) {
                            f.isAccessible = true
                            return f.get(null)
                        }
                    }
                } catch (_: ClassNotFoundException) { /* try next */
                }
            }
            // final fallback: scan any class loaded with "Registries" in name for a BIOME-like field
            try {
                val all = listOf(
                    Class.forName("net.minecraft.core.registries.Registries", false, serverClass.classLoader)
                )
                for (c in all) {
                    try {
                        val f = c.getDeclaredField("BIOME")
                        f.isAccessible = true
                        return f.get(null)
                    } catch (_: Throwable) { /* ignore */
                    }
                }
            } catch (_: Throwable) {
            }
            return null
        }

        // Try each candidate method
        for (m in candidates) {
            try {
                m.isAccessible = true
                val ra = m.invoke(null) ?: continue
                // find a registry(...) method taking one parameter
                val registryMethod =
                    ra.javaClass.methods.firstOrNull { it.name.equals("registry", true) && it.parameterCount == 1 }
                if (registryMethod == null) {
                    logger?.print(
                        "[NMS] candidate ${m.name} returned ${ra.javaClass.name} but has no registry(...) method",
                        false
                    )
                    continue
                }

                val biomeKey = findRegistriesBiomeKey()
                    ?: throw IllegalStateException("Could not find Registries.BIOME field via reflection on this runtime")

                registryMethod.isAccessible = true
                val reg = registryMethod.invoke(ra, biomeKey)
                if (reg is Registry<*>) {
                    @Suppress("UNCHECKED_CAST")
                    return reg as Registry<Biome>
                } else {
                    logger?.print("[NMS] registry(...) returned ${reg?.javaClass?.name}, not a Registry<?>", true)
                }
            } catch (t: Throwable) {
                logger?.print("[NMS] candidate ${m.name} invocation failed: ${t.message}", true)
            }
        }

        // If we get here, try a last-resort brute-force: any zero-arg method that returns something,
        // invoke it and inspect the returned type for a registry(...) method (expensive but only once).
        val fallback = serverClass.methods.filter { it.parameterCount == 0 }
        for (m in fallback) {
            try {
                m.isAccessible = true
                val res = try {
                    m.invoke(null)
                } catch (_: Throwable) {
                    null
                } ?: continue
                val registryMethod =
                    res.javaClass.methods.firstOrNull { it.name.equals("registry", true) && it.parameterCount == 1 }
                if (registryMethod != null) {
                    val biomeKey = findRegistriesBiomeKey() ?: continue
                    registryMethod.isAccessible = true
                    val reg = registryMethod.invoke(res, biomeKey)
                    if (reg is Registry<*>) {
                        @Suppress("UNCHECKED_CAST")
                        return reg as Registry<Biome>
                    }
                }
            } catch (_: Throwable) {
            }
        }

        throw IllegalStateException("Could not locate MinecraftServer.registryAccess() or Registry<Biome> on this server build (${Bukkit.getVersion()}).")
    }
}


class BiomeWrapper_1_20_R4 : BiomeWrapper {

    companion object {
        private val climateSettingsField: Field
        private val temperatureAdjustmentField: Field
        private val generationSettingsField: Field
        private val mobSettingsField: Field
        private val particleDensityField: Field
        private val specialEffectsField: Field

        init {
            val clazz = getNMSClass("world.level.biome", "BiomeBase\$ClimateSettings")
            climateSettingsField = getField(Biome::class.java, clazz)
            temperatureAdjustmentField = getField(clazz, Biome.TemperatureModifier::class.java)
            generationSettingsField = getField(Biome::class.java, BiomeGenerationSettings::class.java)
            mobSettingsField = getField(Biome::class.java, MobSpawnSettings::class.java)
            particleDensityField = getField(AmbientParticleSettings::class.java, Float::class.javaPrimitiveType!!)
            specialEffectsField = getField(Biome::class.java, BiomeSpecialEffects::class.java)
        }
    }

    private val key: NamespacedKey
    private var base: Biome? = null
    private var biome: Biome? = null
    private var isVanilla = false
    private var isExternalPlugin = false
    private var isDirty = false
    private var resource: ResourceKey<Biome>? = null

    constructor(biome: Biome) {
        val biomes = MinecraftServer.getServer().registryAccess().registry(Registries.BIOME).orElseThrow()

        this.key = NamespacedKey.fromString(biomes.getKey(biome).toString())!!
        this.base = biome

        reset()
        val temp = this.base
        this.base = this.biome
        this.biome = temp

        isDirty = false
        isVanilla = true
        isExternalPlugin = this.key.key != NamespacedKey.MINECRAFT
    }

    constructor(key: NamespacedKey, base: BiomeWrapper_1_20_R4) {
        val biomes = MinecraftServer.getServer().registryAccess().registry(Registries.BIOME).orElseThrow()

        this.key = key
        this.base = biomes.get(ResourceLocation(base.getKey().namespace, base.getKey().key))
        reset()

        isDirty = true

        if (key == base.getKey()) {
            val temp = this.biome
            this.biome = this.base
            this.base = temp

            isVanilla = true
            isDirty = false
        }
    }

    override fun reset() {
        if (isVanilla) isDirty = false

        val temp = Biome.BiomeBuilder()
            .hasPrecipitation(base!!.hasPrecipitation())
            .temperature(base!!.baseTemperature)
            .downfall(base!!.climateSettings.downfall())
            .specialEffects(base!!.specialEffects)
            .mobSpawnSettings(base!!.mobSettings)
            .generationSettings(base!!.generationSettings)
            .temperatureAdjustment(
                invokeField(
                    temperatureAdjustmentField,
                    invokeField(climateSettingsField, base!!)
                ) as Biome.TemperatureModifier
            )
            .build()

        if (biome == null) {
            biome = temp
            return
        }

        setField(climateSettingsField, biome, invokeField(climateSettingsField, temp))
        setField(generationSettingsField, biome, invokeField(generationSettingsField, temp))
        setField(mobSettingsField, biome, invokeField(mobSettingsField, temp))
        setField(specialEffectsField, biome, invokeField(specialEffectsField, temp))
    }

    override fun getBase(): org.bukkit.block.Biome {
        if (isVanilla) return getBukkitBiome()

        val biomes = MinecraftServer.getServer().registryAccess().registry(Registries.BIOME).orElseThrow()
        val key = biomes.getResourceKey(base!!).orElseThrow()

        if (key.location().namespace != NamespacedKey.MINECRAFT) return org.bukkit.block.Biome.CUSTOM
        return EnumUtil.getIfPresent(org.bukkit.block.Biome::class.java, key.location().path)
            .orElse(org.bukkit.block.Biome.CUSTOM)
    }

    override fun setBase(biome: org.bukkit.block.Biome) {
        val biomes = MinecraftServer.getServer().registryAccess().registry(Registries.BIOME).orElseThrow()
        base = biomes.get(ResourceLocation(biome.key.namespace, biome.key.key))
            ?: throw IllegalArgumentException("Invalid biome: $biome")
    }

    override fun getKey(): NamespacedKey = key

    override fun getId(): Int {
        val biomes = MinecraftServer.getServer().registryAccess().registry(Registries.BIOME).orElseThrow()
        return biomes.getId(biome)
    }

    fun writeParticle(particle: ParticleOptions): String {
        return when (particle) {
            is DustParticleOptions -> {
                // TODO: Write actual Dust logic
                ""
            }
            else -> ""
        }
    }

    override fun getSpecialEffects(): SpecialEffectsBuilder {
        val effects = biome!!.specialEffects
        val builder = SpecialEffectsBuilder()
        builder.setFogColor(effects.fogColor)
            .setWaterColor(effects.waterColor)
            .setWaterFogColor(effects.waterFogColor)
            .setSkyColor(effects.skyColor)
            .setGrassColorModifier(effects.grassColorModifier.name)

        effects.grassColorOverride.ifPresent(builder::setGrassColorOverride)
        effects.foliageColorOverride.ifPresent(builder::setFoliageColorOverride)
        effects.ambientLoopSoundEvent.ifPresent { holder ->
            builder.setAmbientSound(holder.value().location.toString())
        }

        if (effects.ambientParticleSettings.isPresent) {
            val particle = effects.ambientParticleSettings.get()
            builder.setAmbientParticle(writeParticle(particle.options))
                .setParticleProbability(invokeField(particleDensityField, particle) as Float)
        }

        if (effects.ambientMoodSettings.isPresent) {
            val settings = effects.ambientMoodSettings.get()
            builder.setCaveSound(settings.soundEvent.value().location.toString())
                .setCaveTickDelay(settings.tickDelay)
                .setCaveSearchDistance(settings.blockSearchExtent)
                .setCaveSoundOffset(settings.soundPositionOffset)
        }

        if (effects.ambientAdditionsSettings.isPresent) {
            val settings = effects.ambientAdditionsSettings.get()
            builder.setRandomSound(settings.soundEvent.value().location.toString())
                .setRandomTickChance(settings.tickChance)
        }

        if (effects.backgroundMusic.isPresent) {
            val music = effects.backgroundMusic.get()
            builder.setMusicSound(music.event.value().location.toString())
                .setMusicMinDelay(music.minDelay)
                .setMusicMaxDelay(music.maxDelay)
                .setMusicOverride(music.replaceCurrentMusic())
        }

        return builder
    }

    override fun setSpecialEffects(builder: SpecialEffectsBuilder) {
        isDirty = true

        val particle = builder.getParticle()
        val music = builder.getMusic()
        val caveSettings = builder.getCaveSoundSettings()
        val cave = builder.getRandomSound()

        val a = BiomeSpecialEffects.Builder()
            .fogColor(builder.getFogColor())
            .waterColor(builder.getWaterColor())
            .waterFogColor(builder.getWaterFogColor())
            .skyColor(builder.getSkyColor())
            .grassColorModifier(
                BiomeSpecialEffects.GrassColorModifier.valueOf(builder.getGrassColorModifier().trim().uppercase())
            )

        if (builder.getGrassColorOverride() != -1) {
            a.grassColorOverride(builder.getGrassColorOverride())
        }
        if (builder.getFoliageColorOverride() != -1) {
            a.foliageColorOverride(builder.getFoliageColorOverride())
        }
        builder.getAmbientSound()?.let { a.ambientLoopSound(getSound(it)) }

        if (particle.particle != null) {
            try {
                val access: RegistryAccess = (Bukkit.getServer() as CraftServer).server.registryAccess()
                val nmsParticle = ParticleArgument.readParticle(
                    StringReader(particle.particle),
                    access as HolderLookup<ParticleType<*>?>
                )
                a.ambientParticle(AmbientParticleSettings(nmsParticle, particle.density))
            } catch (ex: CommandSyntaxException) {
                NMS.logger?.print("Could not set particle: $particle", true)
                ex.printStackTrace()
            }
        }
        caveSettings.sound?.let {
            a.ambientMoodSound(
                AmbientMoodSettings(
                    getSound(it),
                    caveSettings.tickDelay,
                    caveSettings.searchOffset,
                    caveSettings.soundOffset
                )
            )
        }
        cave.sound?.let {
            a.ambientAdditionsSound(AmbientAdditionsSettings(getSound(it), cave.tickChance))
        }
        music.sound?.let {
            a.backgroundMusic(Music(getSound(it), music.minDelay, music.maxDelay, music.isOverride))
        }

        setField(specialEffectsField, biome, a.build())
    }

    override fun setBiome(block: Block): Boolean {
        val world = (block.world as CraftWorld).handle
        val chunk = world.getChunkIfLoaded(BlockPos(block.x, block.y, block.z)) ?: return false

        val x = QuartPos.toSection(block.x)
        val y = QuartPos.toSection(block.y)
        val z = QuartPos.toSection(block.z)

        chunk.setBiome(x, y, z, Holder.direct(biome))
        return true
    }

    override fun register(isCustom: Boolean) {
        val biomes = MinecraftServer.getServer().registryAccess().registry(Registries.BIOME).orElseThrow()
        resource = ResourceKey.create(biomes.key(), ResourceLocation(key.namespace, key.key))

        if (biomes !is WritableRegistry<*>) throw InternalError("$biomes was not a writable registry???")

        if (isCustom) {
            val freezeField = getField(MappedRegistry::class.java, Boolean::class.javaPrimitiveType!!)
            setField(freezeField, biomes, false)

            val intrusiveHoldersField = getField(MappedRegistry::class.java, "m")
            setField(intrusiveHoldersField, biomes, HashMap<Any, Any>())

            (biomes as WritableRegistry<Biome>).apply {
                createIntrusiveHolder(biome!!)
                register(resource!!, biome!!, Lifecycle.stable())
            }

            setField(intrusiveHoldersField, biomes, null)
            setField(freezeField, biomes, true)
        }
        BiomeRegistry.instance.add(key, this)
    }

    override fun getResource(): ResourceKey<Biome>? = resource
    override fun setResource(key: ResourceKey<Biome>) {
        resource = key
    }

    override fun isExternalPlugin(): Boolean = isExternalPlugin
    override fun isDirty(): Boolean = isDirty
    override fun toString(): String = key.toString()
    override fun hashCode(): Int = key.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BiomeWrapper) return false
        return key == other.getKey()
    }

    private fun getSound(sound: String): Holder<SoundEvent> {
        val key = ResourceLocation.tryParse(sound)
        var existing = BuiltInRegistries.SOUND_EVENT.get(key)
        if (existing == null) {
            existing = SoundEvent.createVariableRangeEvent(key)
        }
        return Holder.direct(existing)
    }

    private fun getParticle(particle: String): ParticleType<*> {
        val key = ResourceLocation.tryParse(particle)
        return BuiltInRegistries.PARTICLE_TYPE.get(key)!!
    }
}

class NMS_v1_20_R3_Handler : NMSHandler() {
    override fun placeStructure(
        location: Location,
        structureName: String,
        namespace: String,
        loot_table: String?,
        rotation: BukkitRotation,
        mirror: Mirror,
        replacementPalette: Map<Material, PaletteFunction>
    ) {
        val world = (location.world as CraftWorld).handle

        // Convert enums
        val mcRotation = when (rotation) {
            BukkitRotation.NONE -> NMSRotation.NONE
            BukkitRotation.CLOCKWISE_90 -> NMSRotation.CLOCKWISE_90
            BukkitRotation.CLOCKWISE_180 -> NMSRotation.CLOCKWISE_180
            BukkitRotation.COUNTERCLOCKWISE_90 -> NMSRotation.COUNTERCLOCKWISE_90
        }
        val mcMirror = when (mirror) {
            Mirror.NONE -> NMSMirror.NONE
            Mirror.LEFT_RIGHT -> NMSMirror.LEFT_RIGHT
            Mirror.FRONT_BACK -> NMSMirror.FRONT_BACK
        }

        // Load the structure template
        val structureManager = world.server.structureManager
        val resourceLocation = ResourceLocation(namespace, structureName)
        val template = structureManager.get(resourceLocation)
            .orElseThrow { IllegalArgumentException("Structure $resourceLocation not found") }
        val settings =
            StructurePlaceSettings()
                .setRotation(mcRotation)
                .setMirror(mcMirror)
                .setIgnoreEntities(false)
        val basePos = BlockPos(location.blockX, location.blockY, location.blockZ)

        // Place it at location
        template.placeInWorld(
            world,
            basePos,
            basePos,
            settings,
            world.random,
            2
        )

        if (replacementPalette.isNotEmpty()) {
            val boundingBox = template.getBoundingBox(settings, basePos)
            val center = boundingBox.center

            BlockPos.betweenClosedStream(boundingBox).forEach { pos ->
                val blockState = world.getBlockState(pos)
                val material = CraftMagicNumbers.getMaterial(blockState.block)

                val paletteFunc = replacementPalette[material]
                if (paletteFunc != null) {
                    val height = pos.y - boundingBox.minY()
                    val distanceFromCenter = pos.distManhattan(center)

                    val replacementMaterial = paletteFunc(pos, height, distanceFromCenter, world.random)
                    val replacementBlock = CraftMagicNumbers.getBlock(replacementMaterial)

                    world.setBlock(pos, replacementBlock.defaultBlockState(), 3)
                }
            }
        }

        if (!loot_table.isNullOrEmpty()) {
            val boundingBox = template.getBoundingBox(settings, basePos)

            BlockPos.betweenClosedStream(boundingBox).forEach { pos ->
                val blockEntity = world.getBlockEntity(pos)
                if (blockEntity is RandomizableContainerBlockEntity) {
                    blockEntity.setLootTable(ResourceLocation(loot_table.split(":")[0], loot_table.split(":")[1]), world.random.nextLong())
                }
            }
        }
    }
    override fun placeJigsawStructure(
        location: Location,
        poolKey: String,
        namespace: String,
        level: ServerLevel,
        loot_table: String?,
        rotation: BukkitRotation
    ) {
        val registryAccess = level.registryAccess()
        val structureManager = level.structureManager()
        val templateManager = level.structureManager
        val chunkGenerator = level.chunkSource.generator
        val biomeSource = chunkGenerator.biomeSource
        val randomState = level.chunkSource.randomState()
        val seed = level.seed
        val worldGenRandom = WorldgenRandom(RandomSource.create(seed))
        val poolRegistry = registryAccess.registryOrThrow(Registries.TEMPLATE_POOL)
        val finalKey = ResourceKey.create(Registries.TEMPLATE_POOL, ResourceLocation(namespace, poolKey))
        val holder = poolRegistry.getHolder(finalKey).orElseGet { throw IllegalArgumentException("No pool for $finalKey found") }
        val origin = BlockPos(location.blockX, location.blockY, location.blockZ)

        val generationContext = Structure.GenerationContext(
            registryAccess,
            chunkGenerator,
            biomeSource,
            randomState,
            templateManager,
            worldGenRandom,
            seed,
            location.toChunkPos(),
            level
        ) { true }

        JigsawPlacement.addPieces(
            generationContext,
            holder,
            Optional.of(finalKey.location()),
            8,
            origin, // fallback name (starting jigsaw ID)
            false,
            Optional.empty(), // No heightmap
            128
        ) { it }
            .ifPresent { stub ->
                val pieces = stub.piecesBuilder.build().pieces

                for (piece in pieces) {
                    if (piece is PoolElementStructurePiece) {
                        piece.element.place(
                            templateManager,
                            level,
                            structureManager,
                            chunkGenerator,
                            origin,
                            piece.position,
                            rotation.toNMS(),
                            BoundingBox.infinite(),
                            level.random,
                            false
                        )
                        val chunk = level.getChunkAt(piece.position)
                        if (!loot_table.isNullOrEmpty())
                        for ((_, blockEntity) in chunk.blockEntities) {
                            val tables = loot_table.split(":")
                            when (blockEntity) {
                                is ChestBlockEntity -> {
                                    blockEntity.setLootTable(ResourceLocation(tables[0], tables[1]), level.random.nextLong())
                                }

                                is BarrelBlockEntity -> {
                                    blockEntity.setLootTable(ResourceLocation(tables[0], tables[1]), level.random.nextLong())
                                }

                                is ShulkerBoxBlockEntity -> {
                                    blockEntity.setLootTable(ResourceLocation(tables[0], tables[1]), level.random.nextLong())
                                }
                            }
                        }
                    }
                }
            }
    }

    override fun getBiomeCompatibility(): BiomeCompatibility = BiomeCompatibilityAPI.getBiomeCompatibility()
}

fun Location.toChunkPos(): ChunkPos {
    return ChunkPos(this.blockX shr 4, this.blockZ shr 4)
}

fun BukkitRotation.toNMS(): NMSRotation = when (this) {
    BukkitRotation.NONE -> NMSRotation.NONE
    BukkitRotation.CLOCKWISE_90 -> NMSRotation.CLOCKWISE_90
    BukkitRotation.CLOCKWISE_180 -> NMSRotation.CLOCKWISE_180
    BukkitRotation.COUNTERCLOCKWISE_90 -> NMSRotation.COUNTERCLOCKWISE_90
}

fun BukkitMirror.toNMS(): NMSMirror = when (this) {
    BukkitMirror.NONE -> NMSMirror.NONE
    BukkitMirror.FRONT_BACK -> NMSMirror.FRONT_BACK
    BukkitMirror.LEFT_RIGHT -> NMSMirror.LEFT_RIGHT
}


object NMS {
    val handler: NMSHandler = NMS_v1_20_R3_Handler()
    var logger: ContextLogger? = ContextLogger(ContextLogger.ContextType.REGISTRY, "NMS")
}
