package org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator

import org.vicky.vspe.noise.FastNoiseLite

interface NoiseSampler {
    fun getSeed(): Int
    fun sample(x: Double, z: Double): Double
}

interface NoiseLayer {
    fun sample(x: Double, z: Double): Double
}

class CompositeNoiseLayer(
    private val layers: List<Pair<NoiseSampler, Double>>
): NoiseLayer {
    override fun sample(x: Double, z: Double): Double {
        var total = 0.0
        var totalWeight = 0.0
        for ((layer, weight) in layers) {
            total += layer.sample(x, z) * weight
            totalWeight += weight
        }
        return total / totalWeight
    }
}


class FBMGenerator(seed: Long, octaves: Int = 4, amplitude: Float = 1.0f, frequency: Float = 0.01f, lacunarity: Float = 2.0f, gain: Float = 0.5f): NoiseSampler {
    private val noise = FastNoiseLite(seed.toInt()).apply {
        SetNoiseType(FastNoiseLite.NoiseType.Value)
        SetFractalType(FastNoiseLite.FractalType.FBm)
        SetFractalOctaves(octaves)
        SetFractalLacunarity(lacunarity)
        SetFractalGain(gain)
        SetDomainWarpAmp(amplitude)
        SetFrequency(frequency)
    }

    override fun getSeed(): Int {
        return noise.mSeed
    }
    override fun sample(x: Double, z: Double): Double = noise.GetNoise(x.toFloat(), z.toFloat()).toDouble()
}

class NoiseBiomeResolver<B: PlatformBiome>(
    private val palette: Palette<B>,
    private val noise: NoiseSampler
) : BiomeResolver<B> {
    override fun getBiomePalette(): Palette<B> {
        return palette
    }

    override fun resolveBiome(x: Int, y: Int, z: Int, seed: Long): B {
        val nx = x / 512.0
        val nz = z / 512.0
        val value = noise.sample(nx, nz)
        return getBiomePalette().get(value, null)
    }
}

class SimpleNoiseHeightSampler(
    val noise: NoiseLayer,
    val elevationOffset: Double = 0.0
) : BiomeHeightSampler {
    override fun getHeight(x: Double, z: Double): Double {
        return noise.sample(x, z) + elevationOffset
    }
}
