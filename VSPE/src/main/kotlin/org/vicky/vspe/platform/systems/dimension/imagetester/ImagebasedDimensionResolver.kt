package org.vicky.vspe.platform.systems.dimension.imagetester

import de.articdive.jnoise.api.NoiseGenerator
import de.articdive.jnoise.interpolation.Interpolation
import de.pauleff.api.ICompoundTag
import org.vicky.platform.PlatformPlayer
import org.vicky.platform.utils.ResourceLocation
import org.vicky.platform.utils.Vec3
import org.vicky.platform.world.*
import org.vicky.vspe.BiomeCategory
import org.vicky.vspe.PrecipitationType
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.*
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.*
import kotlin.math.abs
import kotlin.math.pow

object BlockRegistry {
    private val blockTypes = mutableListOf<ImagePlatformBlock>()

    fun register(block: ImagePlatformBlock): Int {
        // Return existing ID if identical type exists
        val existingId = blockTypes.indexOfFirst {
            it.name == block.name && it.properties == block.properties && it.isWater == block.isWater
        }
        if (existingId != -1) return existingId

        blockTypes.add(block)
        return blockTypes.size - 1
    }

    fun get(id: Int): ImagePlatformBlock = blockTypes[id]
}

object ImageBasedWorld : PlatformWorld<String, String> {
    val blocks: MutableMap<Triple<Int, Int, Int>, Int> = mutableMapOf() // stores block IDs
    val biomes: MutableMap<Pair<Int, Int>, Pair<Int, String>> = mutableMapOf()

    override fun getBlockAt(x: Double, y: Double, z: Double): PlatformBlock<String?>? {
        return null
    }

    override fun getBlockAt(vec: Vec3): PlatformBlock<String?>? {
        return null
    }

    override fun getAirBlockState(): PlatformBlockState<String?>? = SimpleBlockState.from<String>("test:air") { it }

    override fun getWaterBlockState(): PlatformBlockState<String?>? = SimpleBlockState.from<String>("test:water") { it }

    override fun setPlatformBlockState(position: Vec3?, state: PlatformBlockState<String?>?) {
        if (position == null || state == null) return
        val worldX = position.x.toInt()
        val worldZ = position.z.toInt()
        // choose block color fallback or use biome later
        val blockRgb = when {
            state.native.toString().contains("water") -> Color(0, 191, 255).rgb
            state.native.toString().contains("grass") -> Color(34,139,34).rgb
            state.native.toString().contains("stone") -> Color(128,128,128).rgb
            else -> Color(200,200,200).rgb
        }
        // We don't store per-block object forever; just push to flusher
        ChunkFlusher.add(worldX, worldZ, blockRgb, null)
    }

    override fun setPlatformBlockState(position: Vec3?, state: PlatformBlockState<String?>?, tag: ICompoundTag) {
        setPlatformBlockState(position, state)
    }

    fun setBiome(x: Int, z: Int, biome: SimpleConstructorBasedBiome) {
        val worldX = x
        val worldZ = z
        val biomeColorRgb = Color(biome.biomeColor).rgb
        ChunkFlusher.add(worldX, worldZ, null, biomeColorRgb to biome.name)
    }

    fun getBiome(x: Int, z: Int): Pair<Int, String>? = null

    override fun getName(): String? {
        return "Image based Named"
    }
    override fun getNative(): String? = "test:image_based"

    override fun getHighestBlockYAt(x: Double, z: Double): Int = 64
    override fun getMaxWorldHeight(): Int = 64
    override fun getPlayers(): List<PlatformPlayer?>? = listOf()

    override fun createPlatformBlockState(
        id: String?,
        properties: String?
    ): PlatformBlockState<String?>? = SimpleBlockState.from(id, properties, { it })

    override fun loadChunkIfNeeded(i: Int, i1: Int) {
        TODO("Not yet implemented")
    }

    override fun isChunkLoaded(i: Int, i1: Int): Boolean {
        TODO("Not yet implemented")
    }
}

object BlockCache {
    private val materials = mutableMapOf<String, PlatformMaterial>()
    private val states = mutableMapOf<String, PlatformBlockState<String?>>()

    fun material(name: String): PlatformMaterial =
        materials.getOrPut(name) {
            SimpleMaterial(ResourceLocation.from("test", name))
        }

    fun state(name: String, properties: String): PlatformBlockState<String?> {
        val key = "$name|$properties"
        return states.getOrPut(key) {
            SimpleBlockState.from(
                "test:${name.lowercase()}${if (properties.isNotEmpty()) "[$properties]" else ""}",
                { it }
            )
        }
    }
}

open class ImagePlatformBlock(
    val isWater: Boolean,
    val pos: Vec3, // This could be removed if the block itself doesn't store its position
    val name: String,
    val properties: String = ""
) : PlatformBlock<String?> {
    override fun isSolid(): Boolean = !isWater
    override fun getMaterial(): PlatformMaterial? = BlockCache.material(name)
    override fun getLocation(): PlatformLocation? = null // location is not stored in shared blocks
    override fun getBlockState(): PlatformBlockState<String?>? = BlockCache.state(name, properties)
    override fun setBlockState(platformBlockState: PlatformBlockState<String?>?) {}
}

