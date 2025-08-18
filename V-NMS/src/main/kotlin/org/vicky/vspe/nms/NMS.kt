package org.vicky.vspe.nms

import com.comphenix.protocol.collections.IntegerMap
import me.deecaad.core.compatibility.CompatibilitySetup
import me.deecaad.core.file.SerializeData
import me.deecaad.core.file.Serializer
import me.deecaad.core.file.serializers.ColorSerializer
import me.deecaad.core.utils.EnumUtil
import net.minecraft.server.level.ServerLevel
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Biome
import org.bukkit.block.Block
import org.bukkit.block.structure.Mirror
import org.bukkit.configuration.ConfigurationSection
import org.spongepowered.configurate.yaml.internal.snakeyaml.serializer.SerializerException
import java.util.*
import javax.annotation.Nonnull
import kotlin.Throws
import org.bukkit.block.structure.StructureRotation as BukkitRotation

/**
 * Base, version-agnostic NMS API. Implementations per-MC-version will implement the real logic.
 */
abstract class NMSHandler {
    abstract fun placeStructure(
        location: Location,
        structureName: String,
        namespace: String,
        lootTable: String? = null,
        rotation: BukkitRotation = BukkitRotation.NONE,
        mirror: Mirror = Mirror.NONE,
        replacementPalette: Map<Material, PaletteFunction> = emptyMap()
    )

    abstract fun placeJigsawStructure(
        location: Location,
        poolKey: String,
        namespace: String,
        level: ServerLevel,
        lootTable: String? = null,
        rotation: BukkitRotation = BukkitRotation.NONE
    )

    abstract fun getBiomeCompatibility(): BiomeCompatibility

    /** Optional lifecycle hooks implementations may override (no-op by default). */
    open fun onEnable() {
    }

    open fun onDisable() {}
}


interface BiomeCompatibility {
    fun createBiome(key: NamespacedKey, base: BiomeWrapper): BiomeWrapper
    fun getBiomeAt(block: Block): BiomeWrapper?
}

interface BiomeWrapper {

    /**
     * Resets this biome's settings to its default values.
     * For custom biomes, this resets the biome to the base biome's values.
     */
    fun reset()

    /**
     * Gets the "base" biome used for default values.
     */
    fun getBase(): Biome

    /**
     * Sets the "base" biome used for default values.
     * Make sure to call [reset] for the changes to take effect.
     */
    fun setBase(biome: Biome)

    /**
     * Returns the key for this biome.
     */
    fun getKey(): NamespacedKey

    /**
     * Returns the unique numerical ID for this biome.
     * This value is not guaranteed to be the same across server restarts.
     */
    fun getId(): Int

    /**
     * Returns the name of this biome. Biome names *may* not be unique.
     */
    fun getName(): String = getKey().key

    /**
     * Gets the special effects of this biome.
     * The returned effects are cloned, so make sure to use [setSpecialEffects] after changes.
     */
    fun getSpecialEffects(): SpecialEffectsBuilder

    /**
     * Sets the special effects of this biome.
     */
    fun setSpecialEffects(builder: SpecialEffectsBuilder)

    /**
     * Sets this biome wrapper to the given block.
     *
     * @return true if the biome was changed.
     */
    fun setBiome(block: Block): Boolean

    /**
     * Registers this biome wrapper.
     */
    fun register(isCustom: Boolean)

    /**
     * If this wraps a vanilla biome, return it. Otherwise returns [Biome.CUSTOM].
     */
    fun getBukkitBiome(): Biome {
        return if (getKey().namespace != NamespacedKey.MINECRAFT) {
            Biome.CUSTOM
        } else {
            EnumUtil.getIfPresent(Biome::class.java, getName()).orElse(Biome.CUSTOM)
        }
    }

    /**
     * Returns true if this biome wraps a custom biome.
     */
    fun isCustom(): Boolean = getBukkitBiome() == Biome.CUSTOM

    /**
     * Returns true if this biome wraps a custom biome registered by another plugin.
     */
    fun isExternalPlugin(): Boolean

    /**
     * Returns true if this biome has been modified from its default state.
     * For custom biomes, always true.
     */
    fun isDirty(): Boolean

