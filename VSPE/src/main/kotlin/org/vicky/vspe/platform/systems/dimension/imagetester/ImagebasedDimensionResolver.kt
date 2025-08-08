package org.vicky.vspe.platform.systems.dimension.imagetester

import de.pauleff.api.ICompoundTag
import org.vicky.platform.utils.Vec3
import org.vicky.platform.world.PlatformBlock
import org.vicky.platform.world.PlatformBlockState
import org.vicky.platform.world.PlatformLocation
import org.vicky.platform.world.PlatformWorld
import org.vicky.vspe.BiomeCategory
import org.vicky.vspe.PrecipitationType
import java.awt.Color
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.File
import org.vicky.platform.utils.ResourceLocation
import org.vicky.platform.world.PlatformMaterial
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.BiomeHeightSampler
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.BiomeResolver
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.BiomeStructureData
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.ChunkData
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.ChunkGenerateContext
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.CompositeNoiseLayer
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.FBMGenerator
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.MultiParameterBiomeResolver
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.NoiseBiomeDistributionPaletteBuilder
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.NoiseSampler
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.Palette
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.PlatformChunkGenerator
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.PlatformDimension
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.PlatformStructure
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.RandomSource
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.SeededRandomSource
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.SimpleBlockState
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.SimpleConstructorBasedBiome
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.SimpleMaterial
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.SimpleNoiseHeightSampler
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.StructurePlacer
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.StructureRegistry
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.StructureRule
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.WeightedStructurePlacer
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import javax.swing.BorderFactory
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import kotlin.math.ceil

object ImageBasedWorld : PlatformWorld<String, String> {
    val blocks: MutableMap<Triple<Int, Int, Int>, ImagePlatformBlock> = mutableMapOf()
    val biomes: MutableMap<Pair<Int, Int>, SimpleConstructorBasedBiome> = mutableMapOf()

    fun setBiome(x: Int, z: Int, biome: SimpleConstructorBasedBiome) {
        biomes[x to z] = biome
    }

    fun getBiome(x: Int, z: Int): SimpleConstructorBasedBiome? {
        return biomes[x to z]
    }

    override fun getName(): String? {
        return "Image based Named"
    }
    override fun getNative(): String? = "test:image_based"

    override fun getHighestBlockYAt(x: Double, z: Double): Int = 64
    override fun getMaxWorldHeight(): Int = 64

    override fun getBlockAt(x: Double, y: Double, z: Double): PlatformBlock<String?>? {
        val key = Triple(x.toInt(), 64, z.toInt())
        return blocks[key] ?: ImagePlatformBlock.GRASS(Vec3(x, 64.0, z))
    }

    override fun getBlockAt(vec3: Vec3?): PlatformBlock<String?>? = blocks[Triple(vec3?.getX(), vec3?.getY(), vec3?.getZ())]

    override fun AIR(): PlatformBlockState<String?>? = SimpleBlockState.from<String>("test:air", { it })
    override fun TOP_LAYER_BLOCK(): PlatformBlockState<String?>? = SimpleBlockState.from("test:grass", { it })
    override fun STONE_LAYER_BLOCK(): PlatformBlockState<String?>? = SimpleBlockState.from("test:stone", { it })


    override fun setPlatformBlockState(position: Vec3?, state: PlatformBlockState<String?>?) {
        if (position != null && state != null) {
            val key = Triple(position.x.toInt(), 64, position.z.toInt())
            blocks[key] = ImagePlatformBlock(
                state.properties.getOrDefault("isWater", false) as Boolean,
                position,
                state.native.toString()
            )
        }
    }

    override fun setPlatformBlockState(
        vec3: Vec3?,
        platformBlockState: PlatformBlockState<String?>?,
        iCompoundTag: ICompoundTag?
    ) {
        if (vec3 != null) {
            blocks.put(Triple(vec3.getX(), vec3.getY(), vec3.getZ()), platformBlockState as ImagePlatformBlock)
        }
    }

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

open class ImagePlatformBlock(
    val isWater: Boolean = false,
    pos: Vec3,
    val name: String,
    val properties: String = ""
) : PlatformBlock<String?> {
    val locationZ = PlatformLocation(ImageBasedWorld, pos.x, 64.0, pos.z)
    class GRASS(positionZ: Vec3) : ImagePlatformBlock(
        false,
        positionZ,
        "grass"
    ) {
        override fun setBlockState(platformBlockState: PlatformBlockState<String?>?) {

        }
    }

    override fun isSolid(): Boolean = !isWater
    override fun getMaterial(): PlatformMaterial? = SimpleMaterial(ResourceLocation.from("test", name))

    override fun getLocation(): PlatformLocation? = locationZ
    override fun getBlockState(): PlatformBlockState<String?>? = SimpleBlockState.from("test:${name.lowercase()}${if (properties.isNotEmpty()) "[$properties]" else ""}", { it })
    override fun setBlockState(platformBlockState: PlatformBlockState<String?>?) {
        TODO("Not yet implemented")
    }
}

private const val size = 16

object ImageBasedChunkGenerator : PlatformChunkGenerator<String, SimpleConstructorBasedBiome> {
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
                val worldX = context.chunkX * size + x
                val worldZ = context.chunkZ * size + z
                val biome = context.dimension.biomeResolver.resolveBiome(worldX, 64, worldZ, context.dimension.random.getSeed())