object ImageBasedChunkGenerator : PlatformChunkGenerator<String, SimpleConstructorBasedBiome> {
    // val chunkCache = object : ChunkCache<Pair<Int, Int>, ChunkData<String, SimpleConstructorBasedBiome>>(512) {}
    override fun generateChunk(context: ChunkGenerateContext<String, SimpleConstructorBasedBiome>): ChunkData<String, SimpleConstructorBasedBiome> {
        val chunkData = object : ChunkData<String, SimpleConstructorBasedBiome> {
            val chunkX: Int = context.chunkX
            val chunkZ: Int = context.chunkZ

            override fun setBlock(x: Int, y: Int, z: Int, block: PlatformBlockState<String>) {
                val worldX = chunkX * size + x
                val worldZ = chunkZ * size + z
                ImageBasedWorld.setPlatformBlockState(Vec3(worldX.toDouble(), y.toDouble(), worldZ.toDouble()), block as PlatformBlockState<String?>)
            }

            override fun setBiome(x: Int, y: Int, z: Int, biome: SimpleConstructorBasedBiome) {
                val worldX = chunkX * size + x
                val worldZ = chunkZ * size + z
                ImageBasedWorld.setBiome(worldX, worldZ, biome)
            }
        }

        // Flat grass terrain
        for (x in 0 until size) {
            for (z in 0 until size) {
                val worldX = chunkData.chunkX * size + x
                val worldZ = chunkData.chunkZ * size + z
                val b = context.biomeResolver.resolveBiome(worldX, 64, worldZ, context.random.getSeed())
                val biome = biomeCache.getOrPut(b.name) { b }

                val block = if (biome.isOcean) {
                    SimpleBlockState.from("test:water", "", { it })
                } else {
                    SimpleBlockState.from("test:grass", "", { it })
                }
                chunkData.setBiome(x, 64, z, biome) // 👈 This line sets the biome!
                chunkData.setBlock(x, 64, z, block as PlatformBlockState<String>)
            }
        }

        return chunkData
    }

    override fun getBiome(x: Int, y: Int, z: Int): SimpleConstructorBasedBiome {
        TODO("Not yet implemented")
    }
}

val biomeCache: MutableMap<String, SimpleConstructorBasedBiome> = mutableMapOf()

fun SimpleConstructorBasedBiome.withChanges(
    name: String = this.name,
    biomeColor: Int = this.biomeColor,
    fogColor: Int = this.fogColor,
    waterColor: Int = this.waterColor,
    waterFogColor: Int = this.waterFogColor,
    foliageColor: Int = this.foliageColor,
    skyColor: Int = this.skyColor,
    isOcean: Boolean = this.isOcean,
    temperature: Double = this.temperature,
    humidity: Double = this.humidity,
    elevation: Double = this.elevation,
    rainfall: Double = this.rainfall,
    category: BiomeCategory = this.category,
    heightSampler: List<NoiseLayer> = this.heightSampler,
    precipitation: PrecipitationType = this.precipitation,
    biomeStructureData: BiomeStructureData = this.biomeStructureData,
    namespace: String = this.namespace
): SimpleConstructorBasedBiome {
    return biomeCache.getOrPut(name) {
        SimpleConstructorBasedBiome(
            name = name,
            namespace = namespace,
            biomeColor = biomeColor,
            fogColor = fogColor,
            waterColor = waterColor,
            waterFogColor = waterFogColor,
            foliageColor = foliageColor,
            skyColor = skyColor,
            isOcean = isOcean,
            temperature = temperature,
            humidity = humidity,
            elevation = elevation,
            rainfall = rainfall,
            category = category,
            heightSampler = heightSampler,
            precipitation = precipitation,
            biomeStructureData = biomeStructureData
        )
    }
}

