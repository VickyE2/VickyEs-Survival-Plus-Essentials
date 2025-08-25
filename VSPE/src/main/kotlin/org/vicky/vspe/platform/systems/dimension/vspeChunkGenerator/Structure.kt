package org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator

import de.pauleff.api.ICompoundTag
import de.pauleff.api.ITag
import de.pauleff.api.NBTFileFactory
import net.sandrohc.schematic4j.schematic.Schematic
import org.vicky.platform.PlatformPlugin
import org.vicky.platform.utils.Mirror
import org.vicky.platform.utils.ResourceLocation
import org.vicky.platform.utils.Rotation
import org.vicky.platform.utils.Vec3
import org.vicky.platform.world.PlatformBlockState
import org.vicky.platform.world.PlatformMaterial
import org.vicky.vspe.*
import org.vicky.vspe.platform.VSPEPlatformPlugin
import org.vicky.vspe.platform.systems.dimension.StructureUtils.ProceduralStructureGenerator
import org.vicky.vspe.structure_gen.offset
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.floor
import kotlin.random.Random

// === Core Interfaces ===

interface PlatformStructure<T> {
    /**
     * Legacy: place the whole structure — kept for compatibility.
     */
    fun place(world: BlockPlacer<T>, origin: Vec3, context: StructurePlacementContext): Boolean

    /**
     * Resolve produces a ResolvedStructure (bucketed by chunk) for the given origin/context.
     * This should be deterministic given the same inputs (include random seed in context if needed).
     */
    fun resolve(origin: Vec3, context: StructurePlacementContext): ResolvedStructure<T>

    /**
     * Place only blocks that belong to the provided chunk using a resolved structure.
     * Implementations should only attempt to place the subset of blocks that fall in chunkX/chunkZ.
     */
    fun placeInChunk(
        world: BlockPlacer<T>,
        chunkX: Int,
        chunkZ: Int,
        resolved: ResolvedStructure<T>,
        context: StructurePlacementContext
    ): Boolean

    fun getSize(): BlockVec3i
}

fun <T> PlatformStructure<T>.placeChunkFallback(
    world: BlockPlacer<T>,
    chunkX: Int,
    chunkZ: Int,
    resolved: ResolvedStructure<T>,
    context: StructurePlacementContext
): Boolean {
    // naive approach: iterate resolved.placementsByChunk[ChunkCoord(chunkX, chunkZ)]
    val list = resolved.placementsByChunk[ChunkCoord(chunkX, chunkZ)] ?: return true
    list.forEach { p ->
        // build a PlatformLocation for p.x/p.y/p.z — adapt to your PlatformLocation constructor
        val loc = Vec3(p.x.toDouble(), p.y.toDouble(), p.z.toDouble())
        if (p.nbt != null)
            world.placeBlock(loc, p.state as PlatformBlockState<T>?, p.nbt)
        else
            world.placeBlock(loc, p.state as PlatformBlockState<T>?)
    }
    return true
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

    fun <T> resolveBlockState(stateIndex: Int): PlatformBlockState<T> {
        val paletteEntry = palette.getOrNull(stateIndex)
            ?: error("Palette index $stateIndex out of bounds")

        val name = paletteEntry.getString("Name")
        val propsTag = paletteEntry.getCompound("Properties")
        val properties = propsTag.data.joinToString(",") { tag ->
            "${tag.name}=${tag.data}"
        }

        return PlatformPlugin.stateFactory()
            .getBlockState(if (properties.isEmpty()) name else "$name[$properties]") as PlatformBlockState<T>
    }
}

// === Structure Implementations ===

class NBTBasedStructure<T>(val id: ResourceLocation) : PlatformStructure<T> {
    private val structure: NbtStructure<T>? =
        VSPEPlatformPlugin.structureManager().getNBTStructure(id) as NbtStructure<T>?

    // small LRU per-structure cache (tune size as needed)
    private val resolvedCache = LruCache<ResolvedKey, ResolvedStructure<T>>(128)

    override fun getSize(): BlockVec3i = structure?.size ?: throw NullPointerException("Structure $id does not exist")

    override fun place(world: BlockPlacer<T>, origin: Vec3, context: StructurePlacementContext): Boolean {
        // fallback: call resolve + place all chunk groups (useful if you want to place whole structure)
        val resolved = resolve(origin, context)
        // iterate all chunks and write; note: might write outside currently generating chunk
        resolved.placementsByChunk.values.flatten().forEach { p ->
            if (p.nbt != null)
                world.placeBlock(p.x, p.y, p.z, p.state as PlatformBlockState<T>?, p.nbt)
            else
                world.placeBlock(p.x, p.y, p.z, p.state as PlatformBlockState<T>?)
        }
        return true
    }

