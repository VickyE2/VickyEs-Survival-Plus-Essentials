package org.vicky.vspe.structure_gen

import net.minecraft.world.level.block.Rotation
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.Directional
import org.vicky.vspe.Direction
import kotlin.math.*
import kotlin.random.Random

abstract class RoadLayoutEngine(
    val strategy: RoadLayoutStrategy // You can plug different layout strategies here
)
{
    /**
     * Converts a basic "is road at x,z" map into a map of Vec3 -> ROAD metadata.
     */
    fun generateRoadAndBuildingMap(
        config: CityConfig,
        buildingSizes: Map<String, Int>,
        specialBuildings: Map<String, Int>, // name -> size
        decorators: Map<String, Pair<DecoratorType, Position>>, // name -> type: by_road, in_cross, random
        heightMap: Map<Pair<Int, Int>, Int>,
        arrangement: ArrangementType,
        centerX: Int,
        centerZ: Int,
        radius: Int,
        thickness: Int,
        length: Int,
        shape: Shape
    ): Triple<List<RoadPlacement>, Map<BuildingPlacement, Rotation>, List<DecoratorPlacement>> {

        val roadPlacements = mutableListOf<RoadPlacement>()
        val buildingPlacements = mutableMapOf<BuildingPlacement, Rotation>()

        val buildingCoords = mutableSetOf<Pair<Int, Int>>()
        val rawRoadMap = mutableMapOf<Pair<Int, Int>, Boolean>()

        // 🏛 TownHall ring first
        if (config.hasTownHall) {
            val spacing = config.townHallSpacing ?: 20
            val baseRingSize = spacing + 1
            for (x in (centerX - baseRingSize)..(centerX + baseRingSize)) {
                for (z in (centerZ - baseRingSize)..(centerZ + baseRingSize)) {
                    if (!shape.isInside(x, z, centerX, centerZ, radius)) continue
                    if (x == centerX - baseRingSize || x == centerX + baseRingSize ||
                        z == centerZ - baseRingSize || z == centerZ + baseRingSize) {
                        rawRoadMap[x to z] = true
                    }
                }
            }
        }

        // 🛣 Build structured road frame using `length`
        for ((x, z) in heightMap.keys) {
            if (!shape.isInside(x, z, centerX, centerZ, radius)) continue

            val dx = x - centerX
            val dz = z - centerZ

            val isMainLane = when (arrangement) {
                ArrangementType.GRID -> {
                    (dx % (length + thickness) < thickness) ||
                            (dz % (length + thickness) < thickness)
                }
                ArrangementType.CENTRALIZED -> dx == 0 || dz == 0
                ArrangementType.SPIRAL -> {
                    val dist = sqrt((dx * dx + dz * dz).toDouble()).toInt()
                    (dist % (length + thickness) < thickness)
                }
                ArrangementType.CIRCULAR -> {
                    val dist = sqrt((dx * dx + dz * dz).toDouble()).toInt()
                    (dist % (length + thickness) in 0 until thickness)
                }
                else -> false
            }

            if (isMainLane) rawRoadMap[x to z] = true
        }


        val roadCoords = rawRoadMap.keys.toSet()

        // 🪝 Detect connections and force corners / junctions
        for ((x, z) in roadCoords) {
            val connections = getConnections(rawRoadMap, x, z)
            val roadType = classifyRoadType(connections)
            val rotation = getRotationFor(roadType)

            // priority: corners & junctions override straights
            roadPlacements.add(
                RoadPlacement(
                    type = roadType,
                    rotation = rotation,
                    x = x,
                    z = z
                )
            )
        }

        val decoratorPlacements = mutableListOf<DecoratorPlacement>()

        roadPlacements.forEach { road ->
            decorators.forEach { (name, data) ->
                when (data.first) {
                    DecoratorType.BY_ROAD -> {
                        // decide offset based on a road type / rotation
                        val (offsetX, offsetZ) = when (road.type) {
                            RoadType.STRAIGHT_NS -> { // road runs North–South → offset left/right on X
                                when (data.second) {
                                    Position.CENTER -> 0 to 0
                                    Position.LEFT -> -thickness to 0
                                    Position.RIGHT -> thickness to 0
                                }
                            }

                            RoadType.STRAIGHT_EW -> { // road runs East–West → offset left/right on Z
                                when (data.second) {
                                    Position.CENTER -> 0 to 0
                                    Position.LEFT -> 0 to -thickness
                                    Position.RIGHT -> 0 to thickness
                                }
                            }

                            else -> 0 to 0 // fallback: don’t offset
                        }

                        decoratorPlacements += DecoratorPlacement(
                            type = name,
                            x = road.x + offsetX,
                            z = road.z + offsetZ,
                            rotation = road.rotation // or NONE if the decor doesn't rotate
                        )
                    }

                    DecoratorType.CROSS -> if (road.type.name.startsWith("CROSS") && Random.nextFloat() < 0.3f) {
                        decoratorPlacements += DecoratorPlacement(name, road.x, road.z, Rotation.NONE)
                    }

                    DecoratorType.RANDOM -> if (Random.nextFloat() < 0.05f) {
                        decoratorPlacements += DecoratorPlacement(name, road.x, road.z, Rotation.NONE)
                    }
                }
            }

            // Optionally place side decoration under / opposite lamp 50/50
            if (Random.nextFloat() > 0.2f) {
                decoratorPlacements += DecoratorPlacement("side_bench", road.x, road.z, Rotation.NONE)
            }
        }

        // find road ends
        val roadEnds = roadPlacements.filter { it.type.name.startsWith("DEAD_END") }

        specialBuildings.forEach { (name, size) ->
            val placed = roadEnds.firstOrNull { end ->
                val coord = end.x to end.z
                // is space free near?
                !buildingCoords.contains(coord)
            } ?: run {
                // fallback: find empty building slot
                heightMap.keys.firstOrNull { (x, z) ->
                    !roadCoords.contains(x to z) &&
                            !buildingCoords.contains(x to z) &&
                            !isNearAnything(x, z, roadCoords, config.roadPadding)
                }?.let { pos -> RoadPlacement(pos.first, pos.second, RoadType.DEAD_END_N, Rotation.NONE) }
            }
            if (placed != null) {
                buildingPlacements[
                    BuildingPlacement(size = size, type=BuildingType.SPECIAL, x=placed.x, z=placed.z, meta=mapOf("name" to name))
                ] = Rotation.NONE
                buildingCoords += placed.x to placed.z
            }
        }


        // 🏢 Place buildings respecting spacing & roads
        for ((x, z) in heightMap.keys) {
            if (!shape.isInside(x, z, centerX, centerZ, radius)) continue
            val coord = x to z

            if (coord in roadCoords) continue

            val nearRoad = isNearAnything(x, z, roadCoords, config.roadPadding)
            if (nearRoad) continue

            val nearBuilding = isNearAnything(x, z, buildingCoords, config.buildingPadding)
            if (nearBuilding) continue

            if (x == centerX && z == centerZ && config.hasTownHall) {
                val size = buildingSizes["TOWNHALL"] ?: 20
                buildingPlacements[
                    BuildingPlacement(
                        size = size,
                        type = BuildingType.TOWNHALL,
                        x = x,
                        z = z
                    )
                ] = Rotation.NONE
                buildingCoords.add(coord)
            } else {
                val minSize = config.buildingMinSpacing
                val maxSize = config.buildingMaxSpacing
                val chosenSize = (minSize..maxSize).random()

                val nearestRoad = findNearestRoadDirection(x, z, roadCoords)
                val rotation = nearestRoad?.toRotation() ?: Rotation.NONE

                buildingPlacements[
                    BuildingPlacement(
                        size = chosenSize,
                        type = BuildingType.HOUSE,
                        x = x,
                        z = z,
                        meta = mapOf("size" to chosenSize)
                    )
                ] = rotation

                buildingCoords.add(coord)
            }
        }

        return Triple(roadPlacements, buildingPlacements, decoratorPlacements)
    }

    private fun findNearestRoadDirection(
        x: Int,
        z: Int,
        roadCoords: Set<Pair<Int, Int>>,
        maxDistance: Int = 5
    ): Direction? {
        for (d in 1..maxDistance) {
            for (dir in Direction.entries) {
                val nx = x + dir.dx * d
                val nz = z + dir.dz * d
                if (nx to nz in roadCoords) {
                    return dir
                }
            }
        }
        return null
    }


    /**
     * Gets all connected directions from this position, using spacing distance.
     */
    private fun getConnections(
        roadMap: Map<Pair<Int, Int>, Boolean>,
        x: Int,
        z: Int,
        spacing: Int = 1
    ): Set<Direction> {
        return Direction.entries.filter { dir ->
            roadMap[x + dir.dx * spacing to z + dir.dz * spacing] == true
        }.toSet()
    }

    fun getConnections(
        roadPlacements: List<RoadPlacement>,
        x: Int,
        z: Int,
        spacing: Int = 1
    ): Set<Direction> {
        val roadCords = roadPlacements.map { Vec2(it.x, it.z) }.toSet()

        return Direction.entries.filter { dir ->
            val nx = x + dir.dx * spacing
            val nz = z + dir.dz * spacing
            Vec2(nx, nz) in roadCords
        }.toSet()
    }


    /**
     * Classifies the road tile by its connection shape.
     */
    fun classifyRoadType(connections: Set<Direction>): RoadType {
        return when (connections) {
            setOf(Direction.NORTH, Direction.SOUTH) -> RoadType.STRAIGHT_NS
            setOf(Direction.EAST, Direction.WEST) -> RoadType.STRAIGHT_EW
            setOf(Direction.NORTHEAST, Direction.SOUTHWEST) -> RoadType.STRAIGHT_NE_SW
            setOf(Direction.NORTHWEST, Direction.SOUTHEAST) -> RoadType.STRAIGHT_NW_SE

            setOf(Direction.NORTH, Direction.EAST) -> RoadType.CORNER_NE
            setOf(Direction.NORTH, Direction.WEST) -> RoadType.CORNER_NW
            setOf(Direction.SOUTH, Direction.EAST) -> RoadType.CORNER_SE
            setOf(Direction.SOUTH, Direction.WEST) -> RoadType.CORNER_SW

            setOf(Direction.NORTH) -> RoadType.DEAD_END_N
            setOf(Direction.SOUTH) -> RoadType.DEAD_END_S
            setOf(Direction.EAST) -> RoadType.DEAD_END_E
            setOf(Direction.WEST) -> RoadType.DEAD_END_W
            setOf(Direction.NORTHEAST) -> RoadType.DEAD_END_NE
            setOf(Direction.NORTHWEST) -> RoadType.DEAD_END_NW
            setOf(Direction.SOUTHEAST) -> RoadType.DEAD_END_SE
            setOf(Direction.SOUTHWEST) -> RoadType.DEAD_END_SW

            setOf(Direction.NORTH, Direction.EAST, Direction.WEST) -> RoadType.T_NORTH
            setOf(Direction.SOUTH, Direction.EAST, Direction.WEST) -> RoadType.T_SOUTH
            setOf(Direction.NORTH, Direction.SOUTH, Direction.EAST) -> RoadType.T_EAST
            setOf(Direction.NORTH, Direction.SOUTH, Direction.WEST) -> RoadType.T_WEST

            setOf(Direction.NORTHEAST, Direction.NORTHWEST, Direction.SOUTHWEST) -> RoadType.T_NE
            setOf(Direction.NORTHWEST, Direction.SOUTHWEST, Direction.SOUTHEAST) -> RoadType.T_NW
            setOf(Direction.SOUTHWEST, Direction.SOUTHEAST, Direction.NORTHEAST) -> RoadType.T_SW
            setOf(Direction.SOUTHEAST, Direction.NORTHEAST, Direction.NORTHWEST) -> RoadType.T_SE

            setOf(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST) -> RoadType.CROSS
            setOf(Direction.NORTHEAST, Direction.NORTHWEST, Direction.SOUTHEAST, Direction.SOUTHWEST) -> RoadType.CROSS_DIAG

            else -> RoadType.DEAD_END_N
        }
    }

    /**
     * Based on direction set, estimate rotation.
     */
    fun getRotationFor(type: RoadType): Rotation {
        // You can improve this further with a table lookup
        return when (type) {
            RoadType.STRAIGHT_NS -> Rotation.NONE
            RoadType.STRAIGHT_EW -> Rotation.CLOCKWISE_90
            RoadType.CORNER_NE -> Rotation.NONE
            RoadType.CORNER_NW -> Rotation.COUNTERCLOCKWISE_90
            RoadType.CORNER_SE -> Rotation.CLOCKWISE_90
            RoadType.CORNER_SW -> Rotation.CLOCKWISE_180
            RoadType.T_NORTH -> Rotation.NONE
            RoadType.T_SOUTH -> Rotation.CLOCKWISE_180
            RoadType.T_EAST -> Rotation.CLOCKWISE_90
            RoadType.T_WEST -> Rotation.COUNTERCLOCKWISE_90
            RoadType.CROSS -> Rotation.NONE
            else -> Rotation.NONE // Default for simplicity
        }
    }

    private fun isNearAnything(x: Int, z: Int, targets: Set<Pair<Int, Int>>, spacing: Int): Boolean {
        for (dx in -spacing..spacing) {
            for (dz in -spacing..spacing) {
                if (dx == 0 && dz == 0) continue
                if ((x + dx) to (z + dz) in targets) return true
            }
        }
        return false
    }

    fun townhallPosition(x: Int, z: Int, centerX: Int, centerZ: Int, config: CityConfig): Boolean {
        val spacing = config.townHallSpacing ?: 20 // default size
        return x in (centerX - spacing)..(centerX + spacing) &&
                z in (centerZ - spacing)..(centerZ + spacing)
    }
}

