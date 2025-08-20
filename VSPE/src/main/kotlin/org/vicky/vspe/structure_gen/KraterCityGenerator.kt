package org.vicky.vspe.structure_gen

import org.vicky.platform.utils.Rotation
import org.vicky.platform.utils.Vec3
import org.vicky.platform.world.PlatformBlockState
import org.vicky.platform.world.PlatformLocation
import org.vicky.platform.world.PlatformWorld
import org.vicky.utilities.ContextLogger.ContextLogger
import org.vicky.vspe.PaletteFunction
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.SimpleBlockState
import org.vicky.vspe.weightedRandomOrNull
import kotlin.math.*
import kotlin.properties.Delegates
import kotlin.random.Random


sealed class Shape {
    data object CIRCLE : Shape()
    data object RECT : Shape()
    data object STAR : Shape()
    data object TRIANGLE : Shape()
    data class POLYGON(val sides: Int) : Shape()

    fun isInside(x: Int, z: Int, centerX: Int, centerZ: Int, size: Int): Boolean {
        val dx = x - centerX
        val dz = z - centerZ
        return when (this) {
            CIRCLE -> dx * dx + dz * dz <= size * size

            RECT -> dx in -size..size && dz in -size..size

            STAR -> {
                val r2 = dx * dx + dz * dz <= size * size
                val spiky = ((dx + dz) % (size / 2 + 1)).absoluteValue < (size / 6)
                r2 && spiky
            }

            TRIANGLE -> {
                val normX = dx.toDouble() / size
                val normZ = dz.toDouble() / size
                normZ in 0.0..1.0 && abs(normX) <= (1 - normZ)
            }

            is POLYGON -> {
                val angle = this.sides // Hexagon. Change this for other polygons.
                val r = sqrt((dx * dx + dz * dz).toDouble())
                val theta = atan2(dz.toDouble(), dx.toDouble())
                val a = Math.PI / angle
                r * cos((PI / angle)) <= size * cos(theta % (2 * a) - a)
            }
        }
    }
}

data class StructureSlot(
    val type: BuildingType,
    val position: Vec3, // x, z position (center or origin-based)
    val containedSize: Int
)

enum class CityType {
    VILLAGE, METROPOLITAN, FORTIFIED, CUSTOM
}

enum class BuildingType {
    HOUSE, TOWNHALL, ROAD, EMPTY, SPECIAL
}

data class StructureConfig(
    val type: BuildingType,
    val minHeight: Int,
    val maxHeight: Int,
    val variationChance: Float = 0.2f // 20% chance height may vary
)

data class CityConfig(
    val cityType: CityType,
    val cityShape: Shape,
    val hasTownHall: Boolean = true,
    val arrangement: ArrangementType,
    val density: Float,
    val heightRatio: Float,
    val townHallSpacing: Int?,
    val buildingMaxSpacing: Int,
    val buildingMinSpacing: Int,
    val buildingPadding: Int,
    val roadPadding: Int
)

enum class ArrangementType {
    GRID, CIRCULAR, ORGANIC, RANDOMIZED, SPIRAL, RANDOM, CENTRALIZED, DISTRIBUTED, BRANCHED
}

enum class RoadType {
    STRAIGHT_NS,
    STRAIGHT_EW,
    STRAIGHT_NE_SW,
    STRAIGHT_NW_SE,
    CORNER_NE,
    CORNER_NW,
    CORNER_SE,
    CORNER_SW,
    CROSS,
    T_NORTH,
    T_SOUTH,
    T_EAST,
    T_WEST,
    T_NORTHEAST,
    T_SOUTHWEST,
    DEAD_END_N,
    DEAD_END_NE,
    DEAD_END_SW,
    DEAD_END_W,
    DEAD_END_NW,
    DEAD_END_SE,
    DEAD_END_S,
    DEAD_END_E,
    T_NE,
    T_NW,
    T_SW,
    T_SE,
    CROSS_DIAG
}

data class Vec2(val x: Int, val z: Int)
typealias PositionalData = Triple<BuildingType, Rotation, Map<String, Any>>
typealias CityLayoutMap = Map<Vec3, PositionalData>
typealias StructurePalette = Map<PlatformBlockState<*>, PaletteFunction>