    companion object {
        @JvmStatic
        @Throws(SerializerException::class)
        fun serialize(data: SerializeData): BiomeWrapper? {
            // Make sure we have initialized the compatibility layer.
            BiomeCompatibilityAPI.getBiomeCompatibility()

            val keyStr: String = data.of("Key").assertExists().get()
            val isCustom = data.of("Custom").assertExists().bool
            val specialEffects =
                data.of("Special_Effects").assertExists().serialize(SpecialEffectsBuilder::class.java)!!
            val base = data.of("Base").assertExists().getEnum(Biome::class.java)

            val key = NamespacedKey.fromString(keyStr)!!

            val baseWrapper = BiomeRegistry.instance.getBukkit(base)
            val wrapper = BiomeCompatibilityAPI.getBiomeCompatibility().createBiome(key, baseWrapper)
            wrapper.setSpecialEffects(specialEffects)

            if (isCustom) wrapper.register(true)

            return wrapper
        }
    }

    fun deserialize(config: ConfigurationSection) {
        config["Key"] = getKey().toString()
        config["Custom"] = isCustom()
        getSpecialEffects().deserialize(config.createSection("Special_Effects"))
        config["Base"] = getBase().name
    }
}

class BiomeCompatibilityAPI {
    companion object {
        private val BIOME_COMPATIBILITY = CompatibilitySetup()
            .getCompatibleVersion(BiomeCompatibility::class.java, "org.vicky.vspe.nms.impl")

        fun getBiomeCompatibility(): BiomeCompatibility {
            return BIOME_COMPATIBILITY!!
        }
    }
}

class SpecialEffectsBuilder : Serializer<SpecialEffectsBuilder> {

    /**
     * The data needed to store "ambient particles".
     */
    data class ParticleData(val particle: String?, val density: Float)

    /**
     * The data needed to store "cave sounds".
     */
    data class CaveSoundData(val sound: String?, val tickDelay: Int, val searchOffset: Int, val soundOffset: Double)

    /**
     * Plays a sound on the player every tick with probability [tickChance].
     */
    data class RandomSoundData(val sound: String?, val tickChance: Double)

    /**
     * Music playback settings.
     */
    data class MusicData(val sound: String?, val minDelay: Int, val maxDelay: Int, val isOverride: Boolean)


    private var fogColor: Int = 0
    private var waterColor: Int = 0
    private var waterFogColor: Int = 0
    private var skyColor: Int = 0
    private var foliageColorOverride: Int = -1
    private var grassColorOverride: Int = -1
    private var grassColorModifier: String = "NONE"
    private var particle: ParticleData = ParticleData(null, -1f)
    private var ambientSound: String? = null
    private var caveSoundSettings: CaveSoundData = CaveSoundData(null, -1, -1, -1.0)
    private var caveSound: RandomSoundData = RandomSoundData(null, -1.0)
    private var music: MusicData = MusicData(null, -1, -1, false)


    fun getFogColor(): Int = fogColor
    fun setFogColor(fogColor: Int) = apply { this.fogColor = fogColor }
    fun setFogColor(fogColor: Color) = apply { this.fogColor = fogColor.asRGB() }

    fun getWaterColor(): Int = waterColor
    fun setWaterColor(waterColor: Int) = apply { this.waterColor = waterColor }
    fun setWaterColor(waterColor: Color) = apply { this.waterColor = waterColor.asRGB() }

    fun getWaterFogColor(): Int = waterFogColor
    fun setWaterFogColor(waterFogColor: Int) = apply { this.waterFogColor = waterFogColor }
    fun setWaterFogColor(waterFogColor: Color) = apply { this.waterFogColor = waterFogColor.asRGB() }

    fun getSkyColor(): Int = skyColor
    fun setSkyColor(skyColor: Int) = apply { this.skyColor = skyColor }
    fun setSkyColor(skyColor: Color) = apply { this.skyColor = skyColor.asRGB() }

    fun getFoliageColorOverride(): Int = foliageColorOverride
    fun setFoliageColorOverride(foliageColorOverride: Int) = apply { this.foliageColorOverride = foliageColorOverride }
    fun setFoliageColorOverride(foliageColorOverride: Color) =
        apply { this.foliageColorOverride = foliageColorOverride.asRGB() }

    fun getGrassColorOverride(): Int = grassColorOverride
    fun setGrassColorOverride(grassColorOverride: Int) = apply { this.grassColorOverride = grassColorOverride }
    fun setGrassColorOverride(grassColorOverride: Color) =
        apply { this.grassColorOverride = grassColorOverride.asRGB() }

    fun getGrassColorModifier(): String = grassColorModifier
    fun setGrassColorModifier(grassColorModifier: String) = apply { this.grassColorModifier = grassColorModifier }

