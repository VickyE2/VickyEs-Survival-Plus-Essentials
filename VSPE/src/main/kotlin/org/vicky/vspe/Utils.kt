package org.vicky.vspe

import org.vicky.platform.utils.Mirror
import org.vicky.platform.utils.Rotation
import org.vicky.platform.utils.Vec3
import org.vicky.platform.world.PlatformBlockState
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.RandomSource
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors
import kotlin.math.sqrt
import kotlin.random.Random

fun <T> List<T>.weightedRandomOrNull(weightProvider: (T) -> Int): T? {
    if (isEmpty()) return null
    val totalWeight = sumOf(weightProvider)
    if (totalWeight <= 0) return null

    val r = Random.nextInt(totalWeight)
    var cumulative = 0
    for (item in this) {
        cumulative += weightProvider(item)
        if (r < cumulative) return item
    }
    return null // shouldn't happen unless totalWeight is messed up
}

fun distance(
    temp1: Double,
    humid1: Double,
    elev1: Double,
    temp2: Double,
    humid2: Double,
    elev2: Double,
    tempWeight: Double = 1.0,
    humidWeight: Double = 1.0,
    elevWeight: Double = 1.0
): Double {
    val dt = (temp1 - temp2) * tempWeight
    val dh = (humid1 - humid2) * humidWeight
    val de = (elev1 - elev2) * elevWeight

    return sqrt(dt * dt + dh * dh + de * de)
}

enum class StructureTag {
    RUINS, DUNGEON, VILLAGE, OCEAN, SKY, NETHER, FROZEN, ANCIENT
}

typealias PaletteFunction = (
    pos: Vec3,
    height: Int,
    distanceFromCenter: Int,
    random: RandomSource
) -> PlatformBlockState<*>

enum class BiomeCategory {
    COAST, WARM_OCEAN, WARM_DEEP_OCEAN, COLD_OCEAN, COLD_DEEP_OCEAN, SNOWY_BEACH, LUSH_CAVES, PLAINS, FOREST, DESERT, MOUNTAIN, OCEAN, DEEP_OCEAN, RIVER, SWAMP, TAIGA, SAVANNA, TUNDRA, NETHER, END, MESA, ICY, JUNGLE, RAINFOREST, WETLAND;
}

fun <T> List<T>.contains(transform: (T) -> Boolean) : Boolean {
    for (e in this) {
        if (transform(e)) {
            return true
        }
    }
    return false
}

enum class PrecipitationType {
    NONE, RAIN, SNOW
}

fun Vec3.rotate(rotation: Rotation, origin: Vec3): Vec3 {
    val x = this.x - origin.x
    val z = this.z - origin.z

    return when (rotation) {
        Rotation.NONE -> this
        Rotation.CLOCKWISE_90 -> Vec3(origin.x - z, this.y, origin.z + x)
        Rotation.CLOCKWISE_180 -> Vec3(origin.x - x, this.y, origin.z - z)
        Rotation.COUNTERCLOCKWISE_90 -> Vec3(origin.x + z, this.y, origin.z - x)
    }
}



fun getAllZipFiles(directoryPath: String): List<Path> {
    val dirPath:Path = Paths.get(directoryPath);
    if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
        throw IllegalArgumentException("Invalid directory path: $directoryPath")
    }
    return Files.walk(dirPath).filter { path -> path.toString().lowercase().endsWith(".zip") }.collect(Collectors.toList())
}

fun Vec3.mirror(mirror: Mirror, origin: Vec3): Vec3 {
    return when (mirror) {
        Mirror.NONE -> this
        Mirror.FRONT_BACK -> Vec3(this.x, this.y, origin.z * 2 - this.z)
        Mirror.LEFT_RIGHT -> Vec3(origin.x * 2 - this.x, this.y, this.z)
    }
}

fun BlockVec3i.toVec3(): Vec3 {
    return Vec3(this.x.toDouble(), this.y.toDouble(), this.z.toDouble())
}

internal fun <T: Vec3> T.offset(
    x: Integer,
    y: Integer,
    z: Integer
): Vec3 {
    return Vec3(this.x + x.toDouble(), this.y + y.toDouble(), this.z + z.toDouble())
}

internal fun <T: Vec3> T.offset(
    x: Int,
    y: Int,
    z: Int
): Vec3 {
    return Vec3(this.x + x.toDouble(), this.y + y.toDouble(), this.z + z.toDouble())
}

data class BlockVec3i(val x: Int, val y: Int, val z: Int) {

    constructor(x: Integer, y: Integer, z: Integer) : this(x.toInt(), y.toInt(), z.toInt())

    operator fun plus(other: BlockVec3i): BlockVec3i =
        BlockVec3i(x + other.x, y + other.y, z + other.z)

    operator fun minus(other: BlockVec3i): BlockVec3i =
        BlockVec3i(x - other.x, y - other.y, z - other.z)

    operator fun times(scalar: Int): BlockVec3i =
        BlockVec3i(x * scalar, y * scalar, z * scalar)

    fun toChunkPos(): Pair<Int, Int> =
        x.shr(4) to z.shr(4)

    fun rotate(rotation: Rotation): BlockVec3i =
        when (rotation) {
            Rotation.NONE -> this
            Rotation.CLOCKWISE_90 -> BlockVec3i(-z, y, x)
            Rotation.CLOCKWISE_180 -> BlockVec3i(-x, y, -z)
            Rotation.COUNTERCLOCKWISE_90 -> BlockVec3i(z, y, -x)
        }

    fun mirror(mirror: Mirror): BlockVec3i =
        when (mirror) {
            Mirror.NONE -> this
            Mirror.LEFT_RIGHT -> BlockVec3i(-x, y, z)
            Mirror.FRONT_BACK -> BlockVec3i(x, y, -z)
        }

    override fun toString(): String = "($x, $y, $z)"
}

enum class Direction(val dx: Int, val dz: Int) {
    NORTH(0, -1),
    SOUTH(0, 1),
    EAST(1, 0),
    WEST(-1, 0),
    NORTHEAST(1, -1),
    NORTHWEST(-1, -1),
    SOUTHEAST(1, 1),
    SOUTHWEST(-1, 1)
}