enum class Position {
    LEFT,
    RIGHT,
    CENTER
}

enum class DecoratorType {
    BY_ROAD,
    CROSS,
    RANDOM
}

class DistributedRoadLayoutEngine @JvmOverloads constructor(
    strategy: RoadLayoutStrategy,
    private val distributionInformation: Map<String, Pair<Double, Double>> = emptyMap()
) : RoadLayoutEngine(strategy)

sealed class RoadLayoutStrategy {
    abstract fun shouldPlaceRoad(
        x: Int, z: Int,
        centerX: Int, centerZ: Int,
        radius: Int,
        thickness: Int,
        shape: Shape
    ): Boolean

    data object Centered : RoadLayoutStrategy() {
        override fun shouldPlaceRoad(x: Int, z: Int, centerX: Int, centerZ: Int, radius: Int, thickness: Int, shape: Shape): Boolean {
            val dx = x - centerX
            val dz = z - centerZ
            return dx % (thickness * 3) < thickness || dz % (thickness * 3) < thickness
        }
    }

    data object Distributed : RoadLayoutStrategy() {
        override fun shouldPlaceRoad(x: Int, z: Int, centerX: Int, centerZ: Int, radius: Int, thickness: Int, shape: Shape): Boolean {
            return Random.nextFloat() < 0.1f
        }
    }

