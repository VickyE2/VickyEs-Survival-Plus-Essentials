package org.vicky.vspe.nms.impl

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.serialization.Lifecycle
import me.deecaad.core.utils.EnumUtil
import me.deecaad.core.utils.ReflectionUtil.*
import net.minecraft.commands.arguments.ParticleArgument
import net.minecraft.core.*
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData
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
import java.lang.reflect.Field
import java.util.*
import net.minecraft.world.level.block.Mirror as NMSMirror
import net.minecraft.world.level.block.Rotation as NMSRotation
import org.bukkit.block.structure.Mirror as BukkitMirror
import org.bukkit.block.structure.StructureRotation as BukkitRotation

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
        val resource = ResourceKey.create(biomes.key(), ResourceLocation(key.namespace, key.key))

        if (biomes !is WritableRegistry<*>) throw InternalError("$biomes was not a writable registry???")

        if (isCustom) {
            val freezeField = getField(MappedRegistry::class.java, Boolean::class.javaPrimitiveType!!)
            setField(freezeField, biomes, false)

            val intrusiveHoldersField = getField(MappedRegistry::class.java, "m")
            setField(intrusiveHoldersField, biomes, HashMap<Any, Any>())

            (biomes as WritableRegistry<Biome>).apply {
                createIntrusiveHolder(biome!!)
                register(resource, biome!!, Lifecycle.experimental())
            }

            setField(intrusiveHoldersField, biomes, null)
            setField(freezeField, biomes, true)
        }
        BiomeRegistry.instance.add(key, this)
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


class v1_20_R4 : BiomeCompatibility {

    companion object {
        private val chunkBiomesField =
            getField(ClientboundLevelChunkPacketData::class.java, ByteArray::class.java)
    }

    private val biomes: Registry<Biome> =
        MinecraftServer.getServer().registryAccess().registry(Registries.BIOME).orElseThrow()

    init {
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

    override fun getBiomeCompatibility(): BiomeCompatibility = v1_20_R4()
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