    override fun resolve(origin: Vec3, context: StructurePlacementContext): ResolvedStructure<T> {
        val seed = context.random.getSeed()
        val key = ResolvedKey(
            id,
            origin.x.toInt(),
            origin.y.toInt(),
            origin.z.toInt(),
            context.rotation,
            context.mirror,
            seed
        )
        resolvedCache.get(key)?.let { return it }

        if (structure == null) error("Unknown structure: $id")

        val placementsByChunk = mutableMapOf<ChunkCoord, MutableList<BlockPlacement<T>>>()
        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var minZ = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var maxY = Int.MIN_VALUE
        var maxZ = Int.MIN_VALUE

        structure.blocks.forEach { blockTag ->
            val posTag = blockTag.getList("pos") ?: return@forEach
            val stateIdx = blockTag.getInt("state")
            val nbt = blockTag.getCompound("nbt")

            val dx = (posTag.data[0] as ITag<Integer>).data
            val dy = (posTag.data[1] as ITag<Integer>).data
            val dz = (posTag.data[2] as ITag<Integer>).data

            // absolute local position before transforms
            val localX = origin.x.toInt() + dx.toInt()
            val localY = origin.y.toInt() + dy.toInt()
            val localZ = origin.z.toInt() + dz.toInt()

            // apply rotation/mirror relative to origin (make util functions for Vec3 / PlatformLocation transforms)
            val transformed = Vec3(localX.toDouble(), localY.toDouble(), localZ.toDouble())
                .let { if (context.rotation != Rotation.NONE) it.rotate(context.rotation, origin) else it }
                .let { if (context.mirror != Mirror.NONE) it.mirror(context.mirror, origin) else it }

            val bx = transformed.x.toInt()
            val by = transformed.y.toInt()
            val bz = transformed.z.toInt()

            val state = structure.resolveBlockState<T>(stateIdx)

            // update bounds
            minX = minOf(minX, bx); minY = minOf(minY, by); minZ = minOf(minZ, bz)
            maxX = maxOf(maxX, bx); maxY = maxOf(maxY, by); maxZ = maxOf(maxZ, bz)

            val chunk = ChunkCoord.fromBlock(bx, bz)
            placementsByChunk.computeIfAbsent(chunk) { mutableListOf() }
                .add(BlockPlacement(bx, by, bz, state, nbt))
        }

        val bounds = StructureBox(
            Vec3(minX.toDouble(), minY.toDouble(), minZ.toDouble()),
            Vec3(maxX.toDouble(), maxY.toDouble(), maxZ.toDouble()),
            id
        )
        val resolved = ResolvedStructure(placementsByChunk.mapValues { it.value.toList() }, bounds)
        resolvedCache.put(key, resolved)
        return resolved
    }

    override fun placeInChunk(
        world: BlockPlacer<T>,
        chunkX: Int,
        chunkZ: Int,
        resolved: ResolvedStructure<T>,
        context: StructurePlacementContext
    ): Boolean {
        val list = resolved.placementsByChunk[ChunkCoord(chunkX, chunkZ)] ?: return true
        list.forEach { p ->
            if (p.nbt != null)
                world.placeBlock(p.x, p.y, p.z, p.state as PlatformBlockState<T>?, p.nbt)
            else
                world.placeBlock(p.x, p.y, p.z, p.state as PlatformBlockState<T>?)
        }
        return true
    }
}