    data object Branched : RoadLayoutStrategy() {
        override fun shouldPlaceRoad(x: Int, z: Int, centerX: Int, centerZ: Int, radius: Int, thickness: Int, shape: Shape): Boolean {
            val dx = x - centerX
            val dz = z - centerZ
            val angle = atan2(dz.toDouble(), dx.toDouble())
            val dist = sqrt((dx * dx + dz * dz).toDouble())
            val branchSpacing = Math.PI / 6
            val branchIndex = (angle / branchSpacing).roundToInt()
            val snappedAngle = branchIndex * branchSpacing
            val branchDx = cos(snappedAngle) * dist
            val branchDz = sin(snappedAngle) * dist
            val snappedX = (branchDx + centerX).roundToInt()
            val snappedZ = (branchDz + centerZ).roundToInt()
            return abs(x - snappedX) <= thickness / 2 && abs(z - snappedZ) <= thickness / 2
        }
    }

    data object Rooted : RoadLayoutStrategy() {
        override fun shouldPlaceRoad(x: Int, z: Int, centerX: Int, centerZ: Int, radius: Int, thickness: Int, shape: Shape): Boolean {
            val dx = x - centerX
            val dz = z - centerZ
            val manhattan = abs(dx) + abs(dz)
            return manhattan % (thickness * 3) < thickness
        }
    }
}


