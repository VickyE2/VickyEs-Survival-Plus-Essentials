package org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator

import de.pauleff.api.ICompoundTag
import de.pauleff.api.ITag
import de.pauleff.api.NBTFileFactory
import net.sandrohc.schematic4j.schematic.Schematic
import org.vicky.platform.utils.Mirror
import org.vicky.platform.utils.ResourceLocation
import org.vicky.platform.utils.Rotation
import org.vicky.platform.utils.Vec3
import org.vicky.platform.world.PlatformBlockState
import org.vicky.platform.world.PlatformLocation
import org.vicky.platform.world.PlatformMaterial
import org.vicky.platform.world.PlatformWorld
import org.vicky.vspe.*
import org.vicky.vspe.platform.VSPEPlatformPlugin
import org.vicky.vspe.platform.systems.dimension.StructureUtils.ProceduralStructureGenerator
import org.vicky.vspe.structure_gen.offset
import java.io.File
import kotlin.random.Random

// === Core Interfaces ===

interface PlatformStructure<T> {
    fun place(world: PlatformWorld<T, *>, origin: PlatformLocation, context: StructurePlacementContext): Boolean
    fun getSize(): BlockVec3i
}

class NbtStructure<T>(
    val nbtFile: File,
    val transformer: (String) -> T
) {
    val rootTag: ICompoundTag by lazy {
        val nbt = NBTFileFactory.readNBTFile(nbtFile)
        if (nbt !is ICompoundTag) error("Invalid NBT structure format")
        nbt
    }

    val size: BlockVec3i
        get() {
            val sizeTag = rootTag.getList("size") ?: error("Missing 'size' in structure")
            val x = (sizeTag.data[0] as ITag<Integer>).data
            val y = (sizeTag.data[1] as ITag<Integer>).data
            val z = (sizeTag.data[2] as ITag<Integer>).data
            return BlockVec3i(x, y, z)
        }

    val blocks: List<ICompoundTag>
        get() {
            val blockList = rootTag.getList("blocks").data ?: return emptyList()
            return blockList.mapNotNull { it as? ICompoundTag }
        }

    val palette: List<ICompoundTag>
        get() {
            val blockList = rootTag.getList("palette").data ?: return emptyList()
            return blockList.mapNotNull { it as? ICompoundTag }
        }



    val blockEntities: Map<BlockVec3i, ICompoundTag> by lazy {
        val entityList = rootTag.getList("entities")?.data ?: return@lazy emptyMap()

        buildMap {
            entityList.mapNotNull { it as? ICompoundTag }.forEach { entity ->
                val posList = entity.getList("blockPos")?.data
                if (posList != null && posList.size >= 3) {
                    val pos = BlockVec3i(
                        (posList[0] as ITag<Int>).data,
                        (posList[1] as ITag<Int>).data,
                        (posList[2] as ITag<Int>).data
                    )
                    put(pos, entity)
                }
            }
        }
    }

    fun resolveBlockState(stateIndex: Int): SimpleBlockState<T> {
        val paletteEntry = palette.getOrNull(stateIndex)
            ?: error("Palette index $stateIndex out of bounds")

        val name = paletteEntry.getString("Name")
        val propsTag = paletteEntry.getCompound("Properties")
        val properties = propsTag.data.joinToString(",") { tag ->
            "${tag.name}=${tag.data}"
        }

        return SimpleBlockState.from<T>(
            if (properties.isEmpty()) name else "$name[$properties]", transformer
        )
    }
}


// === Structure Implementations ===

class NBTBasedStructure<T>(
    val id: ResourceLocation
) : PlatformStructure<T>
{
    @Suppress("UNCHECKED_CAST")
    val structure : NbtStructure<T>? = VSPEPlatformPlugin.structureManager().getStructure(id) as NbtStructure<T>?

    @Suppress("unchecked")
    override fun place(world: PlatformWorld<T, *>, origin: PlatformLocation, context: StructurePlacementContext): Boolean {
        if (structure == null) {
            VSPEPlatformPlugin.platformLogger().warn("Unknown structure: Structure $id not found on manager")
            return false
        }

        structure.blocks.forEach { blockTag ->
            val posTag = blockTag.getList("pos") ?: return@forEach
            val state = blockTag.getInt("state")
            val nbt = blockTag.getCompound("nbt")

            val dx = (posTag.data[0] as ITag<Integer>).data
            val dy = (posTag.data[1] as ITag<Integer>).data
            val dz = (posTag.data[2] as ITag<Integer>).data

            val absolutePos = origin.offset(dx, dy, dz)
            val transformedPos = absolutePos
                .let { if (context.rotation != Rotation.NONE) it.rotate(context.rotation, origin) else it }
                .let { if (context.mirror != Mirror.NONE) it.mirror(context.mirror, origin) else it }

            world.setPlatformBlockState(transformedPos, structure.resolveBlockState(state), nbt)
        }

        /*structure.blockEntities.forEach { pos, tag ->
            world.setPlatformBlockEntityData(origin.offset(pos.x, pos.y, pos.z), tag.get)
        }*/
        return true
    }

    override fun getSize(): BlockVec3i = structure?.size ?: throw NullPointerException("Structure $id does not exist")
}

