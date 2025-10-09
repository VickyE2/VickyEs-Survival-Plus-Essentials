package org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
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
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Rarity
import org.vicky.vspe.structure_gen.offset
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicInteger
import java.util.random.RandomGenerator
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
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

    fun cacheSize(): Long
    
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

    fun generateKey(origin: Vec3, context: StructurePlacementContext, id: ResourceLocation): ResolvedKey {
        val size = getSize() / 2 // BlockVec3i: deterministic
        val spacingBlocks = max(size.x, size.z)
        val spacingChunks = max(1, (spacingBlocks + 15) / 16) // ceil -> chunks

        // 2) snap origin to CHUNK grid (use chunk coordinates)
        val originChunkX = origin.x.toInt() shr 4
        val originChunkZ = origin.z.toInt() shr 4
        val baseChunkX = originChunkX - (spacingChunks / 2)
        val baseChunkZ = originChunkZ - (spacingChunks / 2)

        // 3) back to BLOCK coords for canonical origin used by generator
        val baseX = baseChunkX shl 4
        val baseZ = baseChunkZ shl 4
        val baseY = origin.round().intY

        return ResolvedKey(
            id,
            baseX,
            baseY,
            baseZ,
            context.rotation,
            context.mirror,
            context.random.getSeed()
        )
    }
}