data object BranchingLayoutStrategy : RoadLayoutStrategy() {
    override fun shouldPlaceRoad(
        x: Int, z: Int, centerX: Int, centerZ: Int,
        radius: Int, thickness: Int, shape: Shape
    ): Boolean {
        val dx = x - centerX
        val dz = z - centerZ
        val dist = sqrt((dx * dx + dz * dz).toDouble())

        val angle = atan2(dz.toDouble(), dx.toDouble()) // radians
        val branchSpacing = PI / 6 // every 30 degrees

        // Snap to branch directions
        val branchIndex = (angle / branchSpacing).roundToInt()
        val snappedAngle = branchIndex * branchSpacing
        val branchDx = cos(snappedAngle) * dist
        val branchDz = sin(snappedAngle) * dist

        val snappedX = (branchDx + centerX).roundToInt()
        val snappedZ = (branchDz + centerZ).roundToInt()

        return abs(x - snappedX) <= thickness / 2 && abs(z - snappedZ) <= thickness / 2
    }
}

data object GridLayoutStrategy : RoadLayoutStrategy() {
    override fun shouldPlaceRoad(
        x: Int, z: Int, centerX: Int, centerZ: Int,
        radius: Int, thickness: Int, shape: Shape
    ): Boolean {
        val dx = x - centerX
        val dz = z - centerZ
        return dx % (thickness * 3) < thickness || dz % (thickness * 3) < thickness
    }
}