abstract class CityLayoutBuilder(
    val config: CityConfig,
    private val roadLayoutEngine: RoadLayoutEngine
) {
    private val structureMap = mutableListOf<StructureSlot>()
    private lateinit var cityMap: CityLayoutMap
    val decoratorPlacements: MutableMap<Vec2, DecoratorPlacement> = mutableMapOf()

    private val contextLogger = ContextLogger(ContextLogger.ContextType.SYSTEM, "CITY-BUILDER")

    fun buildCity() {
        init()
        contextLogger.print("🚧 Building city of type ${config.cityType} with layout ${config.arrangement}", ContextLogger.LogType.PENDING)
        cityMap = layoutCity()
        preProcess()
        cityMap.forEach { (pos, tileData) ->
            when (tileData.first) {
                BuildingType.ROAD -> placeRoad(
                    pos,
                    tileData
                )
                BuildingType.TOWNHALL -> placeTownHall(pos)
                BuildingType.HOUSE -> placeBuilding(pos, tileData)
                else -> {} // No-op for now
            }
        }
        postProcess()
    }

    protected abstract fun init()
    protected abstract fun layoutCity(): CityLayoutMap
    protected abstract fun placeTownHall(pos: Vec3)
    protected abstract fun placeRoad(pos: Vec3, tileData: PositionalData)
    protected abstract fun placeBuilding(pos: Vec3, tileData: PositionalData)


    protected open fun preProcess() {

    }
    protected open fun postProcess() {
        contextLogger.print("✅ Post processing city layout...", ContextLogger.LogType.PENDING)
        // Additional logic like parks, decorations etc.
    }

    protected open fun getCenterPosition(): Vec3 = Vec3(0.0, 0.0, 0.0)

    protected open fun generateCityLayoutMap(
        cityRadius: Int,
        roadThickness: Int,
        roadLength: Int,
        buildingSizes: Map<String, Int>,
        specialBuildings: Map<String, Int>,
        decorators: Map<String, Pair<DecoratorType, Position>>
    ): CityLayoutMap {
        val centerX = getCenterPosition().x
        val centerZ = getCenterPosition().z
        val shape = config.cityShape

        val rawRoadMap = roadLayoutEngine.generateRoadAndBuildingMap(config, buildingSizes, specialBuildings, decorators, getMapHeightRange(), config.arrangement, centerX.toInt(), centerZ.toInt(), cityRadius, roadThickness, roadLength, shape)
        val layoutMap = mutableMapOf<Vec3, Triple<BuildingType, Rotation, Map<String, Any>>>()

        val roadPlacements = rawRoadMap.first
        val buildingPlacements = rawRoadMap.second
        val decoratorPlacement = rawRoadMap.third

        val roadCords = roadPlacements.map { Vec2(it.x, it.z) }.toSet()
        val buildingPlacementsByCoord = buildingPlacements.keys.associateBy {
            Vec2(it.x, it.z)
        }
        decoratorPlacements.putAll(decoratorPlacement.toTypeMap { Vec2(it.x, it.z) })

        for ((x, z) in getMapHeightRange().keys) {
            val pos = Vec3(x.toDouble(), getCenterPosition().y, z.toDouble())

            if (!shape.isInside(x, z, centerX.toInt(), centerZ.toInt(), cityRadius)) {
                layoutMap[pos] = Triple(BuildingType.EMPTY, Rotation.NONE, mapOf())
                continue
            }

            val posKey = Vec2(x, z)
            val isRoad = posKey in roadCords
            val bp = buildingPlacementsByCoord[posKey]

            val (buildingType, rotation, metadata) = when {
                isRoad -> {
                    val dirs = roadLayoutEngine.getConnections(rawRoadMap.first, x, z)
                    val roadType = roadLayoutEngine.classifyRoadType(dirs)
                    val roadRotation = roadLayoutEngine.getRotationFor(roadType)

                    val meta = mutableMapOf<String, Any>("roadType" to roadType)

                    Triple(BuildingType.ROAD, roadRotation, meta)
                }

                bp != null -> {
                    val meta = bp.meta.toMutableMap()
                    Triple(bp.type, buildingPlacements[bp] ?: Rotation.NONE, meta)
                }

                else -> {
                    Triple(BuildingType.EMPTY, Rotation.NONE, emptyMap<String, Any>())
                }
            }

            layoutMap[pos] = Triple(buildingType, rotation, metadata)
        }

        return layoutMap
    }

    fun getLogger(): ContextLogger = contextLogger
    fun getStructureMap(): List<StructureSlot> = structureMap
    abstract fun CityType.getCitySpacingBySize(): Pair<Int, Int>
    abstract fun getMapHeightRange(): Map<Pair<Int, Int>, Int>
}