fun <T> PlatformStructure<T>.placeChunkFallback(
    world: BlockPlacer<T>,
    chunkX: Int,
    chunkZ: Int,
    resolved: ResolvedStructure<T>
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
class NBTBasedStructure<T>(
    val id: ResourceLocation
) : PlatformStructure<T> {
    private val structure: NbtStructure<T>? =
        VSPEPlatformPlugin.structureManager().getNBTStructure(id) as NbtStructure<T>?

    // small LRU per-structure cache (tune size as needed)
    private val chunkToKey = ConcurrentHashMap<ChunkCoord, ResolvedKey>()
    private val resolvedCache: Cache<ResolvedKey, ResolvedStructure<T>> = Caffeine.newBuilder()
        .maximumSize(256)
        .removalListener<ResolvedKey, ResolvedStructure<T>> { key, value, _ ->
            if (key != null && value != null) {
                value.placementsByChunk.keys.forEach { chunk -> chunkToKey.remove(chunk, key) }
            }
        }.build()

    override fun cacheSize(): Long {
        return resolvedCache.estimatedSize()
    }

    override fun resolve(origin: Vec3, context: StructurePlacementContext): ResolvedStructure<T> {
        context.random.getSeed() // include if you want seed in key, else 0
        val key = generateKey(origin, context, id)

        // fast chunk->key lookup
        val originChunk = ChunkCoord.fromBlock(origin.x.toInt(), origin.z.toInt())
        chunkToKey[originChunk]?.let { existingKey ->
            resolvedCache.getIfPresent(existingKey)?.let { return it }
        }

        val resolved = resolvedCache.get(key) { k ->
            generateResolvedFromNbt(k)
        }

        // register chunk->key
        resolved.placementsByChunk.keys.forEach { chunkToKey.putIfAbsent(it, key) }
        return resolved
    }

    // helper that builds ResolvedStructure delegating to your existing parsing and transforms
    private fun generateResolvedFromNbt(key: ResolvedKey): ResolvedStructure<T> {
        val origin = Vec3(key.originX.toDouble(), key.originY.toDouble(), key.originZ.toDouble())
        SeededRandomSource(key.seed)

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
                .let { if (key.rotation != Rotation.NONE) it.rotate(key.rotation, origin) else it }
                .let { if (key.mirror != Mirror.NONE) it.mirror(key.mirror, origin) else it }

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

    override fun getSize(): BlockVec3i = structure?.size ?: throw NullPointerException("Structure $id does not exist")

    override fun place(world: BlockPlacer<T>, origin: Vec3, context: StructurePlacementContext): Boolean {
        val resolved = resolve(origin, context)
        resolved.placementsByChunk.values.flatten().forEach { p ->
            if (p.nbt != null)
                world.placeBlock(p.x, p.y, p.z, p.state as PlatformBlockState<T>?, p.nbt)
            else
                world.placeBlock(p.x, p.y, p.z, p.state as PlatformBlockState<T>?)
        }
        return true
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
    private val chunkToKey = ConcurrentHashMap<ChunkCoord, ResolvedKey>()
    private val resolvedCache: Cache<ResolvedKey, ResolvedStructure<BST>> = Caffeine.newBuilder()
        .maximumSize(256)
        .removalListener<ResolvedKey, ResolvedStructure<BST>> { key, value, _ ->
            if (key != null && value != null) {
                value.placementsByChunk.keys.forEach { chunk -> chunkToKey.remove(chunk, key) }
            }
        }.build()

    override fun cacheSize(): Long {
        return resolvedCache.estimatedSize()
    }

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
        val key = generateKey(origin, context, ResourceLocation.getEMPTY())

        // fast chunk->key lookup
        val originChunk = ChunkCoord.fromBlock(origin.x.toInt(), origin.z.toInt())
        chunkToKey[originChunk]?.let { existingKey ->
            resolvedCache.getIfPresent(existingKey)?.let { return it }
        }

        val resolved = resolvedCache.get(key) { k ->
            generateResolvedFromSchem(k)
        }

        // register chunk->key
        resolved.placementsByChunk.keys.forEach { chunkToKey.putIfAbsent(it, key) }
        return resolved
    }

    private fun generateResolvedFromSchem(key: ResolvedKey): ResolvedStructure<BST> {
        val origin = Vec3.of(key.originX.toDouble(), key.originY.toDouble(), key.originZ.toDouble())

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
                .let { if (key.rotation != Rotation.NONE) it.rotate(key.rotation, origin) else it }
                .let { if (key.mirror != Mirror.NONE) it.mirror(key.mirror, origin) else it }

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

class ProceduralStructure<T : ProceduralStructureGenerator.BaseBuilder<BST, *>, BST>(
    private val generator: T
) : PlatformStructure<BST>
{
    private val resolvedCache: Cache<ResolvedKey, ResolvedStructure<BST>> = Caffeine.newBuilder()
        .maximumSize(1024)
        .removalListener<ResolvedKey, ResolvedStructure<BST>> { key, value, _ ->
            if (key != null && value != null) {
                // remove any chunk -> key mappings that still point to this key
                value.placementsByChunk.keys.forEach { chunk ->

                }
            }
        }
        .build()
    private val generationCounter = AtomicInteger()

    fun resetGenerationCounter(): AtomicInteger {
        generationCounter.set(0)
        return generationCounter
    }

    override fun getSize(): BlockVec3i = generator.build().getApproximateSize()

    override fun cacheSize(): Long {
        return resolvedCache.estimatedSize()
    }

    override fun place(world: BlockPlacer<BST>, origin: Vec3, context: StructurePlacementContext): Boolean {
        val resolved = resolve(origin, context)
        resolved.placementsByChunk.values.flatten().forEach { p ->
            val pos = Vec3(p.x.toDouble(), p.y.toDouble(), p.z.toDouble())
            world.placeBlock(pos, p.state)
        }
        return true
    }

    private fun generateResolvedFromKey(key: ResolvedKey): ResolvedStructure<BST> {
        generationCounter.incrementAndGet()
        val origin = Vec3(key.originX.toDouble(), key.originY.toDouble(), key.originZ.toDouble())

        // Create a deterministic Random/RandomSource from the seed.
        // Replace `SeededRandomSource` with your engine's constructor — keep deterministic.
        val rnd = SeededRandomSource(key.seed) // <- adapt if your API differs

        // call generator
        val placements: List<BlockPlacement<BST>> = generator.build().generate(rnd, origin).placements

        // bucket by chunk and compute bounds
        val placementsByChunk = mutableMapOf<ChunkCoord, MutableList<BlockPlacement<BST>>>()
        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var minZ = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var maxY = Int.MIN_VALUE
        var maxZ = Int.MIN_VALUE

        placements.forEach { p ->
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
            key.resource
        )

        val finalMap = ConcurrentHashMap<ChunkCoord, List<BlockPlacement<BST>>>()
        placementsByChunk.forEach { (chunk, list) -> finalMap[chunk] = list.toList() }

        return ResolvedStructure(finalMap, bounds)
    }

    private val generationLimit = Semaphore(30)
    private val locks = SingleFlightCache<ResolvedKey, ResolvedStructure<BST>> { key ->
        resolvedCache.getIfPresent(key)
    }

    @Synchronized
    override fun resolve(origin: Vec3, context: StructurePlacementContext): ResolvedStructure<BST> {
        val resource = ResourceLocation.from("procedural", generator.javaClass.simpleName.lowercase())
        val key = generateKey(origin, context, resource)
        println(key)

        return locks.getOrGenerate(key) {
            generationLimit.acquire()
            println("Generating new structure for $it")
            val resolved = generateResolvedFromKey(it)
            resolvedCache.put(it, resolved)
            generationLimit.release()
            resolved
        }
    }

    override fun placeInChunk(
        world: BlockPlacer<BST>,
        chunkX: Int,
        chunkZ: Int,
        resolved: ResolvedStructure<BST>,
        context: StructurePlacementContext
    ): Boolean {
        val list = resolved.placementsByChunk[ChunkCoord(chunkX, chunkZ)] ?: return false
        list.forEach { p ->
            val pos = Vec3(p.x.toDouble(), p.y.toDouble(), p.z.toDouble())
            world.placeBlock(pos, p.state as PlatformBlockState<BST>?)
        }
        return true
    }
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

    override fun equals(other: Any?): Boolean {
        return if (other !is ChunkCoord) false
        else other.cx == this.cx && other.cz == this.cz
    }

    override fun hashCode(): Int {
        var result = cx
        result = 31 * result + cz
        return result
    }
}

data class BlockPlacement<T>(
    val x: Int,
    val y: Int,
    val z: Int,
    val state: PlatformBlockState<T>?,
    val nbt: ICompoundTag? = null
) {
    fun getVecPosition(): Vec3 = Vec3(x.toDouble(), y.toDouble(), z.toDouble())

    override fun equals(other: Any?): Boolean {
        if (other !is BlockPlacement<T>) return false
        return this.x == other.x && this.y == other.y && this.z == other.z
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        result = 31 * result + z
        result = 31 * result + (state?.hashCode() ?: 0)
        result = 31 * result + (nbt?.hashCode() ?: 0)
        return result
    }
}

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
    val seed: Long
) {
    override fun equals(other: Any?): Boolean {
        return if (other !is ResolvedKey) false
        else other.originX == originX && other.originY == originY && other.originZ == originZ &&
                other.rotation == rotation && other.mirror == mirror && other.resource == resource && other.seed == seed
    }

    override fun hashCode(): Int {
        var result = originX
        result = 31 * result + originY
        result = 31 * result + originZ
        result = 31 * result + seed.hashCode()
        result = 31 * result + resource.hashCode()
        result = 31 * result + rotation.hashCode()
        result = 31 * result + mirror.hashCode()
        return result
    }
}

class LruCache<K, V>(private val maxEntries: Int) {
    private val map = object : LinkedHashMap<K, V>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>): Boolean {
            return size > maxEntries
        }
    }

    @Synchronized
    operator fun get(k: K): V? = map[k]
    @Synchronized
    operator fun set(k: K, v: V) {
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

interface RandomSource : RandomGenerator {
    override fun nextInt(bound: Int): Int
    override fun nextInt(start: Int, bound: Int): Int
    override fun nextDouble(): Double
    override fun nextFloat(): Float
    override fun nextBoolean(): Boolean
    override fun nextLong(): Long
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

    override fun toString(): String = id

    override fun <P : Any?> getProperty(name: String?): P? {
        return name?.let { properties[it] as? P }
    }
}

class SimpleMaterial(private val loc: ResourceLocation) : PlatformMaterial {
    override fun isSolid(): Boolean = true
    override fun isAir(): Boolean = loc.path == "air"
    override fun getResourceLocation(): ResourceLocation? = loc
}

enum class VerticalPlacement {
    SKY, SURFACE, UNDERGROUND
}

data class StructureRule(
    val resource: ResourceLocation,
    val tags: StructureTag,
    val rarity: Rarity,
    val weight: Int,
    val frequency: Double,
    val spacing: Int,
    val fixedY: Int,
    val verticalPlacement: VerticalPlacement,
    val biomes: List<ResourceLocation>
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

        // snapshot rules
        val allRules = VSPEPlatformPlugin.structureManager().structures.values.map { it.second }

        ruleLoop@ for (rule in allRules) {
            // quick biome check (use the chunk center or provided location)
            val biome = context.biomeResolver.resolveBiome(chunkMinX + 8, 64, chunkMinZ + 8, context.random.getSeed())
            if (rule.biomes.contains { it == ResourceLocation.from(biome.identifier) }) continue

            val structurePair = VSPEPlatformPlugin.structureManager().getStructures()[rule.resource] ?: continue
            val structure = structurePair.first
            val baseSize = structure.getSize().toVec3()

            // cell dimension in blocks (rule.spacing is number of chunks between placements; if spacing is already in chunks, keep *16)
            val cellSizeBlocks = rule.spacing * 16

            // We'll consider every possible rotation/mirror placement because transformed size changes intersection range.
            // But if rotation/mirror is deterministic from seed we will compute inside cell handling below.

            // Compute minimal origin block X range (for any transformed orientation) that could intersect this chunk.
            // For conservative but tighter bound, use the maximum transformed size among rotations
            val possibleTSizeX = IntArray(4) // for four rotations
            val possibleTSizeZ = IntArray(4)
            for (rotIdx in 0 until 4) {
                val rot = when (rotIdx) {
                    0 -> Rotation.NONE
                    1 -> Rotation.CLOCKWISE_90
                    2 -> Rotation.CLOCKWISE_180
                    else -> Rotation.COUNTERCLOCKWISE_90
                }
                val transformed = transformSize(Vec3(0.0, 0.0, 0.0), baseSize, rot, Mirror.NONE)
                possibleTSizeX[rotIdx] = transformed.intX.toInt()
                possibleTSizeZ[rotIdx] = transformed.intZ.toInt()
            }
            val maxTSizeX = possibleTSizeX.maxOrNull() ?: baseSize.intX.toInt()
            val maxTSizeZ = possibleTSizeZ.maxOrNull() ?: baseSize.intZ.toInt()

            // Compute the origin block range that could intersect this chunk (origin is min corner)
            // Intersection condition: origin.x <= chunkMaxX && origin.x + sizeX - 1 >= chunkMinX
            val minOriginX = chunkMinX - (maxTSizeX - 1)
            val maxOriginX = chunkMaxX
            val minOriginZ = chunkMinZ - (maxTSizeZ - 1)
            val maxOriginZ = chunkMaxZ

            // Convert block origin range to cell range (floor division OK; use floorDiv helper for negatives)
            val minCellX = Math.floorDiv(minOriginX, cellSizeBlocks)
            val maxCellX = Math.floorDiv(maxOriginX, cellSizeBlocks)
            val minCellZ = Math.floorDiv(minOriginZ, cellSizeBlocks)
            val maxCellZ = Math.floorDiv(maxOriginZ, cellSizeBlocks)

            // iterate cells (this is now a small tight range)
            for (cellX in minCellX..maxCellX) {
                for (cellZ in minCellZ..maxCellZ) {
                    val key = CellKey(worldSeed, rule.resource, cellX, cellZ)
                    var placement = placementCache.get(key)

                    if (placement == null) {
                        // deterministic RNG for this cell
                        val seed = placementSeed(worldSeed, rule.resource, cellX, cellZ)
                        val rng = Random(seed)

                        if (rng.nextDouble() > rule.frequency) {
                            // no structure in this cell
                            placementCache.put(
                                key,
                                PlacementInfo.EMPTY
                            ) // optional sentinel to avoid repeating RNG (tweakable)
                            continue
                        }

                        // decide rotation/mirror deterministically
                        val rot = when (rng.nextInt(4)) {
                            0 -> Rotation.NONE
                            1 -> Rotation.CLOCKWISE_90
                            2 -> Rotation.CLOCKWISE_180
                            else -> Rotation.COUNTERCLOCKWISE_90
                        }
                        val mir = when (rng.nextInt(2)) {
                            0 -> Mirror.NONE
                            1 -> Mirror.FRONT_BACK
                            else -> Mirror.LEFT_RIGHT
                        }

                        val transformedSize = transformSize(Vec3(0.0, 0.0, 0.0), baseSize, rot, mir)
                        val tSizeX = transformedSize.intX.toInt()
                        val tSizeZ = transformedSize.intZ.toInt()

                        // now compute origin offsets inside cell so the structure remains within the cell spacing
                        val maxOffsetX = (cellSizeBlocks - tSizeX).coerceAtLeast(0)
                        val maxOffsetZ = (cellSizeBlocks - tSizeZ).coerceAtLeast(0)
                        val offsetX = if (maxOffsetX > 0) rng.nextInt(maxOffsetX + 1) else 0
                        val offsetZ = if (maxOffsetZ > 0) rng.nextInt(maxOffsetZ + 1) else 0

                        val originBlockX = cellX * cellSizeBlocks + offsetX
                        val originBlockZ = cellZ * cellSizeBlocks + offsetZ

                        // compute Y using a proper provider depending on rule (surface/underground/sky)
                        val originBlockY = computeOriginYForRule(rule, originBlockX, originBlockZ, context)

                        val originVec = Vec3(originBlockX.toDouble(), originBlockY.toDouble(), originBlockZ.toDouble())
                        val min = originVec
                        val max = originVec.offset(
                            (transformedSize.intX - 1).toInt(),
                            (transformedSize.intY - 1).toInt(),
                            (transformedSize.intZ - 1).toInt()
                        )
                        val sBox = StructureBox(min, max, rule.resource)

                        placement = PlacementInfo(
                            origin = originVec,
                            rotation = rot,
                            mirror = mir,
                            box = sBox,
                            rule = rule,
                            resolved = null
                        )

                        placementCache.put(key, placement)
                    } else {
                        // placement may be a sentinel "EMPTY" or existing record
                        if (placement === PlacementInfo.EMPTY) continue
                    }

                    // now check intersection with this chunk
                    placement = placementCache.get(key) ?: continue
                    if (placement.box.max.x < chunkMinX ||
                        placement.box.min.x > chunkMaxX ||
                        placement.box.max.z < chunkMinZ ||
                        placement.box.min.z > chunkMaxZ
                    ) {
                        continue
                    }

                    // avoid overlapping placements in this pass
                    if (structureBoxes.any { it.intersects(placement.box) }) {
                        continue
                    }

                    // ensure placement has a resolved structure so we can place parts for this chunk
                    resolvePlacementIfNeeded(placement, worldSeed)

                    // if still unresolved (owner may be racing), let owner handle it but don't skip drawing neighboring chunks:
                    // if unresolved, we can try to place with a locally resolved copy (resolve synchronously above),
                    // but in pathological race cases we just skip.
                    if (placement.resolved == null) {
                        // fallback: best-effort skip or optionally resolve here synchronously (we already attempted)
                        continue
                    }

                    structureBoxes.add(placement.box)
                    candidates.add(placement)
                } // cellZ
            } // cellX
        } // rules

        // order candidates then place
        candidates.sortedWith(compareBy({ getPlacementPriority(it.rule) }, { -it.rule.weight }))
            .forEach { p ->
                val tuple = VSPEPlatformPlugin.structureManager().getStructures()[p.rule.resource] ?: return@forEach

                @Suppress("UNCHECKED_CAST")
                val structureAny = tuple.first as PlatformStructure<Any?>

                // compute seeded random same as resolution (deterministic)
                val cellSizeBlocks = p.rule.spacing * 16
                val cellX = Math.floorDiv(p.origin.x.toInt(), cellSizeBlocks)
                val cellZ = Math.floorDiv(p.origin.z.toInt(), cellSizeBlocks)
                val seed = placementSeed(worldSeed, p.rule.resource, cellX, cellZ)
                val seededRand = Random(seed)
                val placementContext = StructurePlacementContext(
                    random = SeededRandomSource(seededRand.nextLong()),
                    rotation = p.rotation,
                    mirror = p.mirror
                )

                val platformOrigin = context.locationProvider.invoke(
                    p.origin.x.toInt(), p.origin.y.toInt(), p.origin.z.toInt()
                )

                @Suppress("UNCHECKED_CAST")
                val resolvedAny = p.resolved as ResolvedStructure<Any?>? ?: run {
                    // fallback: if non-owner created placement but resolved is missing — synchronously resolve and cache now
                    val ctxRand = SeededRandomSource(seededRand.nextLong())
                    val placementCtx =
                        StructurePlacementContext(random = ctxRand, rotation = p.rotation, mirror = p.mirror)
                    val resolved = structureAny.resolve(platformOrigin, placementCtx) as ResolvedStructure<Any?>
                    p.resolved = resolved
                    resolved
                }

                @Suppress("UNCHECKED_CAST")
                val placerAny = context.blockPlacer as BlockPlacer<Any?>
                structureAny.placeInChunk(placerAny, chunkX, chunkZ, resolvedAny, placementContext)
            }
    }

    // --- helpers below ---
    // Determine Y for a structure rule; adapt to your StructureRule shape (I assume it has 'verticalPlacement' or similar)
    private fun computeOriginYForRule(
        rule: StructureRule,
        originX: Int,
        originZ: Int,
        context: ChunkGenerateContext<T, *>
    ): Int {
        // Examples of rule vertical type: "SURFACE", "UNDERGROUND", "SKY", or explicit min/max depth values.
        // Adapt the condition below to your actual rule fields.
        return when (rule.verticalPlacement) {
            VerticalPlacement.SURFACE -> {
                context.blockPlacer.getHighestBlockAt(originX, originZ)
            }

            VerticalPlacement.UNDERGROUND -> {
                val surf = context.blockPlacer.getHighestBlockAt(originX, originZ)
                val depth = rule.fixedY.coerceAtLeast(-50)
                (surf - (1 + (abs((originX * 341873128712L + originZ * 132897987541L) % depth).toInt()))).coerceAtLeast(
                    8
                )
            }

            VerticalPlacement.SKY -> {
                rule.fixedY
            }
        }
    }

    // thread-safe attempt to ensure placement.resolved is available (resolves once per cell)
    private fun resolvePlacementIfNeeded(placement: PlacementInfo, worldSeed: Long) {
        // if someone else resolved it already, quick return
        if (placement.resolved != null) return

        // synchronize on the placement object to ensure only one resolver runs
        synchronized(placement) {
            if (placement.resolved != null) return

            // owner should perform the resolve ideally; but to guarantee chunk rendering we resolve here as well.
            val tuple = VSPEPlatformPlugin.structureManager().getStructures()[placement.rule.resource] ?: return

            @Suppress("UNCHECKED_CAST")
            val structureAny = tuple.first as PlatformStructure<Any?>

            val cellSizeBlocks = placement.rule.spacing * 16
            val cellX = Math.floorDiv(placement.origin.x.toInt(), cellSizeBlocks)
            val cellZ = Math.floorDiv(placement.origin.z.toInt(), cellSizeBlocks)
            val seed = placementSeed(worldSeed, placement.rule.resource, cellX, cellZ)
            val seededRand = Random(seed)
            val placementContext = StructurePlacementContext(
                random = SeededRandomSource(seededRand.nextLong()),
                rotation = placement.rotation,
                mirror = placement.mirror
            )

            val platformOrigin = placement.origin

            // resolve and cache
            val resolved = structureAny.resolve(platformOrigin, placementContext) as ResolvedStructure<Any?>
            placement.resolved = resolved
        }
    }

    // reuse transformSize and getPlacementPriority from your class (keep as-is)
    private fun getPlacementPriority(rule: StructureRule): Int {
        return when {
            StructureTag.ANCIENT == rule.tags -> 0
            StructureTag.DUNGEON == rule.tags -> 1
            StructureTag.VILLAGE == rule.tags -> 2
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
open class PlacementInfo(
    val origin: Vec3,                // absolute block origin in world blocks (min corner)
    val rotation: Rotation,
    val mirror: Mirror,
    val box: StructureBox,
    val rule: StructureRule,
    // volatile so other threads may see it after owner sets it
    @Volatile var resolved: ResolvedStructure<Any?>? = null
) {
    object EMPTY : PlacementInfo(
        Vec3(0.0, 0.0, 0.0),
        Rotation.NONE,
        Mirror.NONE,
        StructureBox(Vec3(0.0, 0.0, 0.0), Vec3(0.0, 0.0, 0.0), ResourceLocation.getEMPTY()),
        StructureRule(
            ResourceLocation.getEMPTY(), StructureTag.EMPTY, Rarity.COMMON,
            0, 0.0, 0, 0, VerticalPlacement.SURFACE,
            listOf<ResourceLocation>()
        ),
    )
}
class StructurePlacementCache {
    private val map = ConcurrentHashMap<CellKey, PlacementInfo>()

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