                var block = if (biome.isOcean) {
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

fun SimpleConstructorBasedBiome.withChanges(
    name: String = this.name,
    biomeColor: Int = this.biomeColor,
    fogColor: Int = this.fogColor,
    waterColor: Int = this.waterColor,
    waterFogColor: Int = this.waterFogColor,
    isOcean: Boolean = this.isOcean,
    temperature: Double = this.temperature,
    humidity: Double = this.humidity,
    elevation: Double = this.elevation,
    rainfall: Double = this.rainfall,
    category: BiomeCategory = this.category,
    heightSampler: BiomeHeightSampler = this.heightSampler,
    precipitation: PrecipitationType = this.precipitation,
    biomeStructureData: BiomeStructureData = this.biomeStructureData,
    namespace: String = this.namespace
): SimpleConstructorBasedBiome {
    return SimpleConstructorBasedBiome(
        name = name,
        namespace = namespace,
        biomeColor = biomeColor,
        fogColor = fogColor,
        waterColor = waterColor,
        waterFogColor = waterFogColor,
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
            false,
            0.6,
            0.8,
            0.4,
            rainfall = 0.5,
            category = BiomeCategory.PLAINS,
            heightSampler = SimpleNoiseHeightSampler(CompositeNoiseLayer(listOf<Pair<NoiseSampler, Double>>(FBMGenerator(989829839283) to 3.0))),
            precipitation = PrecipitationType.RAIN,
            biomeStructureData = BiomeStructureData(ResourceLocation.from("test:test_paper_biome") , listOf())
        )
        val forest = plains.withChanges(
            name       = "forest",
            biomeColor    = 0x228B22,
            fogColor  = 0x006400,
            rainfall      = 0.6,
            category   = BiomeCategory.FOREST,
            elevation = 0.4,
            biomeStructureData = BiomeStructureData(ResourceLocation.from("test:forest"), emptyList())
        )
        val rainforest = plains.withChanges(
            name       = "forest",
            biomeColor    = 0x228B88,
            fogColor  = 0x006400,
            rainfall      = 1.0,
            category   = BiomeCategory.RAINFOREST,
            elevation = 0.45,
            biomeStructureData = BiomeStructureData(ResourceLocation.from("test:rainforest"), emptyList())
        )
        val jungle = forest.withChanges(
            name       = "jungle",
            biomeColor    = 0x006600,
            fogColor  = 0x006400,
            rainfall      = 0.7,
            category   = BiomeCategory.JUNGLE,
            elevation = 0.3,
            biomeStructureData = BiomeStructureData(ResourceLocation.from("test:jungle"), emptyList())
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
        return NoiseBiomeDistributionPaletteBuilder<SimpleConstructorBasedBiome>(
            CompositeNoiseLayer(listOf<Pair<NoiseSampler, Double>>(FBMGenerator(87263626382632, frequency = 0.01f * scale) to 1.0)),
            CompositeNoiseLayer(listOf<Pair<NoiseSampler, Double>>(FBMGenerator(87263626382632, frequency = 0.01f * scale) to 1.0)),
            CompositeNoiseLayer(listOf<Pair<NoiseSampler, Double>>(FBMGenerator(87263626382632, frequency = 0.01f * scale) to 1.0))
        )
            .add(0.000000000 to 0.1000, ocean)
            .add(0.1000  to 0.130, rainforest)
            .add(0.130 to 0.155, plains)
            .add(0.155  to 0.210, forest)
            .add(0.215  to 0.310, savanna)
            .add(0.310 to 0.375, desert)
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
            value += FBMGenerator(seed + it, octaves = 8).sample(x * frequency, z * frequency) * amplitude
            frequency *= 2.0
            amplitude *= 0.5
        }
        return value.coerceIn(-1.0, 1.0)
    }

    override fun resolveBiome(x: Int, y: Int, z: Int, seed: Long): SimpleConstructorBasedBiome {
        val freqX = 0.001
        val freqZ = 0.001
        val rawX  = FBMGenerator(seed).sample(x * freqX, z * freqX)
        val rawZ  = FBMGenerator(seed+1).sample(x * freqZ, z * freqZ)
        val normX = ((rawX + 1)/2).coerceIn(0.0, 1.0)
        val normZ = ((rawZ + 1)/2).coerceIn(0.0, 1.0)

        // calls your 2D lookup
        return getBiomePalette().get(normX, normZ)
    }

}

const val seedBase = 87263626382632L
class ImageBasedDimension(
    override val id: String = "test:image_dimension",
    override val world: PlatformWorld<String, *> = ImageBasedWorld,
    override val chunkGenerator: PlatformChunkGenerator<String, SimpleConstructorBasedBiome> = ImageBasedChunkGenerator,
    override val biomeResolver: BiomeResolver<SimpleConstructorBasedBiome> = /*ImageBiomeResolver*/ MultiParameterBiomeResolver<SimpleConstructorBasedBiome>(
        FBMGenerator(seedBase + 1, frequency = 0.45f),         // Very low frequency for large elevation zones
        FBMGenerator(seedBase + 2, frequency = 0.045f),       // Medium frequency
        FBMGenerator(seedBase + 3, frequency = 0.02f, lacunarity = 2.0f, gain = 0.5f) ,         // Slightly higher
        FBMGenerator(seedBase + 4, frequency = 0.07f),
        ImageBiomeResolver.getBiomePalette()
    ),
    override val structureRegistry: StructureRegistry<String> = StructureRegistry(mapOf<ResourceLocation, Pair<PlatformStructure<String>, StructureRule>>()),
    override val structurePlacer: StructurePlacer<String> = WeightedStructurePlacer(structureRegistry),
    override val random: RandomSource = SeededRandomSource(87263626382632)
) : PlatformDimension<String, SimpleConstructorBasedBiome>

const val imageSize = 512 // output image size
const val scale = 6      // how many blocks per pixel (change this to zoom in/out)\

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
            val context = object : ChunkGenerateContext<String, SimpleConstructorBasedBiome>(chunkX, chunkZ, dim) {}
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
}



fun renderImageBasedWorldToPNG(filename: String = "generated_world.png"): Pair<BufferedImage, Array<Array<SimpleConstructorBasedBiome?>>> {
    val biomeMap = Array(imageSize) { arrayOfNulls<SimpleConstructorBasedBiome>(imageSize) }
    val image = BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_RGB)
    val totalPixels = imageSize * imageSize
    val logEvery = totalPixels / 100  // 1% of total pixels
    var pixelsDone = 0