object ImageBiomeResolver : BiomeResolver<SimpleConstructorBasedBiome> {
    override fun getBiomePalette(): Palette<SimpleConstructorBasedBiome> {
        // define your eight biomes up front
        val plains = SimpleConstructorBasedBiome(
            "plains",
            "test",
            0x88CC00,
            0x000000,
            0x00AAEE,
            0x00AAEE,
            0x00AAEE,
            0x00AAEE,
            false,
            0.6,
            0.8,
            0.4,
            rainfall = 0.5,
            category = BiomeCategory.PLAINS,
            heightSampler = listOf(),
            precipitation = PrecipitationType.RAIN,
            biomeStructureData = BiomeStructureData(listOf())
        )
        val forest = plains.withChanges(
            name       = "forest",
            biomeColor    = 0x228B22,
            fogColor  = 0x006400,
            rainfall      = 0.6,
            category   = BiomeCategory.FOREST,
            elevation = 0.4,
            biomeStructureData = BiomeStructureData(emptyList())
        )
        val rainforest = plains.withChanges(
            name       = "rainforest",
            biomeColor    = 0x228B88,
            fogColor  = 0x006400,
            rainfall      = 1.0,
            category   = BiomeCategory.RAINFOREST,
            elevation = 0.45,
            biomeStructureData = BiomeStructureData(emptyList())
        )
        val jungle = forest.withChanges(
            name       = "jungle",
            biomeColor    = 0x006600,
            fogColor  = 0x006400,
            rainfall      = 0.7,
            category   = BiomeCategory.JUNGLE,
            elevation = 0.3,
            biomeStructureData = BiomeStructureData(emptyList())
        )
        val taiga = forest.withChanges(
            name = "taiga",
            biomeColor = 0x01796F,
            fogColor = 0x006D5E,
            temperature = 0.3,
            rainfall = 0.3,
            elevation = 0.6,
            precipitation = PrecipitationType.SNOW,
            category = BiomeCategory.TAIGA // <-- Add this category to your enum if missing
        )

        val desert = plains.withChanges(
            name       = "desert",
            biomeColor    = 0xFAFAD2,
            fogColor  = 0xC2B280,
            waterColor    = 0xE0FFFF,
            waterFogColor = 0xE0FFFF,
            isOcean     = false,
            elevation = 0.3,
            precipitation = PrecipitationType.NONE,
            category    = BiomeCategory.DESERT,
            rainfall       = 0.0
        )

        val swamp = plains.withChanges(
            name       = "swamp",
            biomeColor    = 0x3F7942,
            fogColor  = 0x2E8B57,
            waterColor    = 0x617B64,
            waterFogColor = 0x324B3F,
            rainfall       = 0.7,
            elevation = 0.3,
            category    = BiomeCategory.SWAMP
        )

        val mountains = plains.withChanges(
            name       = "mountains",
            biomeColor    = 0xA9A9A9,
            fogColor  = 0x808080,
            rainfall       = 0.2,
            elevation = 0.9,
            category    = BiomeCategory.MOUNTAIN
        )

        val tundra = forest.withChanges(
            name       = "tundra",
            biomeColor    = 0x0000AA,
            fogColor  = 0x006D5E,
            temperature   = 0.3,
            rainfall = 0.3,
            elevation = 0.6,
            precipitation = PrecipitationType.SNOW,
            category    = BiomeCategory.TUNDRA
        )

        val snowy = taiga.withChanges(
            name       = "snowy",
            biomeColor    = 0xFFFFFF,
            fogColor  = 0xE0FFFF,
            isOcean     = false,
            rainfall = 0.4,
            elevation = 0.3,
            category    = BiomeCategory.ICY
        )

        val ocean = plains.withChanges(
            name       = "ocean",
            biomeColor    = 0x00AAFF,
            fogColor  = 0xE0FFFF,
            isOcean     = true,
            rainfall = 1.0,
            elevation = 0.0,
            category    = BiomeCategory.OCEAN
        )

        val deep = ocean.withChanges(
            name       = "deep",
            biomeColor    = 0x004488,
            fogColor  = 0xE0FFFF,
            isOcean     = true,
            rainfall = 1.0,
            elevation = 0.0,
            category    = BiomeCategory.DEEP_OCEAN
        )

        val mesa = plains.withChanges(
            name       = "mesa",
            biomeColor    = 0xCD853F,
            fogColor  = 0x8B4513,
            rainfall = 0.1,
            elevation = 0.7,
            precipitation = PrecipitationType.NONE,
            category    = BiomeCategory.MESA
        )

        val savanna = desert.withChanges(
            name       = "savanna",
            biomeColor    = 0xead4ad,
            fogColor  = 0x8B4513,
            rainfall = 0.2,
            elevation = 0.2,
            precipitation = PrecipitationType.NONE,
            category    = BiomeCategory.SAVANNA
        )

        val coast = desert.withChanges(
            name       = "coast",
            biomeColor    = 0xeadf93,
            fogColor  = 0x8B4513,
            rainfall = 0.2,
            elevation = 0.2,
            precipitation = PrecipitationType.NONE,
            category    = BiomeCategory.COAST
        )
        return NoiseBiomeDistributionPaletteBuilder<SimpleConstructorBasedBiome>(
            CompositeNoiseLayer(listOf<Pair<NoiseSampler, Double>>(FBMCache.get(87263626382632, frequency = 0.01f * scale) to 1.0)),
            CompositeNoiseLayer(listOf<Pair<NoiseSampler, Double>>(FBMCache.get(87263626382632, frequency = 0.01f * scale) to 1.0)),
            CompositeNoiseLayer(listOf<Pair<NoiseSampler, Double>>(FBMCache.get(87263626382632, frequency = 0.01f * scale) to 1.0))
        )
            .add(0.000000000 to 0.05, ocean)
            .add(0.05 to 0.1000, deep)
            .add(0.1000  to 0.130, rainforest)
            .add(0.130 to 0.155, plains)
            .add(0.155  to 0.210, forest)
            .add(0.215  to 0.310, savanna)
            .add(0.310 to 0.345, desert)
            .add(0.340 to 0.375, coast)
            .add(0.375  to 0.500, swamp)
            .add(0.500 to 0.600, mountains)
            .add(0.600 to 0.685, jungle)
            .add(0.685  to 0.720, taiga)
            .add(0.725  to 0.800, tundra)
            .add(0.800 to 0.875, snowy)
            .add(0.875 to 1.0000000, mesa)
            .build()
    }

    fun fbm(x: Double, z: Double, seed: Long): Double {
        var value = 0.0
        var amplitude = 3.0
        var frequency = 0.001
        repeat(4) {
            value += FBMCache.get(seed + it, octaves = 8).sample(x * frequency, z * frequency) * amplitude
            frequency *= 2.0
            amplitude *= 0.5
        }
        return value.coerceIn(-1.0, 1.0)
    }

    override fun resolveBiome(x: Int, y: Int, z: Int, seed: Long): SimpleConstructorBasedBiome {
        val freqX = 0.001
        val freqZ = 0.001
        val rawX  = FBMCache.get(seed).sample(x * freqX, z * freqX)
        val rawZ  = FBMCache.get(seed+1).sample(x * freqZ, z * freqZ)
        val normX = ((rawX + 1)/2).coerceIn(0.0, 1.0)
        val normZ = ((rawZ + 1)/2).coerceIn(0.0, 1.0)

        // calls your 2D lookup
        return getBiomePalette().get(normX, normZ)
    }

}