data object SpiralLayoutStrategy : RoadLayoutStrategy() {
    override fun shouldPlaceRoad(
        x: Int, z: Int, centerX: Int, centerZ: Int,
        radius: Int, thickness: Int, shape: Shape
    ): Boolean {
        val dx = x - centerX
        val dz = z - centerZ
        val r = sqrt((dx * dx + dz * dz).toDouble())
        val theta = atan2(dz.toDouble(), dx.toDouble())

        val spiral = theta * 10 + r // spiral equation approx
        return spiral % (thickness * 2) < thickness
    }
}
open class Positioned(
    open val x: Int,
    open val z: Int
)

data class RoadPlacement(
    override val x: Int,
    override val z: Int,
    val type: RoadType,
    val rotation: Rotation,
    val meta: Map<String, Any> = emptyMap()
) : Positioned(x, z)

data class DecoratorPlacement (
    val type: String,
    override val x: Int,
    override val z: Int,
    val rotation: Rotation
) : Positioned(x, z)

data class BuildingPlacement(
    override val x: Int,
    override val z: Int,
    val size: Int,
    val type: BuildingType,
    val meta: Map<String, Any> = emptyMap()
) : Positioned(x, z)

class ModularRoadPlacer5000 {
    val roadTemplates = mutableMapOf(
        RoadType.CROSS to RoadTemplate(
            listOf(
                ".||#||.",
                "...#...",
                "##o#o##",
                "...#...",
                ".||#||."
            )
        ),
        RoadType.STRAIGHT_NS to RoadTemplate(
            listOf(
                "...#...",
                "...#...",
                "...#...",
                "...#...",
                "...#..."
            )
        ),
        RoadType.STRAIGHT_EW to RoadTemplate(
            listOf(
                ".......",
                "#######",
                "......."
            )
        ),
        RoadType.CORNER_NE to RoadTemplate(
            listOf(
                "...####",
                "...#...",
                "...#...",
                "...#...",
                "...#..."
            )
        ),
        RoadType.CORNER_NW to RoadTemplate(
            listOf(
                "###....",
                "...#...",
                "...#...",
                "...#...",
                "...#..."
            )
        ),
        RoadType.CORNER_SE to RoadTemplate(
            listOf(
                "...#...",
                "...#...",
                "...#...",
                "...#...",
                "###...."
            )
        ),
        RoadType.CORNER_SW to RoadTemplate(
            listOf(
                "...#...",
                "...#...",
                "...#...",
                "...#...",
                "...###."
            )
        ),
        RoadType.T_NORTH to RoadTemplate(
            listOf(
                "#######",
                "...#...",
                "...#...",
                "...#...",
                "...#..."
            )
        ),
        RoadType.T_SOUTH to RoadTemplate(
            listOf(
                "...#...",
                "...#...",
                "...#...",
                "...#...",
                "#######"
            )
        ),
        RoadType.T_EAST to RoadTemplate(
            listOf(
                "...#...",
                "...#...",
                "...###.",
                "...#...",
                "...#..."
            )
        ),
        RoadType.T_WEST to RoadTemplate(
            listOf(
                "...#...",
                "...#...",
                ".###...",
                "...#...",
                "...#..."
            )
        ),
        RoadType.DEAD_END_N to RoadTemplate(
            listOf(
                "...#...",
                "...#...",
                "...#...",
                "...#...",
                "......."
            )
        ),
        RoadType.DEAD_END_S to RoadTemplate(
            listOf(
                ".......",
                "...#...",
                "...#...",
                "...#...",
                "...#..."
            )
        ),
        RoadType.DEAD_END_E to RoadTemplate(
            listOf(
                "...#...",
                "...#...",
                "...#...",
                "...#...",
                "...###."
            )
        ),
        RoadType.DEAD_END_W to RoadTemplate(
            listOf(
                "...#...",
                "...#...",
                "...#...",
                "...#...",
                ".###..."
            )
        ),
        RoadType.DEAD_END_NE to RoadTemplate(
            listOf(
                "###....",
                "...#...",
                "...#...",
                "...#...",
                "......."
            )
        ),
        RoadType.DEAD_END_NW to RoadTemplate(
            listOf(
                "....###",
                "...#...",
                "...#...",
                "...#...",
                "......."
            )
        ),
        RoadType.DEAD_END_SW to RoadTemplate(
            listOf(
                ".......",
                "...#...",
                "...#...",
                "...#...",
                "....###"
            )
        ),
        RoadType.DEAD_END_SE to RoadTemplate(
            listOf(
                ".......",
                "...#...",
                "...#...",
                "...#...",
                "###...."
            )
        ),
        RoadType.T_NE to RoadTemplate(
            listOf(
                "###.###",
                "...#...",
                "...#...",
                "...#...",
                "...#..."
            )
        ),
        RoadType.T_NW to RoadTemplate(
            listOf(
                "###.###",
                "...#...",
                "...#...",
                "...#...",
                "...#..."
            )
        ),
        RoadType.T_SE to RoadTemplate(
            listOf(
                "...#...",
                "...#...",
                "...#...",
                "...#...",
                "###.###"
            )
        ),
        RoadType.T_SW to RoadTemplate(
            listOf(
                "...#...",
                "...#...",
                "...#...",
                "...#...",
                "###.###"
            )
        ),
        RoadType.STRAIGHT_NE_SW to RoadTemplate(
            listOf(
                "..#....",
                "...#...",
                "....#..",
                "...#...",
                "..#...."
            )
        ),
        RoadType.STRAIGHT_NW_SE to RoadTemplate(
            listOf(
                "....#..",
                "...#...",
                "..#....",
                "...#...",
                "....#.."
            )
        ),
        RoadType.CROSS_DIAG to RoadTemplate(
            listOf(
                "#.....#",
                ".#...#.",
                "..#.#..",
                "...#...",
                "..#.#..",
                ".#...#.",
                "#.....#"
            )
        )
    )