class SchematicStructure<BST>(
    private val schematic: Schematic,
    private val transformer: (String) -> BST
) : PlatformStructure<BST>
{
    override fun place(world: BlockPlacer<BST>, origin: Vec3, context: StructurePlacementContext): Boolean {
        // Backwards-compat: place everything (not chunk-limited)
        val resolved = resolve(origin, context)
        resolved.placementsByChunk.values.flatten().forEach { p ->
            val pos = Vec3(p.x.toDouble(), p.y.toDouble(), p.z.toDouble())
            world.placeBlock(pos, p.state as PlatformBlockState<BST>?)
        }
        return true
    }

    override fun resolve(
        origin: Vec3,
        context: StructurePlacementContext
    ): ResolvedStructure<BST> {
        val placementsByChunk = mutableMapOf<ChunkCoord, MutableList<BlockPlacement<BST>>>()
        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var minZ = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var maxY = Int.MIN_VALUE
        var maxZ = Int.MIN_VALUE

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

            val bx = transformedPos.x.toInt()
            val by = transformedPos.y.toInt()
            val bz = transformedPos.z.toInt()

            val state = SimpleBlockState.from<BST>(blockData.name(), transformer) as PlatformBlockState<BST>?

            minX = minOf(minX, bx); minY = minOf(minY, by); minZ = minOf(minZ, bz)
            maxX = maxOf(maxX, bx); maxY = maxOf(maxY, by); maxZ = maxOf(maxZ, bz)

            val chunk = ChunkCoord.fromBlock(bx, bz)
            placementsByChunk.computeIfAbsent(chunk) { mutableListOf() }
                .add(BlockPlacement(bx, by, bz, state))
        }

        val bounds = StructureBox(
            Vec3(minX.toDouble(), minY.toDouble(), minZ.toDouble()),
            Vec3(maxX.toDouble(), maxY.toDouble(), maxZ.toDouble()),
            ResourceLocation.from("schematic", schematic.toString())
        )

        // convert lists to immutable lists
        val finalMap: Map<ChunkCoord, List<BlockPlacement<BST>>> =
            placementsByChunk.mapValues { it.value.toList() }

        return ResolvedStructure(finalMap, bounds)
    }

    override fun placeInChunk(
        world: BlockPlacer<BST>,
        chunkX: Int,
        chunkZ: Int,
        resolved: ResolvedStructure<BST>,
        context: StructurePlacementContext
    ): Boolean {
        val chunk = ChunkCoord(chunkX, chunkZ)
        val list = resolved.placementsByChunk[chunk] ?: return true
        list.forEach { p ->
            val pos = Vec3(p.x.toDouble(), p.y.toDouble(), p.z.toDouble())
            world.placeBlock(pos, p.state as PlatformBlockState<BST>?)
        }
        return true
    }

    override fun getSize(): BlockVec3i =
        BlockVec3i(schematic.length(), schematic.width(), schematic.height())
}

class ProceduralStructure<T : ProceduralStructureGenerator<BST>, BST>(
    private val generator: T
) : PlatformStructure<BST>
{
    // LRU cache keyed by ResolvedKey (resource can be derived from generator class)
    private val resolvedCache = LruCache<ResolvedKey, ResolvedStructure<BST>>(128)

    override fun place(world: BlockPlacer<BST>, origin: Vec3, context: StructurePlacementContext): Boolean {
        val resolved = resolve(origin, context)
        resolved.placementsByChunk.values.flatten().forEach { p ->
            val pos = Vec3(p.x.toDouble(), p.y.toDouble(), p.z.toDouble())
            world.placeBlock(pos, p.state)
        }
        return true
    }

    override fun resolve(
        origin: Vec3,
        context: StructurePlacementContext
    ): ResolvedStructure<BST> {
        val seed = context.random.getSeed()
        // build a stable resource id for this procedural generator (tweak if you've got a canonical id)
        val resource = ResourceLocation.from("procedural", generator.javaClass.simpleName.lowercase())

        val key = ResolvedKey(
            resource,
            origin.x.toInt(),
            origin.y.toInt(),
            origin.z.toInt(),
            context.rotation,
            context.mirror,
            seed
        )
        resolvedCache.get(key)?.let { return it }

        // call the generator to produce block placements (assume it returns List<BlockPlacement<BST>>)
        // If your generator returns a GenerationResult (placements+actions), adapt this line:
        val placements: List<BlockPlacement<BST>> = generator.generate(context.random, origin).placements

        // bucket by chunk
        val placementsByChunk = mutableMapOf<ChunkCoord, MutableList<BlockPlacement<BST>>>()
        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var minZ = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var maxY = Int.MIN_VALUE
        var maxZ = Int.MIN_VALUE

        placements.forEach { p ->
            // NOTE: BlockPlacement likely already stores absolute coordinates as ints
            val bx = p.x
            val by = p.y
            val bz = p.z

            minX = minOf(minX, bx); minY = minOf(minY, by); minZ = minOf(minZ, bz)
            maxX = maxOf(maxX, bx); maxY = maxOf(maxY, by); maxZ = maxOf(maxZ, bz)

            val chunk = ChunkCoord.fromBlock(bx, bz)
            placementsByChunk.computeIfAbsent(chunk) { mutableListOf() }.add(p)
        }

        val bounds = StructureBox(
            Vec3(minX.toDouble(), minY.toDouble(), minZ.toDouble()),
            Vec3(maxX.toDouble(), maxY.toDouble(), maxZ.toDouble()),
            resource
        )

        val finalMap: Map<ChunkCoord, List<BlockPlacement<BST>>> =
            placementsByChunk.mapValues { it.value.toList() }

        val resolved = ResolvedStructure(finalMap, bounds)
        resolvedCache.put(key, resolved)
        return resolved
    }

    override fun placeInChunk(
        world: BlockPlacer<BST>,
        chunkX: Int,
        chunkZ: Int,
        resolved: ResolvedStructure<BST>,
        context: StructurePlacementContext
    ): Boolean {
        val list = resolved.placementsByChunk[ChunkCoord(chunkX, chunkZ)] ?: return true
        list.forEach { p ->
            val pos = Vec3(p.x.toDouble(), p.y.toDouble(), p.z.toDouble())
            world.placeBlock(pos, p.state as PlatformBlockState<BST>?)
        }
        return true
    }

    override fun getSize(): BlockVec3i = generator.getApproximateSize()
}