val continentNoise = NoiseSamplerFactory.create(
    NoiseSamplerFactory.Type.PERLIN,
    seed = seedBase,
    frequency = 0.000008, // ~1-2 big features per 1000 blocks
    octaves = 3,
    lacunarity = 2.0,
    gain = 2.0,
    fbm = true,
    interpolation = Interpolation.COSINE
)

val worldPx = imageSize * scale // or however big the world is
// val continents = ContinentGenerator(seedBase, 4096, 4096, targetCount = 5, minDist = 4096.0 / 3, radiusRange = 300.0 to 800.0)

// low-frequency "shape" noise (gives coastline roughness)
val baseShapeNoise = JNoiseNoiseSampler(NoiseSamplerFactory.create(
    NoiseSamplerFactory.Type.PERLIN,
    seed = seedBase,
    frequency = 0.000008, // ~1-2 big features per 1000 blocks
    octaves = 3,
    lacunarity = 2.0,
    gain = 2.0,
    fbm = true,
    interpolation = Interpolation.COSINE
))
val localElevationNoise = JNoiseNoiseSampler(NoiseSamplerFactory.create(
    NoiseSamplerFactory.Type.PERLIN,
    seed = seedBase,
    frequency = 0.000008, // ~1-2 big features per 1000 blocks
    octaves = 3,
    lacunarity = 2.0,
    gain = 2.0,
    fbm = true,
    interpolation = Interpolation.COSINE
))

/*
fun maskedElevation(x: Double, z: Double): Double {
    val shape = baseShapeNoise.sample(x, z)    // in -1..1
    val shapeNorm = (shape + 1.0) / 2.0         // 0..1
    val centerMask = continents.maskAt(x, z)    // 0..1 per-centre mask
    val mask = (shapeNorm * 0.6 + centerMask * 0.4).coerceIn(0.0, 1.0) // blend global + centers
    val elev = localElevationNoise.sample(x, z) // -1..1
    return mask * elev
}
 */

val breakupNoise = NoiseSamplerFactory.create(
    NoiseSamplerFactory.Type.PERLIN,
    seed = seedBase,
    frequency = 0.00009, // ~1 big feature every ~800 blocks
    octaves = 1,
    lacunarity = 3.0,
    gain = 3.0,
    fbm = true, // fractal to make shapes less circular        // no fractal for base shape
)

val detailedContinentNoise = CompositeNoiseLayer(
    listOf(
        JNoiseNoiseSampler(continentNoise) to 1.0, // big shapes
        //JNoiseNoiseSampler(breakupNoise) to 0.005    // breakup gaps
    )
)

val landBiomeResolver = MultiParameterBiomeResolver<SimpleConstructorBasedBiome>(
    FBMCache.get(seedBase + 1, frequency = 0.45f),         // Very low frequency for large elevation zones
    FBMCache.get(seedBase + 2, frequency = 0.045f),       // Medium frequency
    FBMCache.get(seedBase + 3, frequency = 0.025f) ,         // Slightly higher
    FBMCache.get(seedBase + 4, frequency = 0.07f),
    ImageBiomeResolver.getBiomePalette()
)

/*
val continentResolver = ContinentBiomeResolver(
    detailedContinentNoise,
    elevationNoise = object : NoiseSampler {
        override fun getSeed() = seedBase + 1
        override fun sample(x: Double, z: Double) = maskedElevation(x, z)
        override fun sample3D(x: Double, y: Double, z: Double) = maskedElevation(x, z)
    },
    landBiomeResolver,
    landThreshold = 0.45,   // just above mean = ~50% ocean
    deepSeaLevel = 0.05, // barely above min
    coastThreshold = 0.30,  // just above deep ocean
    mountainThreshold = 0.70 // near top of range  // mountains when elevation > 0.80
)
 */

const val seedBase = 87263626382632L

/*
class ImageBasedDimension(
    override val id: String = "test:image_dimension",
    override val world: PlatformWorld<String, *> = ImageBasedWorld,
    override val chunkGenerator: PlatformChunkGenerator<String, SimpleConstructorBasedBiome> = ImageBasedChunkGenerator,
    override val biomeResolver: BiomeResolver<SimpleConstructorBasedBiome> = /*ImageBiomeResolver*/ continentResolver,
    override val structurePlacer: StructurePlacer<String> = WeightedStructurePlacer(),
    override val random: RandomSource = SeededRandomSource(seedBase)
) : PlatformDimension<String, SimpleConstructorBasedBiome>
 */
const val imageSize = 256 // output image size
const val scale = 2      // how many blocks per pixel (change this to zoom in/out)\
const val size = 16