    fun getParticle(): ParticleData = particle
    fun setAmbientParticle(ambientParticle: String?) = apply {
        particle = ParticleData(ambientParticle, particle.density)
    }

    fun setParticleProbability(particleProbability: Float) = apply {
        particle = ParticleData(particle.particle, particleProbability)
    }

    fun getAmbientSound(): String? = ambientSound
    fun setAmbientSound(ambientSound: String?) {
        this.ambientSound = ambientSound
    }

    fun getCaveSoundSettings(): CaveSoundData = caveSoundSettings
    fun setCaveSound(sound: String?) = apply {
        caveSoundSettings = caveSoundSettings.copy(sound = sound)
    }

    fun setCaveTickDelay(tickDelay: Int) = apply {
        caveSoundSettings = caveSoundSettings.copy(tickDelay = tickDelay)
    }

    fun setCaveSearchDistance(searchDistance: Int) = apply {
        caveSoundSettings = caveSoundSettings.copy(searchOffset = searchDistance)
    }

    fun setCaveSoundOffset(soundOffset: Double) = apply {
        caveSoundSettings = caveSoundSettings.copy(soundOffset = soundOffset)
    }

    fun getRandomSound(): RandomSoundData = caveSound
    fun setRandomSound(sound: String?) = apply {
        caveSound = caveSound.copy(sound = sound)
    }

    fun setRandomTickChance(tickChance: Double) = apply {
        caveSound = caveSound.copy(tickChance = tickChance)
    }

    fun getMusic(): MusicData = music
    fun setMusicSound(sound: String?) = apply {
        music = music.copy(sound = sound)
    }

    fun setMusicMinDelay(minDelay: Int) = apply {
        music = music.copy(minDelay = minDelay)
    }

    fun setMusicMaxDelay(maxDelay: Int) = apply {
        music = music.copy(maxDelay = maxDelay)
    }

    fun setMusicOverride(isOverride: Boolean) = apply {
        music = music.copy(isOverride = isOverride)
    }

    @Nonnull
    @Throws(SerializerException::class)
    override fun serialize(data: SerializeData): SpecialEffectsBuilder {
        val fogColor = data.of("Fog_Color").assertExists().serialize(ColorSerializer())!!.color
        val waterColor = data.of("Water_Color").assertExists().serialize(ColorSerializer())!!.color
        val waterFogColor = data.of("Water_Fog_Color").assertExists().serialize(ColorSerializer())!!.color
        val skyColor = data.of("Sky_Color").assertExists().serialize(ColorSerializer())!!.color
        val foliageColor = data.of("Foliage_Color").serialize(ColorSerializer())
        val grassColor = data.of("Grass_Color").serialize(ColorSerializer())

        val builder = SpecialEffectsBuilder()
            .setFogColor(fogColor)
            .setWaterColor(waterColor)
            .setWaterFogColor(waterFogColor)
            .setSkyColor(skyColor)
            .setGrassColorModifier(data.of("Grass_Modifier").get("NONE")!!.trim().uppercase())

        if (foliageColor != null) {
            builder.setFoliageColorOverride(foliageColor.color)
        }
        if (grassColor != null) {
            builder.setGrassColorOverride(grassColor.color)
        }

        if (data.has("Particle")) {
            builder.setAmbientParticle(data.of("Particle.Type").assertExists().get())
            builder.setParticleProbability(data.of("Particle.Density").assertExists().double.toFloat())
        }

        builder.setAmbientSound(data.of("Ambient_Sound").get(null))

        if (data.has("Cave_Sound")) {
            builder.setCaveSound(data.of("Cave_Sound.Sound").assertExists().get())
            builder.setCaveTickDelay(data.of("Cave_Sound.Tick_Delay").assertExists().assertPositive().int)
            builder.setCaveSearchDistance(
                data.of("Cave_Sound.Search_Distance").assertExists().assertPositive().int
            )
            builder.setCaveSoundOffset(data.of("Cave_Sound.Sound_Offset").assertExists().assertPositive().double)
        }

        if (data.has("Random_Sound")) {
            builder.setRandomSound(data.of("Random_Sound.Sound").assertExists().get())
            builder.setRandomTickChance(data.of("Random_Sound.Tick_Chance").assertExists().assertPositive().double)
        }

        if (data.has("Music")) {
            builder.setMusicSound(data.of("Music.Sound").assertExists().get())
            builder.setMusicMinDelay(data.of("Music.Min_Delay").assertExists().assertPositive().int)
            builder.setMusicMaxDelay(data.of("Music.Max_Delay").assertExists().assertPositive().int)
            builder.setMusicOverride(data.of("Music.Override_Previous_Music").assertExists().bool)
        }

        return builder
    }

