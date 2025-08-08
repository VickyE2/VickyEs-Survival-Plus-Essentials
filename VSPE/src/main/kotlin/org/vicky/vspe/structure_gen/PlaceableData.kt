package org.vicky.vspe.structure_gen

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.vicky.platform.utils.Rotation
import org.vicky.platform.world.PlatformBlock
import java.io.File

@ConfigSerializable
data class RoadSet(
    val thickness: Int = 4,
    val length: Int = 4,
    val files: Map<RoadType, RoadFile>,
    @Transient var namespace: String = ""
)
@ConfigSerializable
data class RoadFile(
    val name: String,
    @Transient var namespace: String = ""
){
    fun placeRoadPiece(
        location: PlatformBlock<*>,
        rotation: Rotation = Rotation.NONE
    ) {
        /*
        NMS.handler
            .placeStructure(
                location = location.location,
                structureName = name,
                namespace = namespace,
                rotation = rotation,
                replacementPalette = emptyMap()
            )
         */
    }
}

@ConfigSerializable
data class BuildingPack(
    val name: String,
    val components: List<BuildingComponent>,
    val rules: BuildingRules,
    val type: BuildingType,
    @Transient var namespace: String = ""
)

@ConfigSerializable
data class BuildingComponent(
    val tag: String,
    val pieces: List<String>,
    val required: Boolean = false,
    val optional: Boolean = false,
    val repeat: RepeatRule? = null
) {
    /**
     * Resolve a single concrete StructurePiece name to use.
     * @param structurePieces all available StructurePieces (by name)
     * @return selected piece name, or null if nothing found
     */
    fun resolve(structurePieces: List<StructurePiece>): StructurePiece {
        // Filter only pieces in this component's list
        val matchingPieces = structurePieces.filter { it.name in pieces }
        if (matchingPieces.isEmpty()) throw IllegalArgumentException("No pieces in the component are in the list")

        // Pick by weight
        return matchingPieces.weightedRandomOrNull { it.weight } ?: throw IllegalArgumentException("Unknown selected piece")
    }
}

data class RepeatRule(val min: Int, val max: Int)
@ConfigSerializable
data class BuildingRules(
    val weight: Int = 1,
    val central: Boolean = false,
    val unique: Boolean = false,
    val allowNearRoad: Boolean = true,
    val spacing: Int = 3, // Avg
    val chunkSpacing: Int = 2 // Minimum
)

@ConfigSerializable
data class StructurePiece(
    val name: String,
    val file: String,
    val loot_table: String,
    val size: List<Int> = listOf(0, 0, 0),
    val offset: List<Int> = listOf(0, 0, 0),
    val weight: Int = 1,
    @Transient var namespace: String = ""
) {
    fun placeStructurePiece(
        location: PlatformBlock<*>,
        rotation: Rotation = Rotation.NONE,
        replacementPalette: StructurePalette = emptyMap()
    ) {
        /*
        NMS.handler
            .placeStructure(
                location = location.location,
                structureName = name,
                namespace = namespace,
                loot_table = loot_table,
                rotation = rotation,
                replacementPalette = replacementPalette
            )
         */
    }
}

@ConfigSerializable
data class Decorator(
    val name: String,
    val type: DecoratorType,
    val position: Position = Position.CENTER,
    val offset: List<Int> = listOf(0, 0, 0),
    val chance: Double = 1.0,
    val pieces: List<String>,
    @Transient var namespace: String = ""
) {
    /**
     * Resolve a single concrete StructurePiece name to use.
     * @param structurePieces all available StructurePieces (by name)
     * @return selected piece name, or null if nothing found
     */
    fun resolve(structurePieces: List<StructurePiece>): StructurePiece {
        // Filter only pieces in this component's list
        val matchingPieces = structurePieces.filter { it.name in pieces }
        if (matchingPieces.isEmpty()) throw IllegalArgumentException("No pieces in the component are in the list")

        // Pick by weight
        return matchingPieces.weightedRandomOrNull { it.weight } ?: throw IllegalArgumentException("Unknown selected piece")
    }
}


@ConfigSerializable
data class LootTable(
    val name: String,
    val pools: List<LootPool>,
    @Transient var namespace: String = ""
)

@ConfigSerializable
data class LootPool(
    val conditions: List<LootCondition>? = null,
    val rolls: Int,
    val entries: List<LootEntry>
)

@ConfigSerializable
data class LootCondition(
    val condition: String,
    val chance: Double? = null,
    val value: Double? = null,
    val min: Double? = null,
    val max: Double? = null
)

@ConfigSerializable
data class LootEntry(
    val type: String,
    val name: String,
    val weight: Int = 1,
    val functions: List<LootFunction>? = null
)

@ConfigSerializable
data class LootFunction(
    val function: String,
    val data: Int? = null,
    val count: Int? = null,
    val min: Int? = null,
    val max: Int? = null,
    val id: String? = null,
    val tag: String? = null
)

@ConfigSerializable
data class StructurePack(
    val id: String,
    val name: String,
    val description: String,
    val author: String,
    val version: String,
    val namespace: String,
    val road: RoadSet,
    val town_hall: String?,
    val buildings: List<BuildingPack> = listOf(),
    val pieces: List<StructurePiece> = listOf(),
    val decorators: List<Decorator> = listOf(),
    val loot_tables: List<LootTable> = listOf()
)