// === Structure Placement ===

// small util for chunk coordinates
data class ChunkCoord(val cx: Int, val cz: Int) {
    companion object {
        fun fromBlock(x: Int, z: Int): ChunkCoord {
            val cx = floorDiv(x, 16)
            val cz = floorDiv(z, 16)
            return ChunkCoord(cx, cz)
        }
    }
}

data class BlockPlacement<T>(
    val x: Int,
    val y: Int,
    val z: Int,
    val state: PlatformBlockState<T>?,
    val nbt: ICompoundTag? = null
)

/**
 * Resolved structure contains placements bucketed by chunk coords.
 * This is the thing you cache and then reuse to place into multiple chunks.
 */
data class ResolvedStructure<T>(
    val placementsByChunk: Map<ChunkCoord, List<BlockPlacement<T>>>,
    val bounds: StructureBox // optional bounding box for quick tests
)

data class ResolvedKey(
    val resource: ResourceLocation,
    val originX: Int,
    val originY: Int,
    val originZ: Int,
    val rotation: Rotation,
    val mirror: Mirror,
    val seed: Long // include if procedural or randomness depend on it
)

class LruCache<K, V>(private val maxEntries: Int) {
    private val map = object : LinkedHashMap<K, V>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>): Boolean {
            return size > maxEntries
        }
    }

    @Synchronized
    fun get(k: K): V? = map[k]
    @Synchronized
    fun put(k: K, v: V) {
        map[k] = v
    }

    @Synchronized
    fun clear() = map.clear()
}

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
    fun setSeed(seed: Long)
    fun fork(seedModifier: Long): RandomSource // useful for per-feature randomness
}

class SeededRandomSource(private val random: Random, private var seed: Long) : RandomSource {

    constructor(seed: Long) : this(Random(seed), seed)

    override fun nextInt(bound: Int): Int = random.nextInt(bound)
    override fun nextInt(start: Int, bound: Int): Int = random.nextInt(start, bound)

    override fun nextDouble(): Double = random.nextDouble()

    override fun nextFloat(): Float = random.nextFloat()

    override fun nextBoolean(): Boolean = random.nextBoolean()

    override fun nextLong(): Long = random.nextLong()
    override fun getSeed(): Long = seed
    override fun setSeed(seed: Long) {
        this.seed = seed
    }

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


class WeightedStructurePlacer<T> : StructurePlacer<T> {

    // reuse a single cache instance; if you want a per-world per-run cache, create per-world.
    private val placementCache = StructurePlacementCache()

    private fun transformSize(origin: Vec3, size: Vec3, rotation: Rotation, mirror: Mirror): Vec3 {
        val transformed = size
            .let { if (rotation != Rotation.NONE) size.rotate(rotation, origin) else it }
            .let { if (mirror != Mirror.NONE) size.mirror(mirror, origin) else it }

        return transformed
    }