    val startTime = System.currentTimeMillis()

    for (x in 0 until imageSize) {
        for (z in 0 until imageSize) {
            // Sample a block from world position x * scale, z * scale
            val worldX = x * scale + scale / 2.0
            val worldZ = z * scale + scale / 2.0

            val biome = ImageBasedWorld.getBiome(worldX.toInt(), worldZ.toInt())
            val block = ImageBasedWorld.getBlockAt(worldX.toDouble(), 64.0, worldZ.toDouble())

            val native = block?.getBlockState()?.native ?: ""

            val color =  when {
                native.contains("grass") -> biome?.biomeColor?.let(::Color) ?: Color(0, 0, 0)
                native.contains("water") -> biome?.waterColor?.let(::Color) ?: Color(0, 191, 255)
                native.contains("dirt")  -> Color(139, 69, 19)
                native.contains("stone") -> Color(128, 128, 128)
                else -> Color(200, 200, 200)
            }
            biomeMap[x][z] = biome
            image.setRGB(x, z, color.rgb)
            pixelsDone++
            if (pixelsDone % logEvery == 0) {
                val percent = (pixelsDone * 100) / totalPixels
                val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                println("Progress Image: $percent% - Elapsed: ${"%.2f".format(elapsed)}s")
            }
        }
    }

    ImageIO.write(image, "png", File(filename))
    return image to biomeMap
}

fun showBiomeViewer(image: BufferedImage, biomeMap: Array<Array<SimpleConstructorBasedBiome?>>) {
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
                val biome = biomeMap[x][y]
                tooltip.text = biome?.name ?: "Unknown"
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


fun main() {
    generateImageTestWorld()
    val (image, biomeMap) = renderImageBasedWorldToPNG()
    println("Rendered image to generated_world.png")

    SwingUtilities.invokeLater {
        showBiomeViewer(image, biomeMap)
    }
}