/*
fun generateImageTestWorld() {
    val dim = ImageBasedDimension()
    val worldSize = imageSize * scale
    val chunkCount = ceil(worldSize / 16.0).toInt()
    val total = chunkCount * chunkCount
    val mainStep = total / 10
    val miniStep = total / 100
    var done = 0
    val start = System.currentTimeMillis()

    for (chunkX in 0 until chunkCount) {
        for (chunkZ in 0 until chunkCount) {
            val context = object : ChunkGenerateContext<String, SimpleConstructorBasedBiome>(chunkX, chunkZ, b) {}
            dim.chunkGenerator.generateChunk(context)
            done++

            if (done % mainStep == 0) {
                val percent = (done * 100) / total
                val elapsed = System.currentTimeMillis() - start
                println("World gen: $percent% complete (${elapsed}ms)")
            } else if (done % miniStep == 0) {
                print(".")
            }
        }
    }

    val totalTime = System.currentTimeMillis() - start
    println("\nWorld generation completed in ${totalTime}ms")

    // make sure any buffered pixels are flushed and final PNG saved
    ChunkFlusher.finalizeAndSave()
}
*/

object FBMCache {
    private val cache = mutableMapOf<Long, FBMGenerator>()
    fun get(seed: Long, octaves: Int = 4, amplitude: Float = 1f, frequency: Float = 0.01f, lacunarity: Float = 2f, gain: Float = 0.5f): FBMGenerator {
        return cache.getOrPut(seed) {
            FBMGenerator(seed, octaves = octaves, amplitude = amplitude, frequency = frequency, lacunarity = lacunarity, gain = gain)
        }
    }
}
fun showBiomeViewer(image: BufferedImage, biomeMap: Array<Pair<Int, String>>) {
    val frame = JFrame("Biome Viewer")
    val label = JLabel(ImageIcon(image))
    val tooltip = JLabel("Hover over map", SwingConstants.CENTER)

    tooltip.font = Font("Monospaced", Font.PLAIN, 14)
    tooltip.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

    label.addMouseMotionListener(object : MouseMotionAdapter() {
        override fun mouseMoved(e: MouseEvent) {
            val x = e.x
            val y = e.y
            if (x in 0 until image.width && y in 0 until image.height) {
                val pixelColor = image.getRGB(x, y)

                val biome = biomeMap.firstOrNull { colorsAreClose(it.first, pixelColor) }
                tooltip.text = biome?.second ?: "Unknown"
            }
        }
    })
    label.size = Dimension(1024, 1024)

    val panel = JPanel(BorderLayout())
    panel.add(label, BorderLayout.CENTER)
    panel.add(tooltip, BorderLayout.SOUTH)

    frame.contentPane = panel
    frame.setSize(image.width + 20, image.height + 60)
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.isVisible = true
}

fun colorsAreClose(c1: Int, c2: Int, tolerance: Int = 10): Boolean {
    val r1 = (c1 shr 16) and 0xFF
    val g1 = (c1 shr 8) and 0xFF
    val b1 = c1 and 0xFF

    val r2 = (c2 shr 16) and 0xFF
    val g2 = (c2 shr 8) and 0xFF
    val b2 = c2 and 0xFF

    return (abs(r1 - r2) < tolerance &&
            abs(g1 - g2) < tolerance &&
            abs(b1 - b2) < tolerance)
}