    val DefaultRoadPalette = RoadPalette(
        roadBlock = Material.BLACK_CONCRETE.createBlockData(),
        altRoadBlock = Material.GRAY_CONCRETE.createBlockData(),
        sidewalkBlock = Material.YELLOW_CONCRETE.createBlockData(),
        stair = Material.STONE_SLAB.createBlockData(),
        sidewalkThickness = 2
    )

    val symbolMap = mapOf<Char, (PlacementContext, Vec3) -> Unit>(
        '#' to { ctx, pos -> ctx.placeBlock(pos, ctx.palette.roadBlock) },
        'o' to { ctx, pos -> ctx.placeBlock(pos, ctx.palette.altRoadBlock) },
        '.' to { _, _ -> },
        '|' to { ctx, pos -> ctx.placeSidewalk(pos, ctx.rotation) }
    )

    fun placeRoad(world: World, pos: Vec3, roadType: RoadType, palette: RoadPalette, rotation: Rotation) {
        val template = roadTemplates[roadType] ?: return
        val context = PlacementContext(world, palette, rotation)
        template.apply(context, pos, symbolMap)
    }

    data class PlacementContext(
        val world: World,
        val palette: RoadPalette,
        val rotation: Rotation
    ) {
        fun placeBlock(pos: Vec3, block: BlockData) {
            world.setBlock(pos, block)
        }

        fun placeSidewalk(pos: Vec3, facing: Rotation) {
            val stairFacing = facing.opposite()
            world.setBlock(pos, palette.stair.withFacing(stairFacing))
            for (i in 1..palette.sidewalkThickness) {
                world.setBlock(pos.addY(i), palette.sidewalkBlock)
            }
            world.setBlock(pos.addY(palette.sidewalkThickness + 1), palette.stair.withFacing(facing))
        }
    }

    class RoadTemplate(val rows: List<String>) {
        fun apply(context: PlacementContext, basePos: Vec3, symbolMap: Map<Char, (PlacementContext, Vec3) -> Unit>) {
            val halfWidth = rows[0].length / 2
            val halfHeight = rows.size / 2

            for ((zOffset, row) in rows.withIndex()) {
                for ((xOffset, symbol) in row.withIndex()) {
                    val x = xOffset - halfWidth
                    val z = zOffset - halfHeight
                    val rotatedPos = context.rotation.rotateRelative(basePos.add(x, 0, z))
                    symbolMap[symbol]?.invoke(context, rotatedPos)
                }
            }
        }
    }
}

private fun BlockData.withFacing(stairFacing: Rotation): BlockData {
    if (this is Directional) {
        this.facing = stairFacing.toBlockFace()
    }
    return this
}

fun Block.withFacing(facing: Rotation): Block {
    if (this.blockData is Directional) {
        val directional: Directional = this.blockData as Directional
        directional.facing = facing.toBlockFace()
        this.blockData = directional
    }
    return this
}

fun Rotation.toBlockFace(base: BlockFace = BlockFace.NORTH): BlockFace {
    val horizontalFaces = listOf(
        BlockFace.NORTH,
        BlockFace.EAST,
        BlockFace.SOUTH,
        BlockFace.WEST
    )

    val index = horizontalFaces.indexOf(base)
    if (index == -1) {
        throw IllegalArgumentException("Rotation only supports horizontal faces (N, E, S, W)")
    }

    val rotatedIndex = when (this) {
        Rotation.NONE -> index
        Rotation.CLOCKWISE_90 -> (index + 1) % 4
        Rotation.CLOCKWISE_180 -> (index + 2) % 4
        Rotation.COUNTERCLOCKWISE_90 -> (index + 3) % 4
    }

    return horizontalFaces[rotatedIndex]
}

