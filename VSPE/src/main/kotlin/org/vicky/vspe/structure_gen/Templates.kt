package org.vicky.vspe.structure_gen

import kotlinx.serialization.json.*

fun generatePoolTemplate(
    namespace: String,
    structureName: String,
    weight: Int,
    variants: Map<String, Int>,
    structureType: StructureType
): String {
    val projType = if (structureType == StructureType.ROADS) "terrain_matching" else "rigid"
    return buildString {
        appendLine("""
            {
              "name": "${namespace}:${structureType.name.lowercase()}_${structureName}_pool",
              "fallback": "minecraft:empty",
              "elements": [
            	{	
                  "weight": ${weight},
                  "element": {
                    "location": "${namespace}:${structureType.name.lowercase()}/${structureName}",
                    "processors": "minecraft:empty",
            		"projection": $projType,
                    "element_type": "minecraft:single_pool_element"
                  }
                }
        """.trimIndent())
        variants.forEach { variant, weight ->
            appendLine(
                """
            	{	
                  "weight": ${weight},
                  "element": {
                    "location": "${namespace}:${structureType.name.lowercase()}/${variant}",
                    "processors": "minecraft:empty",
            		"projection": $projType,
                    "element_type": "minecraft:single_pool_element"
                  }
                }
        """.trimIndent()
            )
        }
        appendLine("""
              ]
            }
        """.trimIndent())
    }
}

fun generateStructureTemplate(
    namespace: String,
    poolName: String,
    distanceFromGround: Int
): String {
    return buildString {
        appendLine("""
            {
              "type": "minecraft:jigsaw",
              "biomes": "#minecraft:has_structure/pillager_outpost",
              "step": "surface_structures",
              "spawn_overrides": {},
              "start_pool": "${namespace}:${poolName}",
              "size": 3,
              "terrain_adaptation": "beard_thin",
              "start_height": {
                "absolute": ${distanceFromGround}
              },
              "project_start_to_heightmap": "WORLD_SURFACE_WG",
              "max_distance_from_center": 116,
              "use_expansion_hack": true
            }
        """.trimIndent())
    }
}

fun generateStructureSetTemplate(
    namespace: String,
    structureName: String,
    distanceFromGround: Int
): String {
    return buildString {
        appendLine("""
            {
              "structures": [
                {
                  "structure": "${namespace}:$structureName",
                  "weight": 1
                }
              ],
              "placement": {
                "salt": 121541467,
                "spacing": 12,
                "separation": 6,
                "type": "minecraft:random_spread"
              }
            }
        """.trimIndent())
    }
}


enum class StructureType {
    STRUCTURE,
    DECORATION,
    ROADS
}


fun generateStructureSet(building: BuildingPack, namespace: String): String {
    return """
        {
          "structures": [
            {
              "structure": "${namespace}:${building.name}",
              "weight": 1
            }
          ],
          "placement": {
            "salt": ${building.name.hashCode()},
            "spacing": ${building.rules.spacing},
            "separation": ${building.rules.chunkSpacing},
            "type": "minecraft:random_spread"
          }
        }
    """.trimIndent()
}

fun generateStructureJson(building: BuildingPack, namespace: String): String {
    return """
        {
          "type": "minecraft:jigsaw",
          "biomes": "#minecraft:has_structure/village",
          "step": "surface_structures",
          "spawn_overrides": {},
          "start_pool": "${namespace}:${building.name}_start",
          "size": 3,
          "terrain_adaptation": "beard_thin",
          "start_height": {
            "absolute": 64
          },
          "project_start_to_heightmap": "WORLD_SURFACE_WG",
          "max_distance_from_center": 80,
          "use_expansion_hack": true
        }
    """.trimIndent()
}

fun generatePool(building: BuildingPack, namespace: String): String {
    val startElements = buildList {
        for (component in building.components) {
            if (component.required) {
                component.pieces.forEach {
                    add("""
                      {
                        "weight": 1,
                        "element": {
                          "location": "${namespace}:${it}",
                          "processors": "minecraft:empty",
                          "projection": "rigid",
                          "element_type": "minecraft:single_pool_element"
                        }
                      }
                    """.trimIndent())
                }
            }
        }
    }

    return """
        {
          "name": "${namespace}:${building.name}_start",
          "fallback": "minecraft:empty",
          "elements": [
            ${startElements.joinToString(",\n")}
          ]
        }
    """.trimIndent()
}

fun generatePiecePool(piece: StructurePiece, namespace: String): String {
    return """
        {
          "name": "${namespace}:${piece.name}_pool",
          "fallback": "minecraft:empty",
          "elements": [
            {
              "weight": ${piece.weight},
              "element": {
                "location": "${namespace}:${piece.file.removeSuffix(".nbt")}",
                "processors": "minecraft:empty",
                "projection": "rigid",
                "element_type": "minecraft:single_pool_element"
              }
            }
          ]
        }
    """.trimIndent()
}

fun LootTable.toMinecraftJson(): String {
    val json = buildJsonObject {
        put("pools", JsonArray(pools.map { it.toJson() }))
    }
    return Json.encodeToString(JsonObject.serializer(), json)
}

fun LootPool.toJson(): JsonObject = buildJsonObject {
    put("rolls", rolls)
    if (!conditions.isNullOrEmpty()) {
        put("conditions", JsonArray(conditions.map { it.toJson() }))
    }
    put("entries", JsonArray(entries.map { it.toJson() }))
}

fun LootCondition.toJson(): JsonObject = buildJsonObject {
    put("condition", condition)
    chance?.let { put("chance", it) }
    value?.let { put("value", it) }
    min?.let { put("min", it) }
    max?.let { put("max", it) }
}

fun LootEntry.toJson(): JsonObject = buildJsonObject {
    put("type", type)
    put("name", name)
    put("weight", weight)
    functions?.let {
        put("functions", JsonArray(it.map { func -> func.toJson() }))
    }
}

fun LootFunction.toJson(): JsonObject = buildJsonObject {
    put("function", function)
    data?.let { put("data", it) }
    count?.let { put("count", it) }
    min?.let { put("min", it) }
    max?.let { put("max", it) }
    id?.let { put("id", it) }
    tag?.let { put("tag", it) }
}