fun main() {
    /*
    val c = continents.centers.first()
    println("DEBUG center = $c")
    println("mask at center = ${continents.maskAt(c.x, c.z)}")
    println("mask at radius*0.9 = ${continents.maskAt(c.x + c.radius*0.9, c.z)}")
    println("mask at radius*1.1 = ${continents.maskAt(c.x + c.radius*1.1, c.z)}")
    for (i in -5..5) {
        for (j in -5..5) {
            val v = (detailedContinentNoise.sample(i * 100.0, j * 100.0) + 1) / 2.0
            print("%.2f ".format(v))
        }
        println()
    }
    debugNoiseStats(detailedContinentNoise)
    debugNoiseStats(object : NoiseSampler {
        override fun getSeed() = seedBase + 1
        override fun sample(x: Double, z: Double) = maskedElevation(x, z)
        override fun sample3D(x: Double, y: Double, z: Double) = maskedElevation(x, z)
    })

    saveChannelImage(256, 256, scale,
        { x,z -> CompositeNoiseLayer(listOf(
            JNoiseNoiseSampler(NoiseSamplerFactory.create(
                NoiseSamplerFactory.Type.PERLIN,
                { b ->
                    b.addModule(
                        AdditionModuleBuilder.newBuilder()
                            .withSecondary(
                                NoiseSamplerFactory.create(
                                    NoiseSamplerFactory.Type.PERLIN,
                                    { it },
                                    0L,
                                    0.03,
                                    2,
                                    2.0
                                )
                            )
                            .build()
                    )
                    b
                },
                0L,
                0.005,
                4,
                3.0
            )) to 0.25,
            JNoiseNoiseSampler(NoiseSamplerFactory.create(
                NoiseSamplerFactory.Type.PERLIN,
                { b ->
                    b.addModule(
                        AdditionModuleBuilder.newBuilder()
                            .withSecondary(
                                NoiseSamplerFactory.create(
                                    NoiseSamplerFactory.Type.PERLIN,
                                    { it },
                                    0L,
                                    0.07,
                                    2,
                                    2.0
                                )
                            )
                            .build()
                    )
                    b
                },
                0L,
                0.01,
                2,
                5.0
            )) to 0.25,
            JNoiseNoiseSampler(NoiseSamplerFactory.create(
                NoiseSamplerFactory.Type.PERLIN,
                { b ->
                    b.addModule(
                        AdditionModuleBuilder.newBuilder()
                            .withSecondary(
                                NoiseSamplerFactory.create(
                                    NoiseSamplerFactory.Type.PERLIN,
                                    { it },
                                    0L,
                                    0.3,
                                    2,
                                    2.0
                                )
                            )
                            .build()
                    )
                    b
                },
                0L,
                0.001,
                4,
                3.4
            )) to 0.25,
            JNoiseNoiseSampler(NoiseSamplerFactory.create(
                NoiseSamplerFactory.Type.PERLIN,
                { b ->
                    b.addModule(
                        AdditionModuleBuilder.newBuilder()
                            .withSecondary(
                                NoiseSamplerFactory.create(
                                    NoiseSamplerFactory.Type.PERLIN,
                                    { it },
                                    0L,
                                    0.005,
                                    2,
                                    2.0
                                )
                            )
                            .build()
                    )
                    b
                },
                0L,
                0.01,
                2,
                1.5
            )) to 0.25
        )).sample(x,z) }, "debug_continent.png")
    /*
    saveChannelImage(256, 256, scale,
        { x,z -> FBMCache.get(seedBase + 1).sample(x*0.005, z*0.005) }, "debug_temp.png")
     */
    /*
    saveChannelImage(4096, 4096, 1,
        { x, z -> JNoiseNoiseSampler(NoiseSamplerFactory.create(
            NoiseSamplerFactory.Type.PERLIN,
            { b ->
                b.addModule(
                    AdditionModuleBuilder.newBuilder()
                        .withSecondary(
                            NoiseSamplerFactory.create(
                                NoiseSamplerFactory.Type.OPEN_SIMPLEX,
                                { it },
                                frequency = 0.0003,
                                octaves = 2,
                                gain = 2.0,
                                fbm = false,
                                ridged = true,
                                interpolation = Interpolation.COSINE
                            )
                        )
                        .build()
                )
                b
            },
            frequency = 0.0009,
            gain = 3.0,
            fbm = false,
            ridged = true,
            interpolation = Interpolation.COSINE
        )).sample(x, z) }, "debug_continents.png")
     */
    /*
    generateImageTestWorld()
    val image = ImageIO.read(File("generated_world.png"))
    // you still need a biomeMap (or pass an empty/placeholder map) — e.g.:
    val biomeMap: Array<Pair<Int, String>> = biomeCache.map {
        it.value.biomeColor to it.key
    }.toTypedArray()


    SwingUtilities.invokeLater {
        showBiomeViewer(image, biomeMap)
    }
     */
     */
    fun printChunkGrid(heights: IntArray, chunkSize: Int = 16) {
        require(heights.size == chunkSize * chunkSize) { "Array size must be chunkSize^2" }

        for (z in 0 until chunkSize) {
            val row = (0 until chunkSize).joinToString(" ") { x ->
                "%3d".format(heights[z * chunkSize + x])
            }
            println("[ $row ]")
        }
    }

    /*
    val provider =
        ChunkHeightProvider(BiomeResolvers.BiomeDetailHolder.MAGENTA_FOREST.heightSampler.buildSampler().getLayers())
    val heights = provider.getChunkHeights(0, 0)
    val heights1 = provider.getChunkHeights(5, 7)
    val heights2 = provider.getChunkHeights(64, 67)
    printChunkGrid(heights)
    println()
    printChunkGrid(heights1)
    println()
    printChunkGrid(heights2)
    println()

     */
}

/**
 * Ridged multifractal derived from a base sampler.
 * Expects sampler.sample2(x,y) in [-1,1].
 * Returns value roughly in [0,1].
 */
fun ridgedMultifractal(
    sampler: JNoiseNoiseSampler,
    x: Double,
    y: Double,
    octaves: Int = 6,
    lacunarity: Double = 2.0,
    gain: Double = 0.5,
    offset: Double = 1.0 // controls ridge sharpness / baseline
): Double {
    var frequency = 1.0
    var amplitude = 1.0
    var weight = 1.0
    var sum = 0.0
    var maxSum = 0.0

    for (i in 0 until octaves) {
        // sample and map -1..1 -> -1..1 (assumption), then apply ridge transform
        val n = sampler.sample(x * frequency, y * frequency) // [-1,1]
        var signal = 1.0 - abs(n)                // ridge base (0..1)
        signal = signal.coerceAtLeast(0.0)
        // square to sharpen
        signal *= signal
        // apply weight to create ridged multifractal effect
        signal *= weight
        // accumulate
        sum += signal * amplitude
        maxSum += amplitude

        // update weight and frequency/amplitude for next octave
        weight = (signal * gain).coerceIn(0.0, 1.0)
        frequency *= lacunarity
        amplitude *= gain
    }

    // normalize to 0..1
    return if (maxSum > 0.0) (sum / maxSum).coerceIn(0.0, 1.0) else 0.0
}

fun terrace(value: Double, steps: Int): Double {
    if (steps <= 1) return value
    val t = (value * steps).toInt().toDouble() / steps
    // smooth between steps:
    return lerp(t, (t + 1.0/steps), ((value * steps) % 1.0))
}

fun lerp(a: Double, b: Double, t: Double): Double {
    return a + (b - a) * t
}


fun domainWarp(
    warpX: JNoiseNoiseSampler,
    warpY: JNoiseNoiseSampler,
    x: Double, y: Double,
    warpAmp: Double = 10.0,
    warpFreq: Double = 0.005
): Pair<Double, Double> {
    val wx = warpX.sample(x * warpFreq, y * warpFreq) // [-1,1]
    val wy = warpY.sample(x * warpFreq, y * warpFreq)
    val nx = x + wx * warpAmp
    val ny = y + wy * warpAmp
    return nx to ny
}