class SchematicStructure<BST>(
    private val schematic: Schematic,
    private val transformer: (String) -> BST
) : PlatformStructure<BST>
{
    override fun place(world: PlatformWorld<BST, *>, origin: PlatformLocation, context: StructurePlacementContext): Boolean {
        schematic.blocks().forEach { pair ->
            val position = pair.left()
            val blockData = pair.right()
            val absolutePos = Vec3(
                position.x + origin.x,
                position.y + origin.y,
                position.z + origin.z
            )
            val transformedPos = absolutePos
                .let { if (context.rotation != Rotation.NONE) it.rotate(context.rotation, origin) else it }
                .let { if (context.mirror != Mirror.NONE) it.mirror(context.mirror, origin) else it }

            world.setPlatformBlockState(transformedPos, SimpleBlockState.from<BST>(blockData.name(), transformer))
        }
        return true
    }

    override fun getSize(): BlockVec3i =
        BlockVec3i(schematic.length(), schematic.width(), schematic.height())
}

class ProceduralStructure<T : ProceduralStructureGenerator<BST, *>, BST>(
    private val generator: T
) : PlatformStructure<BST>
{

    override fun place(world: PlatformWorld<BST, *>, origin: PlatformLocation, context: StructurePlacementContext): Boolean {
        generator.generate(context.random, origin)
        return true
    }

    override fun getSize(): BlockVec3i = generator.getApproximateSize()
}

// === Structure Placement ===

data class StructurePlacementContext(
    val random: RandomSource,
    val rotation: Rotation,
    val mirror: Mirror
)

interface RandomSource {
    fun nextInt(bound: Int): Int
    fun nextInt(start: Int, bound: Int): Int
    fun nextDouble(): Double
    fun nextFloat(): Float
    fun nextBoolean(): Boolean
    fun nextLong(): Long
    fun getSeed(): Long
    fun fork(seedModifier: Long): RandomSource // useful for per-feature randomness
}

class SeededRandomSource(private val random: Random, private val seed: Long) : RandomSource {

    constructor(seed: Long) : this(Random(seed), seed)

    override fun nextInt(bound: Int): Int = random.nextInt(bound)
    override fun nextInt(start: Int, bound: Int): Int = random.nextInt(start, bound)

    override fun nextDouble(): Double = random.nextDouble()

    override fun nextFloat(): Float = random.nextFloat()

    override fun nextBoolean(): Boolean = random.nextBoolean()

    override fun nextLong(): Long = random.nextLong()
    override fun getSeed(): Long = seed

    override fun fork(seedModifier: Long): RandomSource {
        return SeededRandomSource(random.nextLong() xor seedModifier)
    }
}

// === Block State Handling ===

class SimpleBlockState<T> private constructor(
    private val blockData: T,
    private val id: String,
    private val properties: Map<String, String>
) : PlatformBlockState<T?> {

    companion object {
        fun <T> from(blockData: String, transform: (String) -> T): SimpleBlockState<T> {
            val trimmed = blockData.trim()
            val id = trimmed.substringBefore('[')
            val props = trimmed
                .substringAfter('[', "")
                .removeSuffix("]")
                .takeIf { it.isNotEmpty() }
                ?.split(',')
                ?.mapNotNull {
                    val (k, v) = it.split('=', limit = 2).map(String::trim)
                    if (k.isNotEmpty() && v.isNotEmpty()) k to v else null
                }?.toMap()
                ?: emptyMap()

            return SimpleBlockState<T>(transform(trimmed), id, props)
        }
        fun <T> from(id: String?, props: String?, transform: (String) -> T): SimpleBlockState<T> {
            if (id == null || props == null) return error("id and properties cannot be null")
            val trimmed = "$id:$props".trim()
            val props = props
                .substringAfter('[', "")
                .removeSuffix("]")
                .takeIf { it.isNotEmpty() }
                ?.split(',')
                ?.mapNotNull {
                    val (k, v) = it.split('=', limit = 2).map(String::trim)
                    if (k.isNotEmpty() && v.isNotEmpty()) k to v else null
                }?.toMap()
                ?: emptyMap()

            return SimpleBlockState<T>(transform(trimmed), id, props)
        }
    }

    override fun getId(): String = id
    override fun getMaterial(): PlatformMaterial? = SimpleMaterial(ResourceLocation.from(id.substringBefore(":"), id.substringAfter(":")))

    override fun getNative(): T = blockData
    override fun getProperties(): Map<String, String> = properties

    override fun <P : Any?> getProperty(name: String?): P? {
        return name?.let { properties[it] as? P }
    }
}

class SimpleMaterial(private val loc: ResourceLocation) : PlatformMaterial {
    override fun isSolid(): Boolean = true
    override fun isAir(): Boolean = loc.path == "air"
    override fun getResourceLocation(): ResourceLocation? = loc
}