private fun Rotation.opposite(): Rotation {
    return when (this) {
        Rotation.NONE -> Rotation.NONE
        Rotation.CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90
        Rotation.CLOCKWISE_180 -> Rotation.NONE
        Rotation.COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90
    }
}

fun World.setBlock(pos: Vec3, block: Block) {
    val loc = Location(this, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
    loc.block.blockData = block.blockData
}

fun World.setBlock(pos: Vec3, blockData: BlockData) {
    val loc = Location(this, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
    loc.block.blockData = blockData
}


class RoadTemplate(private val rows: List<String>) {
    fun apply(context: ModularRoadPlacer5000.PlacementContext, basePos: Vec3, symbolMap: Map<Char, (ModularRoadPlacer5000.PlacementContext, Vec3) -> Unit>) {
        val halfWidth = rows[0].length / 2
        val halfHeight = rows.size / 2

        for ((zOffset, row) in rows.withIndex()) {
            for ((xOffset, symbol) in row.withIndex()) {
                val x = xOffset - halfWidth
                val z = zOffset - halfHeight
                val rotatedPos = context.rotation.rotateRelative(basePos.add(x, 0, z))
                symbolMap[symbol]?.invoke(context, rotatedPos)
            }
        }
    }
}

fun Rotation.rotateRelative(add: Vec3): Vec3 {
    return when (this) {
        Rotation.NONE -> add
        Rotation.CLOCKWISE_90 -> Vec3(add.z, add.y, -add.x)
        Rotation.CLOCKWISE_180 -> Vec3(-add.x, add.y, -add.z)
        Rotation.COUNTERCLOCKWISE_90 -> Vec3(-add.z, add.y, add.x)
    }
}

data class RoadPalette(val roadBlock: BlockData, val altRoadBlock: BlockData, val sidewalkBlock: BlockData, val stair: BlockData, val sidewalkThickness: Int)
val defaultRoadPalettes = listOf(

    // 1. Medieval Village
    RoadPalette(
        roadBlock = Material.GRAVEL.createBlockData(),
        altRoadBlock = Material.COARSE_DIRT.createBlockData(),
        sidewalkBlock = Material.COBBLESTONE.createBlockData(),
        stair = Material.COBBLESTONE_STAIRS.createBlockData(),
        sidewalkThickness = 1
    ),

    // 2. Modern City
    RoadPalette(
        roadBlock = Material.BLACK_CONCRETE.createBlockData(),
        altRoadBlock = Material.GRAY_CONCRETE.createBlockData(),
        sidewalkBlock = Material.STONE.createBlockData(),
        stair = Material.STONE_BRICK_STAIRS.createBlockData(),
        sidewalkThickness = 2
    ),

    // 3. Japanese Street (clean, peaceful)
    RoadPalette(
        roadBlock = Material.GRAY_CONCRETE_POWDER.createBlockData(),
        altRoadBlock = Material.LIGHT_GRAY_CONCRETE.createBlockData(),
        sidewalkBlock = Material.SMOOTH_STONE.createBlockData(),
        stair = Material.STONE_STAIRS.createBlockData(),
        sidewalkThickness = 1
    ),

    // 4. Cyberpunk/Futuristic
    RoadPalette(
        roadBlock = Material.BLACK_CONCRETE.createBlockData(),
        altRoadBlock = Material.LIGHT_BLUE_CONCRETE.createBlockData(),
        sidewalkBlock = Material.IRON_BLOCK.createBlockData(),
        stair = Material.POLISHED_ANDESITE_STAIRS.createBlockData(),
        sidewalkThickness = 3
    ),

    // 5. Tropical Resort
    RoadPalette(
        roadBlock = Material.SAND.createBlockData(),
        altRoadBlock = Material.RED_SAND.createBlockData(),
        sidewalkBlock = Material.SMOOTH_SANDSTONE.createBlockData(),
        stair = Material.SANDSTONE_STAIRS.createBlockData(),
        sidewalkThickness = 1
    ),

    // 6. Post-Apocalyptic/Deserted
    RoadPalette(
        roadBlock = Material.MYCELIUM.createBlockData(),
        altRoadBlock = Material.PODZOL.createBlockData(),
        sidewalkBlock = Material.CRACKED_STONE_BRICKS.createBlockData(),
        stair = Material.STONE_BRICK_STAIRS.createBlockData(),
        sidewalkThickness = 2
    ),

    // 7. Steampunk Industrial
    RoadPalette(
        roadBlock = Material.ANDESITE.createBlockData(),
        altRoadBlock = Material.POLISHED_ANDESITE.createBlockData(),
        sidewalkBlock = Material.BRICKS.createBlockData(),
        stair = Material.BRICK_STAIRS.createBlockData(),
        sidewalkThickness = 2
    ),

    // 8. Snowy/Tundra Town
    RoadPalette(
        roadBlock = Material.SNOW_BLOCK.createBlockData(),
        altRoadBlock = Material.WHITE_CONCRETE_POWDER.createBlockData(),
        sidewalkBlock = Material.QUARTZ_BLOCK.createBlockData(),
        stair = Material.QUARTZ_STAIRS.createBlockData(),
        sidewalkThickness = 1
    ),

    // 9. Forest/Gnome Village
    RoadPalette(
        roadBlock = Material.DIRT_PATH.createBlockData(),
        altRoadBlock = Material.MOSS_BLOCK.createBlockData(),
        sidewalkBlock = Material.MOSSY_COBBLESTONE.createBlockData(),
        stair = Material.MOSSY_COBBLESTONE_STAIRS.createBlockData(),
        sidewalkThickness = 1
    ),

    // 10. Worn-Down Rural Road
    RoadPalette(
        roadBlock = Material.DIRT.createBlockData(),
        altRoadBlock = Material.MUD.createBlockData(),
        sidewalkBlock = Material.OAK_PLANKS.createBlockData(),
        stair = Material.OAK_STAIRS.createBlockData(),
        sidewalkThickness = 1
    )
)

// === You will need to provide or define: ===
// - enum class RoadType
// - class Vec3(x: Int, y: Int, z: Int)
// - interface World { fun setBlock(pos: Vec3, block: Block) }
// - class Block
// - class RoadPalette(val roadBlock: Block, val altRoadBlock: Block, val sidewalkBlock: Block, val stair: Block, val sidewalkThickness: Int)
// - enum class Rotation { NORTH, EAST, SOUTH, WEST; fun opposite(): Rotation; fun rotateRelative(pos: Vec3): Vec3 }


interface StructurePlacer {
    fun generateBuildingPositions(
        config: CityConfig,
        roadMap: Map<Vec3, RoadPlacement>,
        spacingMap: Map<String, Int>
    ): Map<Vec3, Pair<BuildingType, String>>
}

class DistributedStructurePlacer : StructurePlacer {
    override fun generateBuildingPositions(
        config: CityConfig,
        roadMap: Map<Vec3, RoadPlacement>,
        spacingMap: Map<String, Int>
    ): Map<Vec3, Pair<BuildingType, String>> {
        val buildings = mutableMapOf<Vec3, Pair<BuildingType, String>>()
        val occupied = roadMap.keys

        pos@for (pos in roadMap.keys) {
            for (spacing in spacingMap.entries) {
                val neighbors = listOf(
                    pos.offset(spacing.value, 0, 0),
                    pos.offset(-spacing.value, 0, 0),
                    pos.offset(0, 0, spacing.value),
                    pos.offset(0, 0, -spacing.value)
                )

                for (n in neighbors) {
                    if (n !in occupied && !buildings.containsKey(n)) {
                        buildings[n] = BuildingType.HOUSE to spacing.key
                        continue@pos
                    }
                }
            }
        }

        return buildings
    }
}

fun Pair<Double, Double>.toIntRange(): IntRange {
    return this.first.toInt()..this.second.toInt()
}

fun Direction.toRotation(): Rotation = when (this) {
    Direction.NORTH -> Rotation.NONE
    Direction.EAST  -> Rotation.CLOCKWISE_90
    Direction.SOUTH -> Rotation.CLOCKWISE_180
    Direction.WEST  -> Rotation.COUNTERCLOCKWISE_90
    else -> Rotation.NONE
}

fun Positioned.toCoord(): Pair<Int, Int> = x to z
fun Int.inRangeOf(value: Int, tolerance: Int = 5): Boolean {
    return abs(this - value) <= tolerance
}

fun Int?.inRangeOf(value: Int?, tolerance: Int = 5): Boolean {
    if (this == null || value == null) return false
    return abs(this - value) <= tolerance
}

fun <T> List<T>.weightedRandomOrNull(weightSelector: (T) -> Int): T? {
    val totalWeight = sumOf { weightSelector(it).coerceAtLeast(0) }
    if (totalWeight <= 0) return null

    var random = Random.nextInt(totalWeight)
    for (item in this) {
        random -= weightSelector(item)
        if (random < 0) return item
    }
    return null
}