fun sampleMountainHeight(
    x: Double, y: Double,
    baseSampler: JNoiseNoiseSampler,
    ridgeSampler: JNoiseNoiseSampler,    // usually same basePerlin or a different perlin for ridge
    warpX: JNoiseNoiseSampler,
    warpY: JNoiseNoiseSampler,
    erosionSampler: JNoiseNoiseSampler,
    voronoiSampler: JNoiseNoiseSampler,
    worldScale: Double = 1.0
): Double {
    // domain warp coordinates for jaggedness
    val (wx, wy) = domainWarp(warpX, warpY, x, y, warpAmp = 30.0, warpFreq = 0.002)

    // base mountain silhouette (low frequency)
    val base = (baseSampler.sample(wx * 0.5, wy * 0.5) + 1.0) * 0.5 // normalize to 0..1

    // ridged detail (higher frequency)
    val ridge = ridgedMultifractal(ridgeSampler, wx, wy, octaves = 6, lacunarity = 2.2, gain = 0.5)

    // erosion: higher-frequency noise used to subtract material from peaks
    val erosion = (erosionSampler.sample(x * 2.0, y * 2.0) + 1.0) * 0.5

    // voronoi for blockiness (small influence)
    val block = (voronoiSampler.sample(x * 1.2, y * 1.2) + 1.0) * 0.5

    // compose: base + ridge * strength - erosion * factor + block * tinyFactor
    var height = base * 0.7 + ridge * 0.8
    height -= erosion * 0.25
    height += block * 0.06

    // push peaks: apply power to exaggerate high values
    height = height.coerceIn(0.0, 1.0).pow(0.75) // <1 lifts peaks a bit

    return height.coerceIn(0.0, 1.0) * worldScale
}

val baseNoise = NoiseSamplerFactory.create(
    NoiseSamplerFactory.Type.OPEN_SIMPLEX,
    { it ->
        it
    },
    seed = seedBase,
    frequency = 0.0005,  // big features (ok)
    octaves = 2,
    gain = 4.5,         // <<<<< was 6.0 — keep < 1
    lacunarity = 0.5    // <<<<< was 0.05 — should be > 1
)

// Erosion: higher-frequency details that carve peaks (note freq > base)
val erosionNoise = NoiseSamplerFactory.create(
    NoiseSamplerFactory.Type.PERLIN,
    seed = seedBase + 1,
    frequency = 0.02,   // higher than base (finer detail)
    octaves = 3,
    gain = 0.6,
    lacunarity = 2.0
)

// Voronoi for blockiness / cell detail
val blockNoise = NoiseSamplerFactory.create(
    NoiseSamplerFactory.Type.VORONOI,
    seed = seedBase + 2,
    frequency = 0.06,   // small tweak, experiment 0.03..0.1
    octaves = 1
)

fun debugNoiseStats(noise: NoiseSampler, cx: Int = 0, cz: Int = 0, step: Int = 100, count: Int = 40) {
    val samples = mutableListOf<Double>()
    for (i in 0 until count) {
        for (j in 0 until count) {
            val x = (cx + i) * step.toDouble()
            val z = (cz + j) * step.toDouble()
            val v = (noise.sample(x, z) + 1.0) / 2.0
            samples += v
        }
    }
    val min = samples.minOrNull() ?: 0.0
    val max = samples.maxOrNull() ?: 0.0
    val mean = samples.average()
    println("Noise stats -> min=%.3f max=%.3f mean=%.3f".format(min, max, mean))
}

object ChunkFlusher {
    private const val IMAGE_FILENAME = "generated_world.png"
    private const val FLUSH_THRESHOLD = 256     // flush when buffered pixels >= this (tune)
    private const val imageWidth = imageSize
    private const val imageHeight = imageSize