    override fun placeStructuresInChunk(
        chunkX: Int,
        chunkZ: Int,
        context: ChunkGenerateContext<T, *>,
        chunkData: ChunkData<T, *>
    ) {
        val worldSeed = context.random.getSeed()
        val chunkMinX = chunkX * 16
        val chunkMaxX = chunkMinX + 15
        val chunkMinZ = chunkZ * 16
        val chunkMaxZ = chunkMinZ + 15

        val candidates = mutableListOf<PlacementInfo>()
        val structureBoxes = mutableListOf<StructureBox>()

        val allRules = VSPEPlatformPlugin.structureManager().structures.values.map { it.second }

        for (rule in allRules) {
            // quick biome check (leave as you had it)
            val biome = context.biomeResolver.resolveBiome(chunkX * 16, 64, chunkZ * 16, context.random.getSeed())
            if (biome.biomeStructureData.structureKeys.contains { it == rule.resource }) continue

            val structurePair = VSPEPlatformPlugin.structureManager().getStructures()[rule.resource] ?: continue
            val structure = structurePair.first
            // compute transformed size once for this structure+rotation+mirror combination
            val size = structure.getSize().toVec3()

            // cell size (in blocks) defines grid for deterministic placement
            val cellSizeBlocks = rule.spacing * 16

            // we want to check every "cell" whose potential structure box might touch this chunk.
            // compute a conservative margin: half the structure diagonal (max extents)
            val maxStructureHalfExtentX = size.getX() // conservative (not halved) to be safe
            val maxStructureHalfExtentZ = size.getZ()

            // convert to cell ranges that could influence this chunk
            val minPossibleOriginBlockX = chunkMinX - maxStructureHalfExtentX
            val maxPossibleOriginBlockX = chunkMaxX + maxStructureHalfExtentX
            val minPossibleOriginBlockZ = chunkMinZ - maxStructureHalfExtentZ
            val maxPossibleOriginBlockZ = chunkMaxZ + maxStructureHalfExtentZ

            val minCellX = floorDiv(minPossibleOriginBlockX, cellSizeBlocks)
            val maxCellX = floorDiv(maxPossibleOriginBlockX, cellSizeBlocks)
            val minCellZ = floorDiv(minPossibleOriginBlockZ, cellSizeBlocks)
            val maxCellZ = floorDiv(maxPossibleOriginBlockZ, cellSizeBlocks)

            // iterate relevant cells only
            cellLoop@ for (cellX in minCellX..maxCellX) {
                for (cellZ in minCellZ..maxCellZ) {
                    val key = CellKey(worldSeed, rule.resource, cellX, cellZ)
                    val placement = placementCache.computeIfAbsent(key) {
                        // compute whether this cell produces a structure
                        val seed = placementSeed(worldSeed, rule.resource, cellX, cellZ)
                        val rng = Random(seed)

                        // frequency check: deterministic per cell
                        if (rng.nextDouble() > rule.frequency) return@computeIfAbsent null

                        // pick rotation deterministically (example: 4 cardinal rotations)
                        val rotation = when (rng.nextInt(4)) {
                            0 -> Rotation.NONE
                            1 -> Rotation.CLOCKWISE_90
                            2 -> Rotation.CLOCKWISE_180
                            else -> Rotation.COUNTERCLOCKWISE_90
                        }
                        // pick mirror deterministically (adjust to your Mirror enum)
                        val mirror = if (rng.nextBoolean()) rule.mirror else Mirror.NONE

                        // compute transformed structure size for rotation/mirror
                        val transformedSize = transformSize(Vec3(0.0, 0.0, 0.0), size, rotation, mirror)
                        val tSizeX = transformedSize.getX().toInt()
                        val tSizeZ = transformedSize.getZ().toInt()

                        // choose an origin within the cell so structures don't all align to cell corner
                        val maxOffsetX = (cellSizeBlocks - tSizeX).coerceAtLeast(0)
                        val maxOffsetZ = (cellSizeBlocks - tSizeZ).coerceAtLeast(0)
                        val offsetX = if (maxOffsetX > 0) rng.nextInt(0, maxOffsetX + 1) else 0
                        val offsetZ = if (maxOffsetZ > 0) rng.nextInt(0, maxOffsetZ + 1) else 0

                        val originBlockX = cellX * cellSizeBlocks + offsetX
                        val originBlockZ = cellZ * cellSizeBlocks + offsetZ
                        // choose Y using your locationProvider or keep baseline (64)
                        val originBlockY = 64 // you can also query context.locationProvider for terrain height

                        val originVec = Vec3(originBlockX.toDouble(), originBlockY.toDouble(), originBlockZ.toDouble())

                        // compute box for collision testing
                        val min = originVec
                        val max = originVec.offset(
                            (transformedSize.getX() - 1).toInt(),
                            (transformedSize.getY() - 1).toInt(),
                            (transformedSize.getZ() - 1).toInt()
                        )
                        val structureBox = StructureBox(min, max, rule.resource)

                        // Also do a spacing/overlap check here if you want to avoid conflict with already decided placements
                        // For now we just return PlacementInfo; caller will check for overlaps in-chunk
                        return@computeIfAbsent PlacementInfo(originVec, rotation, mirror, structureBox, rule)
                    }

                    if (placement != null) {
                        // quick intersection check with this chunk's block bounds
                        if (placement.box.max.x >= chunkMinX &&
                            placement.box.min.x <= chunkMaxX &&
                            placement.box.max.z >= chunkMinZ &&
                            placement.box.min.z <= chunkMaxZ
                        ) {

                            // do not allow overlapping same-pass placements if you want: check structureBoxes list
                            if (structureBoxes.any { it.intersects(placement.box) }) {
                                // skip overlapping placements (optional; previously you filtered overlaps earlier)
                                continue@cellLoop
                            }

                            structureBoxes.add(placement.box)
                            candidates.add(placement)
                        }
                    }
                }
            }
        } // end rules loop

        // sort candidates by priority (use rule.priority if you have such), then by weight
        // I keep your getPlacementPriority() logic; use it to sort
        candidates.sortedWith(compareBy({ getPlacementPriority(it.rule) }, { -it.rule.weight })).forEach { p ->
            val structureRaw = VSPEPlatformPlugin.structureManager()
                .getStructures()[p.rule.resource] as Pair<PlatformStructure<T>, StructureRule>?
            if (structureRaw != null) {
                val structure = structureRaw.first
                val origin = context.locationProvider.invoke(p.origin.x.toInt(), p.origin.y.toInt(), p.origin.z.toInt())
                val success = structure.placeInChunk(
                    context.blockPlacer,
                    chunkX, chunkZ,
                    structure.resolve(
                        origin,
                        StructurePlacementContext(
                            random = context.random.fork(p.origin.hashCode().toLong()),
                            rotation = p.rotation,
                            mirror = p.mirror
                        )
                    ),
                    StructurePlacementContext(
                        random = context.random.fork(p.origin.hashCode().toLong()),
                        rotation = p.rotation,
                        mirror = p.mirror
                    )
                )
                if (!success) {
                    VSPEPlatformPlugin.platformLogger()
                        .warn("Failed to place structure ${p.rule.resource} at cell placement ${p.origin} ")
                }
            }
        }
    }

