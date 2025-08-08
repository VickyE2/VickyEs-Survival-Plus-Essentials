package org.vicky.vspe.structure_gen

import org.vicky.platform.world.PlatformBlockState
import org.vicky.vspe.PaletteFunction
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.SimpleBlockState

object ReplacementPalettes {
    val medievalReplacementPalette: Map<PlatformBlockState<*>, PaletteFunction> = mapOf(
        SimpleBlockState.from("minecraft:white_wool") { it } to { _, height, _, random ->
            if (height > 5) SimpleBlockState.from("minecraft:cobblestone") { it }
            else if (random.nextFloat() < 0.3f) SimpleBlockState.from("minecraft:mossy_cobblestone") { it }
            else SimpleBlockState.from("minecraft:andesite") { it }
        },
        SimpleBlockState.from("minecraft:gray_wool") { it } to { _, _, _, random ->
            if (random.nextBoolean()) SimpleBlockState.from("minecraft:deepslate_bricks") { it } else SimpleBlockState.from("minecraft:polished_andesite") { it }
        },
        SimpleBlockState.from("minecraft:red_wool") { it } to { _, height, _, _ ->
            if (height > 3) SimpleBlockState.from("minecraft:spruce_planks") { it } else SimpleBlockState.from("minecraft:stripped_spruce_log") { it }
        },
        SimpleBlockState.from("minecraft:stripped_oak_wood") { it } to { _, height, _, _ ->
            if (height > 6) SimpleBlockState.from("minecraft:stripped_dark_oak_log") { it } else SimpleBlockState.from("minecraft:dark_oak_log") { it }
        },
        SimpleBlockState.from("minecraft:oak_stairs") { it } to { _, height, _, random ->
            if (height % 2 == 0) SimpleBlockState.from("minecraft:spruce_stairs") { it }
            else if (random.nextFloat() < 0.2f) SimpleBlockState.from("minecraft:dark_oak_stairs") { it }
            else SimpleBlockState.from("minecraft:mangrove_stairs") { it }
        },
        SimpleBlockState.from("minecraft:red_sandstone_slab") { it } to { _, _, _, random ->
            if (random.nextBoolean()) SimpleBlockState.from("minecraft:mud_brick_slab") { it } else SimpleBlockState.from("minecraft:spruce_slab") { it }
        },
        SimpleBlockState.from("minecraft:light_blue_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:glass_pane") { it } },
        SimpleBlockState.from("minecraft:oak_door") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:spruce_door") { it } },
        SimpleBlockState.from("minecraft:green_wool") { it } to { _, height, dist, random ->
            if (height < 2 && dist < 3) SimpleBlockState.from("minecraft:moss_block") { it }
            else if (random.nextFloat() < 0.5f) SimpleBlockState.from("minecraft:oak_leaves") { it }
            else SimpleBlockState.from("minecraft:azalea_leaves") { it }
        },
        SimpleBlockState.from("minecraft:yellow_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:lantern") { it } },
        SimpleBlockState.from("minecraft:blue_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:crafting_table") { it } },
        SimpleBlockState.from("minecraft:magenta_wool") { it } to { _, _, _, random ->
            if (random.nextBoolean()) SimpleBlockState.from("minecraft:spruce_fence") { it } else SimpleBlockState.from("minecraft:dark_oak_fence") { it }
        },
        SimpleBlockState.from("minecraft:orange_wool") { it } to { _, height, _, _ ->
            if (height > 4) SimpleBlockState.from("minecraft:mud_bricks") { it } else SimpleBlockState.from("minecraft:packed_mud") { it }
        },
        SimpleBlockState.from("minecraft:purple_wool") { it } to { _, _, _, random ->
            if (random.nextFloat() < 0.3f) SimpleBlockState.from("minecraft:smoker") { it }
            else SimpleBlockState.from("minecraft:barrel") { it }
        },
        SimpleBlockState.from("minecraft:lime_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:air") { it } },
        SimpleBlockState.from("minecraft:pink_wool") { it } to { _, height, _, random ->
            if (height > 4) SimpleBlockState.from("minecraft:chiseled_deepslate") { it }
            else if (random.nextFloat() < 0.5f) SimpleBlockState.from("minecraft:smooth_stone") { it }
            else SimpleBlockState.from("minecraft:polished_andesite") { it }
        }
    )
    val modernUrbanReplacementPalette: Map<PlatformBlockState<*>, PaletteFunction> = mapOf(
        SimpleBlockState.from("minecraft:white_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:quartz_block") { it } },
        SimpleBlockState.from("minecraft:gray_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:stone") { it } },
        SimpleBlockState.from("minecraft:red_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:red_concrete") { it } },
        SimpleBlockState.from("minecraft:blue_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:light_blue_concrete") { it } },
        SimpleBlockState.from("minecraft:yellow_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:yellow_concrete") { it } },
        SimpleBlockState.from("minecraft:black_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:black_concrete") { it } },
        SimpleBlockState.from("minecraft:light_blue_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:glass") { it } },
        SimpleBlockState.from("minecraft:lime_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:sea_lantern") { it } },
        SimpleBlockState.from("minecraft:green_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:grass_block") { it } },
        SimpleBlockState.from("minecraft:pink_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:magenta_concrete") { it } }
    )
    val japaneseTraditionalPalette: Map<PlatformBlockState<*>, PaletteFunction> = mapOf(
        SimpleBlockState.from("minecraft:white_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:smooth_quartz") { it } },
        SimpleBlockState.from("minecraft:gray_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:polished_andesite") { it } },
        SimpleBlockState.from("minecraft:red_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:cherry_planks") { it } },
        SimpleBlockState.from("minecraft:black_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:deepslate_tiles") { it } },
        SimpleBlockState.from("minecraft:brown_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:dark_oak_planks") { it } },
        SimpleBlockState.from("minecraft:stripped_oak_wood") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:stripped_dark_oak_log") { it } },
        SimpleBlockState.from("minecraft:light_blue_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:paper") { it } },
        SimpleBlockState.from("minecraft:yellow_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:lantern") { it } },
        SimpleBlockState.from("minecraft:green_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:bamboo") { it } },
        SimpleBlockState.from("minecraft:orange_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:smooth_red_sandstone") { it } }
    )
    val futuristicPalette: Map<PlatformBlockState<*>, PaletteFunction> = mapOf(
        SimpleBlockState.from("minecraft:white_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:end_rod") { it } },
        SimpleBlockState.from("minecraft:light_blue_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:sea_lantern") { it } },
        SimpleBlockState.from("minecraft:gray_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:iron_block") { it } },
        SimpleBlockState.from("minecraft:black_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:obsidian") { it } },
        SimpleBlockState.from("minecraft:blue_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:prismarine_bricks") { it } },
        SimpleBlockState.from("minecraft:red_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:redstone_block") { it } },
        SimpleBlockState.from("minecraft:pink_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:purpur_block") { it } },
        SimpleBlockState.from("minecraft:yellow_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:beacon") { it } },
        SimpleBlockState.from("minecraft:lime_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:light_weighted_pressure_plate") { it } },
        SimpleBlockState.from("minecraft:green_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:shroomlight") { it } }
    )
    val japaneseUrbanPalette: Map<PlatformBlockState<*>, PaletteFunction> = mapOf(
        SimpleBlockState.from("minecraft:white_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:white_concrete") { it } },
        SimpleBlockState.from("minecraft:gray_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:light_gray_concrete") { it } },
        SimpleBlockState.from("minecraft:red_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:red_terracotta") { it } },
        SimpleBlockState.from("minecraft:blue_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:blue_concrete") { it } },
        SimpleBlockState.from("minecraft:light_blue_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:glass_pane") { it } },
        SimpleBlockState.from("minecraft:yellow_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:glowstone") { it } },
        SimpleBlockState.from("minecraft:green_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:oak_leaves") { it } },
        SimpleBlockState.from("minecraft:lime_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:green_concrete_powder") { it } },
        SimpleBlockState.from("minecraft:black_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:coal_block") { it } },
        SimpleBlockState.from("minecraft:orange_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:orange_terracotta") { it } }
    )
    val industrialPalette: Map<PlatformBlockState<*>, PaletteFunction> = mapOf(
        SimpleBlockState.from("minecraft:gray_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:polished_andesite") { it } },
        SimpleBlockState.from("minecraft:red_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:redstone_block") { it } },
        SimpleBlockState.from("minecraft:black_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:coal_block") { it } },
        SimpleBlockState.from("minecraft:orange_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:copper_block") { it } },
        SimpleBlockState.from("minecraft:yellow_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:lightning_rod") { it } },
        SimpleBlockState.from("minecraft:blue_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:lodestone") { it } },
        SimpleBlockState.from("minecraft:green_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:slime_block") { it } },
        SimpleBlockState.from("minecraft:light_blue_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:iron_bars") { it } },
        SimpleBlockState.from("minecraft:brown_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:oxidized_copper") { it } }, // If using a custom mod
        SimpleBlockState.from("minecraft:purple_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:blast_furnace") { it } }
    )
    val desertCityPalette: Map<PlatformBlockState<*>, PaletteFunction> = mapOf(
        SimpleBlockState.from("minecraft:white_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:sandstone") { it } },
        SimpleBlockState.from("minecraft:yellow_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:smooth_sandstone") { it } },
        SimpleBlockState.from("minecraft:orange_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:red_sandstone") { it } },
        SimpleBlockState.from("minecraft:red_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:terracotta") { it } },
        SimpleBlockState.from("minecraft:brown_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:packed_mud") { it } },
        SimpleBlockState.from("minecraft:light_blue_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:cyan_stained_glass") { it } },
        SimpleBlockState.from("minecraft:gray_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:chiseled_sandstone") { it } },
        SimpleBlockState.from("minecraft:green_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:dead_bush") { it } },
        SimpleBlockState.from("minecraft:black_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:chiseled_red_sandstone") { it } },
        SimpleBlockState.from("minecraft:pink_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:bricks") { it } }
    )
    val villageRusticPalette: Map<PlatformBlockState<*>, PaletteFunction> = mapOf(
        SimpleBlockState.from("minecraft:white_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:oak_planks") { it } },
        SimpleBlockState.from("minecraft:yellow_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:hay_block") { it } },
        SimpleBlockState.from("minecraft:red_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:bricks") { it } },
        SimpleBlockState.from("minecraft:brown_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:dirt") { it } },
        SimpleBlockState.from("minecraft:green_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:oak_leaves") { it } },
        SimpleBlockState.from("minecraft:light_blue_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:glass") { it } },
        SimpleBlockState.from("minecraft:orange_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:pumpkin") { it } },
        SimpleBlockState.from("minecraft:gray_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:gravel") { it } },
        SimpleBlockState.from("minecraft:pink_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:cauldron") { it } },
        SimpleBlockState.from("minecraft:magenta_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:composter") { it } }
    )
    val overgrownPalette: Map<PlatformBlockState<*>, PaletteFunction> = mapOf(
        SimpleBlockState.from("minecraft:green_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:moss_block") { it } },
        SimpleBlockState.from("minecraft:lime_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:moss_carpet") { it } },
        SimpleBlockState.from("minecraft:brown_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:mud") { it } },
        SimpleBlockState.from("minecraft:white_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:spore_blossom") { it } },
        SimpleBlockState.from("minecraft:gray_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:deepslate") { it } },
        SimpleBlockState.from("minecraft:light_blue_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:vine") { it } },
        SimpleBlockState.from("minecraft:yellow_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:flowering_azalea_leaves") { it } },
        SimpleBlockState.from("minecraft:orange_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:rooted_dirt") { it } },
        SimpleBlockState.from("minecraft:purple_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:shroomlight") { it } },
        SimpleBlockState.from("minecraft:pink_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:large_fern") { it } }
    )
    val nordicPalette: Map<PlatformBlockState<*>, PaletteFunction> = mapOf(
        SimpleBlockState.from("minecraft:white_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:snow_block") { it } },
        SimpleBlockState.from("minecraft:light_blue_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:ice") { it } },
        SimpleBlockState.from("minecraft:blue_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:packed_ice") { it } },
        SimpleBlockState.from("minecraft:gray_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:stone_bricks") { it } },
        SimpleBlockState.from("minecraft:red_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:spruce_planks") { it } },
        SimpleBlockState.from("minecraft:green_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:spruce_leaves") { it } },
        SimpleBlockState.from("minecraft:brown_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:stripped_spruce_log") { it } },
        SimpleBlockState.from("minecraft:yellow_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:lantern") { it } },
        SimpleBlockState.from("minecraft:purple_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:soul_lantern") { it } },
        SimpleBlockState.from("minecraft:black_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:blackstone") { it } }
    )
    val steampunkPalette: Map<PlatformBlockState<*>, PaletteFunction> = mapOf(
        SimpleBlockState.from("minecraft:brown_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:copper_block") { it } },
        SimpleBlockState.from("minecraft:orange_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:exposed_copper") { it } },
        SimpleBlockState.from("minecraft:yellow_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:waxed_copper_block") { it } }, // If using modded material
        SimpleBlockState.from("minecraft:gray_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:stone_bricks") { it } },
        SimpleBlockState.from("minecraft:red_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:red_nether_bricks") { it } },
        SimpleBlockState.from("minecraft:light_blue_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:iron_bars") { it } },
        SimpleBlockState.from("minecraft:purple_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:piston") { it } },
        SimpleBlockState.from("minecraft:green_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:mossy_cobblestone") { it } },
        SimpleBlockState.from("minecraft:black_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:coal_block") { it } },
        SimpleBlockState.from("minecraft:white_wool") { it } to { _, _, _, _ -> SimpleBlockState.from("minecraft:chiseled_quartz_block") { it } }
    )

}