    // Buffered image kept in memory — small and OK (imageSize x imageSize)
    private val image = BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB)

    // buffer keyed by pixel coords (px, pz) -> Pair(blockColorInt, biomeName)
    private val buffer = mutableMapOf<Pair<Int,Int>, Pair<Int, String?>>()

    // small cache to avoid new Color() allocations
    private val colorCache = mutableMapOf<Int, Int>() // biomeColor -> rgb int

    private val lock = Any()

    // map world coords -> pixel coords
    private fun worldToPixel(wx: Int, wz: Int): Pair<Int, Int>? {
        val px = wx / scale
        val pz = wz / scale
        if (px !in 0 until imageWidth || pz !in 0 until imageHeight) return null
        return px to pz
    }

    private fun colorIntFromBiome(biomeColor: Int): Int =
        colorCache.getOrPut(biomeColor) { Color(biomeColor).rgb }

    /**
     * Add a block/biome write to the buffer. Will flush automatically when threshold is exceeded.
     * - worldX/worldZ are absolute block coords
     * - blockColor is the rgb int for the block (or null if you only want biome color)
     * - biomePair is Pair(biomeColorInt, biomeName) or null
     */
    fun add(worldX: Int, worldZ: Int, blockColor: Int?, biomePair: Pair<Int, String>?) {
        val pxpz = worldToPixel(worldX, worldZ) ?: return
        synchronized(lock) {
            // prefer biome color for the pixel (you can change preference)
            val color = biomePair?.first ?: blockColor ?: 0xC8C8C8
            // store RGB int and biome name (small string)
            buffer[pxpz] = color to biomePair?.second
            if (buffer.size >= FLUSH_THRESHOLD) {
                flushToImage(saveToDisk = true)
            }
        }
    }

    /**
     * Force-flush buffered pixels into the image and optionally write PNG to disk.
     * After this call buffer is cleared (so memory for those entries is freed).
     */
    fun flushToImage(saveToDisk: Boolean = false) {
        val snapshot: Map<Pair<Int,Int>, Pair<Int, String?>>
        synchronized(lock) {
            if (buffer.isEmpty()) return
            snapshot = HashMap(buffer) // copy out
            buffer.clear()
        }

        // paint into in-memory BufferedImage (fast)
        for ((pxpz, pair) in snapshot) {
            val (px, pz) = pxpz
            val (biomeColorInt, _) = pair
            // biomeColorInt is an integer rgb value from colorIntFromBiome OR direct
            image.setRGB(px, pz, biomeColorInt)
        }

        // optionally save a PNG snapshot (cheapish; can be disabled if disk I/O is the bottleneck)
        if (saveToDisk) {
            try {
                ImageIO.write(image, "png", File(IMAGE_FILENAME))
            } catch (ex: Exception) {
                println("ChunkFlusher: failed to write image snapshot: ${ex.message}")
            }
        }
    }

    /** call at end of generation to guarantee final image saved */
    fun finalizeAndSave() {
        flushToImage(saveToDisk = true)
    }
}
fun saveChannelImage(
    width: Int,
    height: Int,
    scale: Int,
    sampler: (Double, Double) -> Double,
    filename: String
) {
    val img = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

    val totalPixels = width * height
    var processedPixels = 0
    var nextPrint = 0.1 // 10% threshold
    var minVal = Double.POSITIVE_INFINITY
    var maxVal = Double.NEGATIVE_INFINITY

    for (px in 0 until width) {
        for (pz in 0 until height) {
            val worldX = px * scale + scale / 2
            val worldZ = pz * scale + scale / 2

            val raw = sampler(worldX.toDouble(), worldZ.toDouble()) // expected -1..1
            if (raw < minVal) minVal = raw
            if (raw > maxVal) maxVal = raw

            processedPixels++
            val progress = processedPixels.toDouble() / totalPixels

            if (progress >= nextPrint) {
                println("Progress: ${(progress * 100).toInt()}%, min=$minVal, max=$maxVal")
                nextPrint += 0.1
            }

            val n = ((raw + 1.0) / 2.0).coerceIn(0.0, 1.0)
            val gray = (n * 255).toInt()
            val rgb = (gray shl 16) or (gray shl 8) or gray
            img.setRGB(px, pz, rgb)
        }
    }

    println("Final min=$minVal, max=$maxVal")
    ImageIO.write(img, "png", File(filename))
}

val ridgedSampler: NoiseSampler = object : NoiseSampler {
    override fun getSeed(): Long = seedBase + 10L

    override fun sample(x: Double, z: Double): Double {
        // adjust parameters as you like
        return ridgedMultifractal(
            sampler = JNoiseNoiseSampler(baseNoise), // or pass the actual sampler instance
            x = x, y = z,
            octaves = 6,
            lacunarity = 2.2,
            gain = 0.5
        )
    }

    override fun sample3D(x: Double, y: Double, z: Double): Double {
        // Use y as extra input if you want time/height dependence — otherwise forward
        return sample(x, z)
    }
}

val mountainSampler: NoiseSampler = object : NoiseSampler {
    // seeds offset so each sampler remains independent (important!)
    override fun getSeed(): Long = seedBase + 100L

    override fun sample(x: Double, z: Double): Double {
        // domain warp
        val (wx, wz) = domainWarp(
            warpX = JNoiseNoiseSampler(createPerlin(seedBase + 20L, 0.002, 3, 0.8)),
            warpY = JNoiseNoiseSampler(createPerlin(seedBase + 21L, 0.002, 3, 0.8)),
            x = x, y = z,
            warpAmp = 30.0,
            warpFreq = 0.002
        )

        // sample composed mountain height (uses your helper)
        val height = sampleMountainHeight(
            x = wx, y = wz,
            baseSampler = JNoiseNoiseSampler(baseNoise),
            ridgeSampler = JNoiseNoiseSampler(baseNoise),   // could be separate sampler
            warpX = JNoiseNoiseSampler(createPerlin(seedBase + 22L, 0.002, 2, 0.7)),
            warpY = JNoiseNoiseSampler(createPerlin(seedBase + 23L, 0.002, 2, 0.7)),
            erosionSampler = JNoiseNoiseSampler(erosionNoise),
            voronoiSampler = JNoiseNoiseSampler(blockNoise),
            worldScale = scale * 1.0
        )

        return height
    }

    private fun createPerlin(
        lng: Long,
        freq: Double,
        i: Int,
        gain: Double
    ): NoiseGenerator<DefaultedNoiseResult> = NoiseSamplerFactory.create(
        NoiseSamplerFactory.Type.PERLIN,
        seed = lng,
        frequency = freq,
        gain = gain,
        octaves = i
    )

    override fun sample3D(x: Double, y: Double, z: Double): Double {
        // Option A: ignore y and sample 2D
        return sample(x, z)

        // Option B: if you want 4D behavior (animated/time-varying), you could:
        // return sampleMountainHeight(x = x, y = z, /* pass voronoiSampler.sample4(x,z,y,time) */)
    }
}