    fun deserialize(config: ConfigurationSection) {
        config["Fog_Color"] = deserializeColor(fogColor)
        config["Water_Color"] = deserializeColor(waterColor)
        config["Water_Fog_Color"] = deserializeColor(waterFogColor)
        config["Sky_Color"] = deserializeColor(skyColor)

        config["Foliage_Color"] = if (foliageColorOverride == -1) null else deserializeColor(foliageColorOverride)
        config["Grass_Color"] = if (grassColorOverride == -1) null else deserializeColor(grassColorOverride)
        config["Grass_Modifier"] = if (grassColorModifier == "NONE") null else grassColorModifier

        if (particle.particle != null) {
            config["Particle.Type"] = particle.particle
            config["Particle.Density"] = particle.density
        } else {
            config["Particle"] = null
        }

        config["Ambient_Sound"] = ambientSound

        if (caveSoundSettings.sound != null) {
            config["Cave_Sound.Sound"] = caveSoundSettings.sound
            config["Cave_Sound.Tick_Delay"] = caveSoundSettings.tickDelay
            config["Cave_Sound.Search_Distance"] = caveSoundSettings.searchOffset
            config["Cave_Sound.Sound_Offset"] = caveSoundSettings.soundOffset
        } else {
            config["Cave_Sound"] = null
        }

        if (caveSound.sound != null) {
            config["Random_Sound.Sound"] = caveSound.sound
            config["Random_Sound.Tick_Chance"] = caveSound.tickChance
        } else {
            config["Random_Sound"] = null
        }

        if (music.sound != null) {
            config["Music.Sound"] = music.sound
            config["Music.Min_Delay"] = music.minDelay
            config["Music.Max_Delay"] = music.maxDelay
            config["Music.Override_Previous_Music"] = music.isOverride
        } else {
            config["Music"] = null
        }
    }

    private fun deserializeColor(rgb: Int): String {
        val color = Color.fromRGB(rgb)
        return "${color.red}-${color.green}-${color.blue}"
    }
}

class BiomeRegistry private constructor() {

    private val map: MutableMap<NamespacedKey, BiomeWrapper> = LinkedHashMap()
    private val byId: IntegerMap<BiomeWrapper> = IntegerMap()
    private val removedBiomes: MutableSet<NamespacedKey> = HashSet()

    fun add(key: String, biome: BiomeWrapper) {
        add(NamespacedKey("vspe", key), biome)
    }

    fun add(key: NamespacedKey, biome: BiomeWrapper) {
        require(!map.containsKey(key)) { "Duplicate key '$key'" }
        require(!removedBiomes.contains(key)) {
            "Tried to add deleted biome '$key' without restarting the server"
        }

        map[key] = biome
        byId.put(biome.getId(), biome)
    }

    fun remove(key: NamespacedKey): BiomeWrapper? {
        val removed = map.remove(key)
        if (removed != null) {
            removedBiomes.add(key)
        }
        return removed
    }

    operator fun get(key: NamespacedKey): BiomeWrapper? = map[key]

    fun getById(id: Int): BiomeWrapper? = byId[id]

    fun getBukkit(biome: Biome?): BiomeWrapper {
        require(biome != Biome.CUSTOM) { "Cannot use Biome.CUSTOM" }

        return map[biome?.key]
            ?: throw IllegalStateException("BiomeManager failed to wrap vanilla biomes... map: $map")
    }

    fun getOrCreate(key: NamespacedKey): BiomeWrapper =
        getOrCreate(key, Biome.PLAINS)

    fun getOrCreate(key: NamespacedKey, base: Biome): BiomeWrapper =
        getOrCreate(key, getBukkit(base))

    fun getOrCreate(key: NamespacedKey, base: BiomeWrapper): BiomeWrapper {
        var biome = get(key)
        if (biome == null) {
            biome = BiomeCompatibilityAPI.getBiomeCompatibility().createBiome(key, base)
            add(key, biome)
        }
        return biome
    }

    fun getKeys(dirty: Boolean): Set<NamespacedKey> {
        return if (dirty) {
            map.filter { it.value.isDirty() }.keys.toCollection(LinkedHashSet())
        } else {
            Collections.unmodifiableSet(map.keys)
        }
    }

    companion object {
        @JvmStatic
        val instance: BiomeRegistry by lazy { BiomeRegistry() }
    }
}
