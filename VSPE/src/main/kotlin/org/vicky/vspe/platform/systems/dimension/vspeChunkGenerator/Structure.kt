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
    val nbtFile: File
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

    @Synchronized
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

        val finalMap: ConcurrentHashMap<ChunkCoord, List<BlockPlacement<T>>> =
            ConcurrentHashMap<ChunkCoord, List<BlockPlacement<T>>>()
        placementsByChunk.mapValues { it.value.toList() }
            .forEach { finalMap.put(it.key, it.value) }
        val resolved = ResolvedStructure(finalMap, bounds)
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

    @Synchronized
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
        val finalMap: ConcurrentHashMap<ChunkCoord, List<BlockPlacement<BST>>> =
            ConcurrentHashMap<ChunkCoord, List<BlockPlacement<BST>>>()
            placementsByChunk.mapValues { it.value.toList() }
                .forEach { finalMap.put(it.key, it.value) }

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

    @Synchronized
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


        val finalMap: ConcurrentHashMap<ChunkCoord, List<BlockPlacement<BST>>> =
            ConcurrentHashMap<ChunkCoord, List<BlockPlacement<BST>>>()
        placementsByChunk.mapValues { it.value.toList() }
            .forEach { finalMap.put(it.key, it.value) }

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
    val placementsByChunk: ConcurrentHashMap<ChunkCoord, List<BlockPlacement<T>>>,
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

    @Synchronized
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

        // copy rules out (avoid concurrent-mod issues)
        val allRules = VSPEPlatformPlugin.structureManager().structures.values.map { it.second }

        for (rule in allRules.toList()) {
            // quick biome check
            val biome = context.biomeResolver.resolveBiome(chunkX * 16, 64, chunkZ * 16, context.random.getSeed())
            if (biome.biomeStructureData.structureKeys.contains { it == rule.resource }) continue

            val structurePair = VSPEPlatformPlugin.structureManager().getStructures()[rule.resource] ?: continue
            val structure = structurePair.first
            val size = structure.getSize().toVec3()

            val cellSizeBlocks = rule.spacing * 16

            // conservative margins (safe over-approximation)
            val maxStructureHalfExtentX = size.getX().toInt()
            val maxStructureHalfExtentZ = size.getZ().toInt()

            val minPossibleOriginBlockX = chunkMinX - maxStructureHalfExtentX
            val maxPossibleOriginBlockX = chunkMaxX + maxStructureHalfExtentX
            val minPossibleOriginBlockZ = chunkMinZ - maxStructureHalfExtentZ
            val maxPossibleOriginBlockZ = chunkMaxZ + maxStructureHalfExtentZ

            val minCellX = floorDiv(minPossibleOriginBlockX, cellSizeBlocks)
            val maxCellX = floorDiv(maxPossibleOriginBlockX, cellSizeBlocks)
            val minCellZ = floorDiv(minPossibleOriginBlockZ, cellSizeBlocks)
            val maxCellZ = floorDiv(maxPossibleOriginBlockZ, cellSizeBlocks)

            cellLoop@ for (cellX in minCellX..maxCellX) {
                for (cellZ in minCellZ..maxCellZ) {
                    val key = CellKey(worldSeed, rule.resource, cellX, cellZ)
                    var placement: PlacementInfo? = placementCache.get(key)
                    if (placement == null) {
                        // decide deterministically for this cell
                        val seed = placementSeed(worldSeed, rule.resource, cellX, cellZ)
                        val rng = Random(seed)

                        if (rng.nextDouble() <= rule.frequency) {
                            // build PlacementInfo only when this cell actually contains a structure
                            val rot = when (rng.nextInt(4)) {
                                0 -> Rotation.NONE
                                1 -> Rotation.CLOCKWISE_90
                                2 -> Rotation.CLOCKWISE_180
                                else -> Rotation.COUNTERCLOCKWISE_90
                            }
                            val mir = if (rng.nextBoolean()) rule.mirror else Mirror.NONE

                            val transformedSize =
                                transformSize(Vec3(0.0, 0.0, 0.0), structure.getSize().toVec3(), rot, mir)
                            val tSizeX = transformedSize.getX().toInt()
                            val tSizeZ = transformedSize.getZ().toInt()

                            val maxOffsetX = (cellSizeBlocks - tSizeX).coerceAtLeast(0)
                            val maxOffsetZ = (cellSizeBlocks - tSizeZ).coerceAtLeast(0)
                            val offsetX = if (maxOffsetX > 0) rng.nextInt(maxOffsetX + 1) else 0
                            val offsetZ = if (maxOffsetZ > 0) rng.nextInt(maxOffsetZ + 1) else 0

                            val originBlockX = cellX * cellSizeBlocks + offsetX
                            val originBlockZ = cellZ * cellSizeBlocks + offsetZ
                            val originBlockY = 64 // or deterministic height provider

                            val originVec =
                                Vec3(originBlockX.toDouble(), originBlockY.toDouble(), originBlockZ.toDouble())
                            val min = originVec
                            val max = originVec.offset(
                                (transformedSize.getX() - 1).toInt(),
                                (transformedSize.getY() - 1).toInt(),
                                (transformedSize.getZ() - 1).toInt()
                            )
                            val sBox = StructureBox(min, max, rule.resource)

                            // make the PlacementInfo (add ownerChunkX/Z if needed)
                            placement = PlacementInfo(
                                origin = originVec,
                                rotation = rot,
                                mirror = mir,
                                box = sBox,
                                rule = rule,
                                ownerChunkX = Math.floorDiv(originBlockX, 16),
                                ownerChunkZ = Math.floorDiv(originBlockZ, 16),
                                resolved = null // if your PlacementInfo has resolved field
                            )

                            // store into cache (only non-null placements are cached)
                            placementCache.put(key, placement)
                        } else {
                            // rng test failed -> no placement for this cell -> leave placement==null
                        }
                    }

                    if (placement != null) {
                        // quick intersection check with this chunk's block bounds
                        if (placement.box.max.x >= chunkMinX &&
                            placement.box.min.x <= chunkMaxX &&
                            placement.box.max.z >= chunkMinZ &&
                            placement.box.min.z <= chunkMaxZ
                        ) {
                            // avoid overlapping placements in this same-pass candidate list
                            if (structureBoxes.any { it.intersects(placement.box) }) {
                                continue@cellLoop
                            }

                            structureBoxes.add(placement.box)
                            candidates.add(placement)
                        }
                    }
                }
            }
        } // end rules loop

        // sort by priority, then weight (weight descending)
        candidates.sortedWith(compareBy({ getPlacementPriority(it.rule) }, { -it.rule.weight })).forEach { p ->
            // assume tuple: Pair<PlatformStructure<*>, StructureRule>
            val tuple = VSPEPlatformPlugin.structureManager().getStructures()[p.rule.resource] ?: return@forEach

// treat structure as PlatformStructure<Any?>
            @Suppress("UNCHECKED_CAST")
            val structureAny = tuple.first as PlatformStructure<Any?>

            if (p.ownerChunkX == chunkX && p.ownerChunkZ == chunkZ) {
                val seed = placementSeed(
                    worldSeed, p.rule.resource,
                    (p.origin.x.toInt() / (p.rule.spacing * 16)),
                    (p.origin.z.toInt() / (p.rule.spacing * 16))
                )
                val seededRand = Random(seed)
                val placementContext = StructurePlacementContext(
                    random = SeededRandomSource(seededRand.nextLong()),
                    rotation = p.rotation,
                    mirror = p.mirror
                )

                val platformOrigin = context.locationProvider.invoke(
                    p.origin.x.toInt(), p.origin.y.toInt(), p.origin.z.toInt()
                )

                // resolve -> returns ResolvedStructure<*>, cast to ResolvedStructure<Any?>
                @Suppress("UNCHECKED_CAST")
                val resolvedAny = structureAny.resolve(platformOrigin, placementContext) as ResolvedStructure<Any?>

                // store into cache slot (make sure PlacementInfo.resolved is typed ResolvedStructure<Any?>?)
                p.resolved = resolvedAny

                // Cast the BlockPlacer to BlockPlacer<Any?> and call placeInChunk
                @Suppress("UNCHECKED_CAST")
                val placerAny = context.blockPlacer as BlockPlacer<Any?>

                structureAny.placeInChunk(placerAny, chunkX, chunkZ, resolvedAny, placementContext)
            } else {
                // non-owner chunk: try to fetch cached resolved structure
                val cellSizeBlocks = p.rule.spacing * 16
                val cellX = floorDiv(p.origin.x.toInt(), cellSizeBlocks)
                val cellZ = floorDiv(p.origin.z.toInt(), cellSizeBlocks)

                val cached = placementCache.get(CellKey(worldSeed, p.rule.resource, cellX, cellZ))?.resolved
                if (cached != null) {
                    @Suppress("UNCHECKED_CAST")
                    val cachedAny = cached as ResolvedStructure<Any?>

                    val ctxRand = context.random.fork(p.origin.hashCode().toLong())
                    val placementContext =
                        StructurePlacementContext(random = ctxRand, rotation = p.rotation, mirror = p.mirror)

                    @Suppress("UNCHECKED_CAST")
                    val placerAny = context.blockPlacer as BlockPlacer<Any?>

                    structureAny.placeInChunk(placerAny, chunkX, chunkZ, cachedAny, placementContext)
                } else {
                    // owner will compute and cache later — skip for now
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
data class CellKey(val worldSeed: Long, val resource: ResourceLocation, val cellX: Int, val cellZ: Int) {
    override fun equals(other: Any?): Boolean {
        if (other !is CellKey) return false
        if (other.worldSeed != this.worldSeed) return false
        if (this.resource != other.resource) return false
        if (this.cellX != other.cellX) return false
        return this.cellZ == other.cellZ
    }

    override fun hashCode(): Int {
        var result = worldSeed.hashCode()
        result = 31 * result + cellX
        result = 31 * result + cellZ
        result = 31 * result + resource.hashCode()
        return result
    }
}

data class PlacementInfo(
    val origin: Vec3,                // absolute block origin in world blocks (min corner)
    val rotation: Rotation,
    val mirror: Mirror,
    val box: StructureBox,
    val rule: StructureRule,
    // canonical owner chunk (so only owner resolves the heavy work)
    val ownerChunkX: Int,
    val ownerChunkZ: Int,
    // volatile so other threads may see it after owner sets it
    @Volatile var resolved: ResolvedStructure<Any?>? = null
)

class StructurePlacementCache {
    private val map = ConcurrentHashMap<CellKey, PlacementInfo>()

    fun computeIfAbsent(key: CellKey, supplier: () -> PlacementInfo): PlacementInfo {
        // computeIfAbsent on ConcurrentHashMap provides atomic first-writer semantics
        return map.computeIfAbsent(key) { supplier() }
    }

    fun get(key: CellKey): PlacementInfo? = map[key]

    fun put(key: CellKey, value: PlacementInfo) {
        map[key] = value
    }

    fun clear() = map.clear()
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