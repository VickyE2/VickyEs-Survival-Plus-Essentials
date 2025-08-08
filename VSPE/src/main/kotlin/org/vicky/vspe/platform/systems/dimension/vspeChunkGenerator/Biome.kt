package org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator

import org.vicky.utilities.Identifiable
import org.vicky.vspe.BiomeCategory
import org.vicky.vspe.PrecipitationType
import org.vicky.platform.utils.ResourceLocation

interface BiomeHeightSampler {
    fun getHeight(x: Double, z: Double): Double
}

interface PlatformBiome : Identifiable {
    val name: String
    val biomeColor: Int
    val fogColor: Int
    val waterColor: Int
    val waterFogColor: Int get() = waterColor
    val isOcean: Boolean
    val temperature: Double   // 0.0 - 1.0
    val humidity: Double      // 0.0 - 1.0
    val elevation: Double     // 0.0 - 1.0
    val rainfall: Double      // 0.0 - 1.0
    val category: BiomeCategory
    val heightSampler: BiomeHeightSampler
    val precipitation: PrecipitationType
        get() = PrecipitationType.RAIN
    val biomeStructureData: BiomeStructureData

    fun toNativeBiome(): Any
    fun isCold(): Boolean = temperature < 0.3
    fun isHumid(): Boolean = humidity > 0.7
    fun isMountainous(): Boolean = elevation > 0.6
}

class SimpleConstructorBasedBiome(
    override val name: String,
    val namespace: String,
    override val biomeColor: Int,
    override val fogColor: Int,
    override val waterColor: Int,
    override val waterFogColor: Int,
    override val isOcean: Boolean,
    override val temperature: Double,
    override val humidity: Double,
    override val elevation: Double,
    override val rainfall: Double,
    override val category: BiomeCategory,
    override val heightSampler: BiomeHeightSampler,
    override val precipitation: PrecipitationType,
    override val biomeStructureData: BiomeStructureData
): PlatformBiome {
    override fun toNativeBiome(): String {
        return "$namespace:${name.trim().lowercase()}"
    }
    override fun getIdentifier(): String? {
        return "$namespace:${name.trim().lowercase()}"
    }
}

data class BiomeStructureData(
    val biomeId: ResourceLocation,
    val structureKeys: List<ResourceLocation>
)