private fun <E, F> List<E>.toTypeMap(function: (E) -> F): MutableMap<F, E> {
    val map = mutableMapOf<F, E>()
    for (entity in this) {
        map[function(entity)] = entity
    }
    return map
}

private fun Vec3.from(center: Vec3): Vec3 {
    return Vec3(x - center.x, y - center.y, z - center.z)
}

fun Vec3.offset(dx: Int, dy: Int, dz: Int): Vec3 {
    return Vec3(x + dx, y + dy, z + dz)
}


class KraterBukkitCityGenerator @JvmOverloads constructor(
    config: CityConfig,
    val world: PlatformWorld<*, *>,
    private val structurePack: StructurePack,
    private val location: PlatformLocation,
    roadLayoutEngine: RoadLayoutEngine,
    private val useAlphaRoad: Boolean = false,
    private val useBetaReplacementLogic: Boolean = false,
    private val modularPlacerPalette: RoadPalette? = null,
    private val modularPlacerTemplates: Map<RoadType, ModularRoadPlacer5000.RoadTemplate> = emptyMap(),
    private val palette: StructurePalette = ReplacementPalettes.medievalReplacementPalette
): CityLayoutBuilder(config, roadLayoutEngine) {
    private var mapHeightRange: Map<Pair<Int, Int>, Int> = mapOf()
    private var cityRange by Delegates.notNull<Int>()
    private var placer = ModularRoadPlacer5000()

    override fun init() {
        placer.roadTemplates.putAll(modularPlacerTemplates)
    }

    override fun layoutCity(): CityLayoutMap  {
        cityRange = Random.nextInt(config.cityType.getCityTypeBySize().first,
            config.cityType.getCityTypeBySize().second)
        return generateCityLayoutMap(
            cityRange,
            structurePack.road.thickness,
            structurePack.road.length,
            structurePack.buildings.toParameterizedMap(
                transform = { it.name to it.rules.spacing },
                condition = { !it.rules.unique },
                performOnMap = { map ->
                    structurePack.town_hall?.let { name ->
                        structurePack.buildings.find { it.name == name }?.let {
                            map["TOWNHALL"] = it.rules.spacing
                        } ?: throw IllegalArgumentException("Town hall building specified does not exist")
                    }
                }
            ),
            structurePack.buildings.toParameterizedMap(
                transform = { it.name to it.rules.spacing },
                condition = { it.rules.unique }
            ),
            structurePack.decorators.toParameterizedMap(
                transform = { it.name to (it.type to it.position) }
            )
        )
    }

    override fun getCenterPosition(): Vec3 {
        return Vec3(location.x, location.y, location.z)
    }

    override fun getMapHeightRange(): Map<Pair<Int, Int>, Int> {
        return mapHeightRange
    }

    override fun preProcess() {
        mapHeightRange = world.extractHeightMapFast((location.z.toInt() - cityRange)..(location.z.toInt() + cityRange), (location.z.toInt() - cityRange)..(location.z.toInt() + cityRange))
        world.clearAboveHeights(mapHeightRange, config.cityShape, location, cityRange)
        world.flattenCityArea(getCenterPosition(), cityRange, mapHeightRange.values.min(), config.cityShape)
    }

    override fun postProcess() {
        decoratorPlacements.forEach { data ->
            val dp = data.value
            val pos = data.key

            val piece = structurePack.decorators.find { it.name == dp.type }
                ?: throw IllegalArgumentException("Decorator piece not found: ${dp.type}")

            piece.resolve(structurePack.pieces).placeStructurePiece(
                location = world.getBlockAt(pos.x.toDouble(), mapHeightRange.values.min() + piece.offset[1].toDouble(), pos.z.toDouble()),
                rotation = dp.rotation
            )
        }
    }
    override fun placeTownHall(pos: Vec3) {
        if (config.hasTownHall) {
            val townhallPack = structurePack.town_hall?.let { name ->
                structurePack.buildings.find { it.name == name }
            } ?: throw IllegalArgumentException("Townhall building specified does not exist")

            val townhallPieceName = townhallPack.components.find { it.tag == "full_building" }?.pieces?.first()
                ?: throw IllegalArgumentException("Townhall building specified does not exist")

            val townhallPiece = structurePack.pieces.find { it.name == townhallPieceName }
                ?: throw IllegalArgumentException("Townhall building specified does not exist")

            townhallPiece.placeStructurePiece(
                location = world.getBlockAt(pos.x, mapHeightRange.values.min().toDouble(), pos.z)
            )
        }
    }

    override fun placeRoad(pos: Vec3, tileData: PositionalData) {
        if (!useAlphaRoad) {
            val roadPack = structurePack.road
            val roadFile = roadPack.files[tileData.third["roadType"] as RoadType]
                ?: throw IllegalArgumentException("Road file specified does not exist : ${tileData.third["roadType"]}")

            roadFile.placeRoadPiece(
                location = world.getBlockAt(pos.x, mapHeightRange.values.min().toDouble(), pos.z),
                tileData.second
            )
        }
        else {
            val meta = tileData.third
            val roadType = meta["roadType"] as? RoadType ?: return
            val rotation = tileData.second
            placer.placeRoad(world, pos, roadType, modularPlacerPalette ?: placer.DefaultRoadPalette, rotation)
        }
    }

    override fun placeBuilding(pos: Vec3, tileData: PositionalData) {
        val buildingPacks = structurePack.buildings
        val buildingPack = buildingPacks.find { it.rules.spacing.inRangeOf(tileData.third["size"] as Int, tolerance = 1) } ?: throw IllegalArgumentException("Road file specified does not exist : ${tileData.third["roadType"]}")

        val fullBuildingComponent = buildingPack.components.find { it.tag == "full_building" }
        val entryComponent = buildingPack.components.find { it.tag == "entry" }
        val groundFloorComponent = buildingPack.components.find { it.tag == "ground_floor" }
        val floorComponent = buildingPack.components.find { it.tag == "floor" }
        val basementComponent = buildingPack.components.find { it.tag == "roof" }
        val roofComponent = buildingPack.components.find { it.tag == "basement" }

        if (fullBuildingComponent != null) {
            val piece = fullBuildingComponent.resolve(structurePack.pieces)
            val accPos = pos.offset(0, -piece.size[1], 0)
            piece.placeStructurePiece(
                world.getBlockAt(accPos.x, accPos.y, accPos.z),
                tileData.second
            )
        }
        else {
            var currentY = mapHeightRange.values.min()
            if (basementComponent != null) {
                val piece = basementComponent.resolve(structurePack.pieces)
                val accPos = pos.offset(0, currentY - piece.size[1], 0)
                piece.placeStructurePiece(
                    world.getBlockAt(accPos.x, accPos.y, accPos.z),
                    tileData.second,
                    if (useBetaReplacementLogic) palette else emptyMap()
                )
            }
            if (entryComponent == null && groundFloorComponent == null)
                throw IllegalArgumentException("Pack must have an entry or ground floor")
            if (entryComponent != null) {
                val piece = entryComponent.resolve(structurePack.pieces)
                val yOffset = currentY + piece.offset[1] + piece.size[1]
                val accPos = pos.offset(0, yOffset, 0)
                currentY += yOffset
                piece.placeStructurePiece(
                    world.getBlockAt(accPos.x, accPos.y, accPos.z),
                    tileData.second,
                    if (useBetaReplacementLogic) palette else emptyMap()
                )
            }
            if (groundFloorComponent != null) {
                val piece = groundFloorComponent.resolve(structurePack.pieces)
                val yOffset = currentY + piece.offset[1] + piece.size[1]
                val accPos = pos.offset(0, yOffset, 0)
                currentY += yOffset
                piece.placeStructurePiece(
                    world.getBlockAt(accPos.x, accPos.y, accPos.z),
                    tileData.second,
                    if (useBetaReplacementLogic) palette else emptyMap()
                )
            }
            if (floorComponent != null) {
                if (floorComponent.repeat != null) {
                    for (i in floorComponent.repeat.min..floorComponent.repeat.max) {
                        val piece = floorComponent.resolve(structurePack.pieces)
                        val yOffset = currentY + piece.offset[1] + piece.size[1]
                        val accPos = pos.offset(0, yOffset, 0)
                        currentY += yOffset
                        piece.placeStructurePiece(
                            world.getBlockAt(accPos.x, accPos.y, accPos.z),
                            tileData.second,
                            if (useBetaReplacementLogic) palette else emptyMap()
                        )
                    }
                }
                else {
                    val piece = floorComponent.resolve(structurePack.pieces)
                    val yOffset = currentY + piece.offset[1] + piece.size[1]
                    val accPos = pos.offset(0, yOffset, 0)
                    currentY += yOffset
                    piece.placeStructurePiece(
                        world.getBlockAt(accPos.x, accPos.y, accPos.z),
                        tileData.second,
                        if (useBetaReplacementLogic) palette else emptyMap()
                    )
                }
            }
            if (roofComponent != null) {
                val piece = roofComponent.resolve(structurePack.pieces)
                val yOffset = currentY + piece.offset[1] + piece.size[1]
                val accPos = pos.offset(0, yOffset, 0)
                currentY += yOffset
                piece.placeStructurePiece(
                    world.getBlockAt(accPos.x, accPos.y, accPos.z),
                    tileData.second,
                    if (useBetaReplacementLogic) palette else emptyMap()
                )
            }
        }
    }

    private fun CityType.getCityTypeBySize(): Pair<Int, Int> {
        return when (this) {
            CityType.VILLAGE -> 50 to 100
            CityType.METROPOLITAN -> 200 to 400
            CityType.FORTIFIED -> 80 to 210
            CityType.CUSTOM -> TODO()
        }
    }

    override fun CityType.getCitySpacingBySize(): Pair<Int, Int> {
        return when (this) {
            CityType.VILLAGE -> 4 to 7
            CityType.METROPOLITAN -> 1 to 5
            CityType.FORTIFIED -> 3 to 7
            CityType.CUSTOM -> TODO()
        }
    }

    fun <T> PlatformWorld<T, *>.flattenCityArea(
        center: Vec3,
        radius: Int,
        baseY: Int,
        shape: Shape,
        fillDepth: Int = 5, // how deep to fill below with stone/dirt
        surface: PlatformBlockState<T> = this.getBlockAt(
            center.x,
            this.getHighestBlockYAt(center.x, center.z).toDouble(),
            center.z
        ).blockState,
        underFill: PlatformBlockState<T> = this.getBlockAt(
            center.x,
            this.getHighestBlockYAt(center.x, center.z).toDouble() - 10,
            center.z
        ).blockState
    ) {
        val centerX = center.x
        val centerZ = center.z

        for (x in (centerX - radius).toInt()..(centerX + radius).toInt()) {
            for (z in (centerZ - radius).toInt()..(centerZ + radius).toInt()) {
                if (!shape.isInside(x, z, centerX.toInt(), centerZ.toInt(), radius)) continue

                val highest = this.getHighestBlockYAt(x.toDouble(), z.toDouble())

                // Clear blocks above target level
                for (y in baseY..highest) {
                    val block = this.getBlockAt(x.toDouble(), y.toDouble(), z.toDouble())
                    if (block.blockState != this.airBlockState) {
                        block.blockState = this.airBlockState
                    }
                }

                // Fill under target level to solid ground
                for (y in (baseY - fillDepth) until baseY - 1) {
                    this.getBlockAt(x.toDouble(), y.toDouble(), z.toDouble()).blockState = underFill
                }

                // Place surface block
                this.getBlockAt(x.toDouble(), (baseY - 1).toDouble(), z.toDouble()).blockState = surface
            }
        }
    }

    private fun PlatformWorld<*, *>.extractHeightMapFast(xRange: IntRange, zRange: IntRange): Map<Pair<Int, Int>, Int> {
        val heightMap = mutableMapOf<Pair<Int, Int>, Int>()
        for (x in xRange) {
            for (z in zRange) {
                this.loadChunkIfNeeded(x shr 4, z shr 4)
                val y = this.getTrueGroundY(x, z)
                if (y >= 0) {
                    heightMap[x to z] = y
                }
            }
        }
        return heightMap
    }
    private fun <T> PlatformWorld<T, *>.clearAboveHeights(
        heightMap: Map<Pair<Int, Int>, Int>,
        shape: Shape,
        center: PlatformLocation,
        size: Int,
        maxY: Int = this.maxWorldHeight
    ) {
        val centerX = center.x
        val centerZ = center.z

        for ((pos, groundY) in heightMap) {
            val (x, z) = pos
            if (!shape.isInside(x, z, centerX.toInt(), centerZ.toInt(), size)) continue

            for (y in (groundY + 1)..maxY) {
                val block = this.getBlockAt(x.toDouble(), y.toDouble(), z.toDouble())
                if (block.blockState != this.airBlockState) {
                    block.blockState = this.airBlockState
                }
            }
        }
    }
    private fun PlatformWorld<*, *>.getTrueGroundY(x: Int, z: Int): Int {
        setOf(
            SimpleBlockState.from("minecraft:grass_block") { it },
            SimpleBlockState.from("minecraft:dirt") { it },
            SimpleBlockState.from("minecraft:coarse_dirt") { it },
            SimpleBlockState.from("minecraft:stone") { it },
            SimpleBlockState.from("minecraft:andesite") { it },
            SimpleBlockState.from("minecraft:gravel") { it },
            SimpleBlockState.from("minecraft:sand") { it },
            SimpleBlockState.from("minecraft:sandstone") { it },
            SimpleBlockState.from("minecraft:podzol") { it },
            SimpleBlockState.from("minecraft:mycelium") { it },
            SimpleBlockState.from("minecraft:snow_block") { it },
            SimpleBlockState.from("minecraft:clay") { it },
            SimpleBlockState.from("minecraft:terracotta") { it }
        )
        if (!this.isChunkLoaded(x shr 4, z shr 4)) return -1

        for (y in this.maxWorldHeight - 1 downTo 0) {
            val type = this.getBlockAt(x.toDouble(), y.toDouble(), z.toDouble()).blockState
            if (type != this.airBlockState) {
                return y
            }
        }
        return -1
    }
    fun StructurePack.randomBuildingFor(type: BuildingType, y: Float, tolerance: Float = 5f): BuildingPack? {
        fun BuildingPack.estimatedHeight(): Float {
            val floor = components.firstOrNull { it.tag.equals("floor", ignoreCase = true) } ?: return 1f
            val max = floor.repeat?.max?.toFloat() ?: 1f
            val min = floor.repeat?.min?.toFloat() ?: 1f
            return (max / min).coerceIn(0f..1f)
        }
        val matches = buildings.filter { it.type == type && abs(y - it.estimatedHeight()) <= tolerance }

        // If we got matches, do a weighted random pick
        if (matches.isNotEmpty()) {
            return matches.weightedRandomOrNull { it.rules.weight }
        }

        // Fallback: pick any of that type randomly
        return buildings.filter { it.type == type }.randomOrNull()
    }
}

private fun <K, T, U> List<K>.toParameterizedMap(transform: (K) -> Pair<T, U>, condition: (K) -> (Boolean) = { true }, performOnMap: (MutableMap<T,U>) -> Unit = {}): MutableMap<T, U> {
    val map = mutableMapOf<T, U>()
    for (entity in this) {
        if (!condition(entity)) continue
        val (key, value) = transform(entity)
        map[key] = value
    }
    performOnMap(map)
    return map
}