    // reuse transformSize and getPlacementPriority from your class (keep as-is)
    private fun getPlacementPriority(rule: StructureRule): Int {
        return when {
            StructureTag.ANCIENT in rule.tags -> 0
            StructureTag.DUNGEON in rule.tags -> 1
            StructureTag.VILLAGE in rule.tags -> 2
            else -> 3
        }
    }
}

// Deterministic placement info for a structure instance inside a "cell".
data class PlacementInfo(
    val origin: Vec3,                // absolute block origin in world blocks (min corner)
    val rotation: Rotation,
    val mirror: Mirror,
    val box: StructureBox,
    val rule: StructureRule
)

// Cache key for per-cell placement decisions
data class CellKey(val worldSeed: Long, val resource: ResourceLocation, val cellX: Int, val cellZ: Int)

// Simple cache. You may replace with an LRU map if you want to bound memory.
class StructurePlacementCache {
    private val cache = ConcurrentHashMap<CellKey, PlacementInfo?>()
    fun get(key: CellKey): PlacementInfo? = cache[key]
    fun put(key: CellKey, value: PlacementInfo?) {
        cache[key] = value
    }

    // compute-if-absent convenience
    fun computeIfAbsent(key: CellKey, supplier: () -> PlacementInfo?): PlacementInfo? {
        return cache.computeIfAbsent(key) { supplier() }
    }

    fun clear() = cache.clear()
}

private const val PRIME1 = 341873128712L
private const val PRIME2 = 132897987541L

/**
 * Create a deterministic seed for a given world seed + resource + cell coords
 */
private fun placementSeed(worldSeed: Long, resource: ResourceLocation, cellX: Int, cellZ: Int): Long {
    var h = worldSeed xor resource.hashCode().toLong()
    h = h * 6364136223846793005L + PRIME1 * cellX + PRIME2 * cellZ
    return h
}

/**
 * floorDivide that works like Java's Math.floorDiv for Ints (makes negative cells sane).
 */
private fun floorDiv(a: Int, b: Int): Int {
    return floor(a.toDouble() / b.toDouble()).toInt()
}
