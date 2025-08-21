package org.vicky.vspe.nms

import com.comphenix.protocol.collections.IntegerMap
import io.papermc.paper.registry.RegistryKey
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Biome
import org.bukkit.block.Block
import org.bukkit.block.structure.Mirror
import org.vicky.vspe.nms.utils.EnumUtil
import java.util.*
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
    fun getRegistry(): Registry<net.minecraft.world.level.biome.Biome>
}

interface BiomeWrapper {

    fun getResource(): ResourceKey<net.minecraft.world.level.biome.Biome>?

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

class SpecialEffectsBuilder {

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

    private fun deserializeColor(rgb: Int): String {
        val color = Color.fromRGB(rgb)
        return "${color.red}-${color.green}-${color.blue}"
    }
}

class BiomeRegistry private constructor() {

    private val map: MutableMap<NamespacedKey, BiomeWrapper> = LinkedHashMap()
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
    }

    fun remove(key: NamespacedKey): BiomeWrapper? {
        val removed = map.remove(key)
        if (removed != null) {
            removedBiomes.add(key)
        }
        return removed
    }

    operator fun get(key: NamespacedKey): BiomeWrapper? = map[key]

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