data class StructureRule(
    val resource: ResourceLocation,
    val tags: Set<StructureTag>,
    val rotation: Rotation,
    val mirror: Mirror,
    val weight: Int,
    val frequency: Double,
    val spacing: Int
)

interface StructurePlacer<T> {
    fun placeStructuresInChunk(
        chunkX: Int,
        chunkZ: Int,
        context: ChunkGenerateContext<T, *>,
        chunkData: ChunkData<T, *>
    )
}

data class PlacedStructureCandidate(
    val rule: StructureRule,
    val position: BlockVec3i,
    val priority: Int, // Lower = earlier placement
    val weight: Int
)

data class StructureBox(val min: Vec3, val max: Vec3, val resource: ResourceLocation) {
    fun intersects(other: StructureBox): Boolean {
        return !(other.max.x < min.x || other.min.x > max.x ||
                other.max.y < min.y || other.min.y > max.y ||
                other.max.z < min.z || other.min.z > max.z)
    }
}


class WeightedStructurePlacer<T>(
    private val structureRegistry: StructureRegistry<T>
) : StructurePlacer<T>
{
    override fun placeStructuresInChunk(
        chunkX: Int,
        chunkZ: Int,
        context: ChunkGenerateContext<T, *>,
        chunkData: ChunkData<T, *>
    ) {
        val candidates = mutableListOf<PlacedStructureCandidate>()
        val structureBoxes = mutableListOf<StructureBox>()

        rule@for (rule in getAllStructureRules()) {
            // Check biome if needed
            val biome = context.dimension.biomeResolver.resolveBiome(chunkX * 16, 64, chunkZ * 16, context.dimension.random.getSeed())
            if (biome.biomeStructureData.structureKeys.contains { it.structureKey == rule.resource }) continue

            // Respect spacing
            if ((chunkX % rule.spacing != 0) || (chunkZ % rule.spacing != 0)) continue

            // Frequency check
            if (context.dimension.random.nextDouble() > rule.frequency) continue

            val origin = BlockVec3i(chunkX * 16, 64, chunkZ * 16)
            val structure = structureRegistry.structures[rule.resource]

            if (structure != null) {
                val structureBox =
                    computeStructureBox(
                        structure.first,
                        rule.resource,
                        origin.toVec3(),
                        rule.rotation,
                        rule.mirror
                    )
                if (structureBoxes.any { it.intersects(structureBox) }) {
                    VSPEPlatformPlugin.platformLogger().debug("Skipping ${rule.resource} due to overlap.")
                    continue@rule
                }
                candidates += PlacedStructureCandidate(
                    rule,
                    origin,
                    priority = getPlacementPriority(rule),
                    weight = rule.weight
                )
                structureBoxes.add(
                    structureBox
                )
            }
        }

        // Sort by priority
        candidates.sortedBy { it.priority }.forEach { candidate ->
            val structureRaw = structureRegistry.structures[candidate.rule.resource]
            if (structureRaw != null) {
                val structure = structureRaw.first

                val origin = PlatformLocation(context.dimension.world, candidate.position.x.toDouble(), candidate.position.y.toDouble(), candidate.position.z.toDouble())
                val success = structure.place(
                    context.dimension.world,
                    origin,
                    StructurePlacementContext(
                        random = context.dimension.random.fork(candidate.position.hashCode().toLong()),
                        rotation = candidate.rule.rotation,
                        mirror = candidate.rule.mirror
                    )
                )

                if (!success) {
                    VSPEPlatformPlugin.platformLogger().warn("Failed to place structure ${candidate.rule.resource}")
                }
            }
        }
    }

    private fun getAllStructureRules(): List<StructureRule> {
        return structureRegistry.structures.values.map { it.second }
    }

    private fun <T> computeStructureBox(
        structure: PlatformStructure<T>,
        resource: ResourceLocation,
        origin: Vec3,
        rotation: Rotation,
        mirror: Mirror
    ): StructureBox {
        val size = structure.getSize()
        val transformedSize = transformSize(origin, size.toVec3(), rotation, mirror)
        val min = origin
        val max = origin.offset((transformedSize.getX() - 1).toInt(), (transformedSize.getY() - 1).toInt(), (transformedSize.getZ() - 1).toInt())
        return StructureBox(min, max, resource)
    }

    private fun transformSize(origin: Vec3, size: Vec3, rotation: Rotation, mirror: Mirror): Vec3 {
        val transformed = size
            .let { if (rotation != Rotation.NONE) size.rotate(rotation, origin) else it }
            .let { if (mirror != Mirror.NONE) size.mirror(mirror, origin) else it }

        return transformed
    }


    private fun getPlacementPriority(rule: StructureRule): Int {
        // Simple example: Ancient ruins before villages
        return when {
            StructureTag.ANCIENT in rule.tags -> 0
            StructureTag.DUNGEON in rule.tags -> 1
            StructureTag.VILLAGE in rule.tags -> 2
            else -> 3
        }
    }
}

