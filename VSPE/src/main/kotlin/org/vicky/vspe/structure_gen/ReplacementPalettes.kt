package org.vicky.vspe.structure_gen

import org.bukkit.Material
import org.vicky.nms.PaletteFunction

object ReplacementPalettes {
    val medievalReplacementPalette: Map<Material, PaletteFunction> = mapOf(
        Material.WHITE_WOOL to { _, height, _, random ->
            if (height > 5) Material.COBBLESTONE
            else if (random.nextFloat() < 0.3f) Material.MOSSY_COBBLESTONE
            else Material.ANDESITE
        },
        Material.GRAY_WOOL to { _, _, _, random ->
            if (random.nextBoolean()) Material.DEEPSLATE_BRICKS else Material.POLISHED_ANDESITE
        },
        Material.RED_WOOL to { _, height, _, _ ->
            if (height > 3) Material.SPRUCE_PLANKS else Material.STRIPPED_SPRUCE_LOG
        },
        Material.STRIPPED_OAK_WOOD to { _, height, _, _ ->
            if (height > 6) Material.STRIPPED_DARK_OAK_LOG else Material.DARK_OAK_LOG
        },
        Material.OAK_STAIRS to { _, height, _, random ->
            if (height % 2 == 0) Material.SPRUCE_STAIRS
            else if (random.nextFloat() < 0.2f) Material.DARK_OAK_STAIRS
            else Material.MANGROVE_STAIRS
        },
        Material.RED_SANDSTONE_SLAB to { _, _, _, random ->
            if (random.nextBoolean()) Material.MUD_BRICK_SLAB else Material.SPRUCE_SLAB
        },
        Material.LIGHT_BLUE_WOOL to { _, _, _, _ -> Material.GLASS_PANE },
        Material.OAK_DOOR to { _, _, _, _ -> Material.SPRUCE_DOOR },
        Material.GREEN_WOOL to { _, height, dist, random ->
            if (height < 2 && dist < 3) Material.MOSS_BLOCK
            else if (random.nextFloat() < 0.5f) Material.OAK_LEAVES
            else Material.AZALEA_LEAVES
        },
        Material.YELLOW_WOOL to { _, _, _, _ -> Material.LANTERN },
        Material.BLUE_WOOL to { _, _, _, _ -> Material.CRAFTING_TABLE },
        Material.MAGENTA_WOOL to { _, _, _, random ->
            if (random.nextBoolean()) Material.SPRUCE_FENCE else Material.DARK_OAK_FENCE
        },
        Material.ORANGE_WOOL to { _, height, _, _ ->
            if (height > 4) Material.MUD_BRICKS else Material.PACKED_MUD
        },
        Material.PURPLE_WOOL to { _, _, _, random ->
            if (random.nextFloat() < 0.3f) Material.SMOKER
            else Material.BARREL
        },
        Material.LIME_WOOL to { _, _, _, _ -> Material.AIR },
        Material.PINK_WOOL to { _, height, _, random ->
            if (height > 4) Material.CHISELED_DEEPSLATE
            else if (random.nextFloat() < 0.5f) Material.SMOOTH_STONE
            else Material.POLISHED_ANDESITE
        }
    )
    val modernUrbanReplacementPalette: Map<Material, PaletteFunction> = mapOf(
        Material.WHITE_WOOL to { _, _, _, _ -> Material.QUARTZ_BLOCK },
        Material.GRAY_WOOL to { _, _, _, _ -> Material.STONE },
        Material.RED_WOOL to { _, _, _, _ -> Material.RED_CONCRETE },
        Material.BLUE_WOOL to { _, _, _, _ -> Material.LIGHT_BLUE_CONCRETE },
        Material.YELLOW_WOOL to { _, _, _, _ -> Material.YELLOW_CONCRETE },
        Material.BLACK_WOOL to { _, _, _, _ -> Material.BLACK_CONCRETE },
        Material.LIGHT_BLUE_WOOL to { _, _, _, _ -> Material.GLASS },
        Material.LIME_WOOL to { _, _, _, _ -> Material.SEA_LANTERN },
        Material.GREEN_WOOL to { _, _, _, _ -> Material.GRASS_BLOCK },
        Material.PINK_WOOL to { _, _, _, _ -> Material.MAGENTA_CONCRETE }
    )
    val japaneseTraditionalPalette: Map<Material, PaletteFunction> = mapOf(
        Material.WHITE_WOOL to { _, _, _, _ -> Material.SMOOTH_QUARTZ },
        Material.GRAY_WOOL to { _, _, _, _ -> Material.POLISHED_ANDESITE },
        Material.RED_WOOL to { _, _, _, _ -> Material.CHERRY_PLANKS },
        Material.BLACK_WOOL to { _, _, _, _ -> Material.DEEPSLATE_TILES },
        Material.BROWN_WOOL to { _, _, _, _ -> Material.DARK_OAK_PLANKS },
        Material.STRIPPED_OAK_WOOD to { _, _, _, _ -> Material.STRIPPED_DARK_OAK_LOG },
        Material.LIGHT_BLUE_WOOL to { _, _, _, _ -> Material.PAPER },
        Material.YELLOW_WOOL to { _, _, _, _ -> Material.LANTERN },
        Material.GREEN_WOOL to { _, _, _, _ -> Material.BAMBOO },
        Material.ORANGE_WOOL to { _, _, _, _ -> Material.SMOOTH_RED_SANDSTONE }
    )
    val futuristicPalette: Map<Material, PaletteFunction> = mapOf(
        Material.WHITE_WOOL to { _, _, _, _ -> Material.END_ROD },
        Material.LIGHT_BLUE_WOOL to { _, _, _, _ -> Material.SEA_LANTERN },
        Material.GRAY_WOOL to { _, _, _, _ -> Material.IRON_BLOCK },
        Material.BLACK_WOOL to { _, _, _, _ -> Material.OBSIDIAN },
        Material.BLUE_WOOL to { _, _, _, _ -> Material.PRISMARINE_BRICKS },
        Material.RED_WOOL to { _, _, _, _ -> Material.REDSTONE_BLOCK },
        Material.PINK_WOOL to { _, _, _, _ -> Material.PURPUR_BLOCK },
        Material.YELLOW_WOOL to { _, _, _, _ -> Material.BEACON },
        Material.LIME_WOOL to { _, _, _, _ -> Material.LIGHT_WEIGHTED_PRESSURE_PLATE },
        Material.GREEN_WOOL to { _, _, _, _ -> Material.SHROOMLIGHT }
    )
    val japaneseUrbanPalette: Map<Material, PaletteFunction> = mapOf(
        Material.WHITE_WOOL to { _, _, _, _ -> Material.WHITE_CONCRETE },
        Material.GRAY_WOOL to { _, _, _, _ -> Material.LIGHT_GRAY_CONCRETE },
        Material.RED_WOOL to { _, _, _, _ -> Material.RED_TERRACOTTA },
        Material.BLUE_WOOL to { _, _, _, _ -> Material.BLUE_CONCRETE },
        Material.LIGHT_BLUE_WOOL to { _, _, _, _ -> Material.GLASS_PANE },
        Material.YELLOW_WOOL to { _, _, _, _ -> Material.GLOWSTONE },
        Material.GREEN_WOOL to { _, _, _, _ -> Material.OAK_LEAVES },
        Material.LIME_WOOL to { _, _, _, _ -> Material.GREEN_CONCRETE_POWDER },
        Material.BLACK_WOOL to { _, _, _, _ -> Material.COAL_BLOCK },
        Material.ORANGE_WOOL to { _, _, _, _ -> Material.ORANGE_TERRACOTTA }
    )
    val industrialPalette: Map<Material, PaletteFunction> = mapOf(
        Material.GRAY_WOOL to { _, _, _, _ -> Material.POLISHED_ANDESITE },
        Material.RED_WOOL to { _, _, _, _ -> Material.REDSTONE_BLOCK },
        Material.BLACK_WOOL to { _, _, _, _ -> Material.COAL_BLOCK },
        Material.ORANGE_WOOL to { _, _, _, _ -> Material.COPPER_BLOCK },
        Material.YELLOW_WOOL to { _, _, _, _ -> Material.LIGHTNING_ROD },
        Material.BLUE_WOOL to { _, _, _, _ -> Material.LODESTONE },
        Material.GREEN_WOOL to { _, _, _, _ -> Material.SLIME_BLOCK },
        Material.LIGHT_BLUE_WOOL to { _, _, _, _ -> Material.IRON_BARS },
        Material.BROWN_WOOL to { _, _, _, _ -> Material.OXIDIZED_COPPER }, // If using a custom mod
        Material.PURPLE_WOOL to { _, _, _, _ -> Material.BLAST_FURNACE }
    )
    val desertCityPalette: Map<Material, PaletteFunction> = mapOf(
        Material.WHITE_WOOL to { _, _, _, _ -> Material.SANDSTONE },
        Material.YELLOW_WOOL to { _, _, _, _ -> Material.SMOOTH_SANDSTONE },
        Material.ORANGE_WOOL to { _, _, _, _ -> Material.RED_SANDSTONE },
        Material.RED_WOOL to { _, _, _, _ -> Material.TERRACOTTA },
        Material.BROWN_WOOL to { _, _, _, _ -> Material.PACKED_MUD },
        Material.LIGHT_BLUE_WOOL to { _, _, _, _ -> Material.CYAN_STAINED_GLASS },
        Material.GRAY_WOOL to { _, _, _, _ -> Material.CHISELED_SANDSTONE },
        Material.GREEN_WOOL to { _, _, _, _ -> Material.DEAD_BUSH },
        Material.BLACK_WOOL to { _, _, _, _ -> Material.CHISELED_RED_SANDSTONE },
        Material.PINK_WOOL to { _, _, _, _ -> Material.BRICKS }
    )
    val villageRusticPalette: Map<Material, PaletteFunction> = mapOf(
        Material.WHITE_WOOL to { _, _, _, _ -> Material.OAK_PLANKS },
        Material.YELLOW_WOOL to { _, _, _, _ -> Material.HAY_BLOCK },
        Material.RED_WOOL to { _, _, _, _ -> Material.BRICKS },
        Material.BROWN_WOOL to { _, _, _, _ -> Material.DIRT },
        Material.GREEN_WOOL to { _, _, _, _ -> Material.OAK_LEAVES },
        Material.LIGHT_BLUE_WOOL to { _, _, _, _ -> Material.GLASS },
        Material.ORANGE_WOOL to { _, _, _, _ -> Material.PUMPKIN },
        Material.GRAY_WOOL to { _, _, _, _ -> Material.GRAVEL },
        Material.PINK_WOOL to { _, _, _, _ -> Material.CAULDRON },
        Material.MAGENTA_WOOL to { _, _, _, _ -> Material.COMPOSTER }
    )
    val overgrownPalette: Map<Material, PaletteFunction> = mapOf(
        Material.GREEN_WOOL to { _, _, _, _ -> Material.MOSS_BLOCK },
        Material.LIME_WOOL to { _, _, _, _ -> Material.MOSS_CARPET },
        Material.BROWN_WOOL to { _, _, _, _ -> Material.MUD },
        Material.WHITE_WOOL to { _, _, _, _ -> Material.SPORE_BLOSSOM },
        Material.GRAY_WOOL to { _, _, _, _ -> Material.DEEPSLATE },
        Material.LIGHT_BLUE_WOOL to { _, _, _, _ -> Material.VINE },
        Material.YELLOW_WOOL to { _, _, _, _ -> Material.FLOWERING_AZALEA_LEAVES },
        Material.ORANGE_WOOL to { _, _, _, _ -> Material.ROOTED_DIRT },
        Material.PURPLE_WOOL to { _, _, _, _ -> Material.SHROOMLIGHT },
        Material.PINK_WOOL to { _, _, _, _ -> Material.LARGE_FERN }
    )
    val nordicPalette: Map<Material, PaletteFunction> = mapOf(
        Material.WHITE_WOOL to { _, _, _, _ -> Material.SNOW_BLOCK },
        Material.LIGHT_BLUE_WOOL to { _, _, _, _ -> Material.ICE },
        Material.BLUE_WOOL to { _, _, _, _ -> Material.PACKED_ICE },
        Material.GRAY_WOOL to { _, _, _, _ -> Material.STONE_BRICKS },
        Material.RED_WOOL to { _, _, _, _ -> Material.SPRUCE_PLANKS },
        Material.GREEN_WOOL to { _, _, _, _ -> Material.SPRUCE_LEAVES },
        Material.BROWN_WOOL to { _, _, _, _ -> Material.STRIPPED_SPRUCE_LOG },
        Material.YELLOW_WOOL to { _, _, _, _ -> Material.LANTERN },
        Material.PURPLE_WOOL to { _, _, _, _ -> Material.SOUL_LANTERN },
        Material.BLACK_WOOL to { _, _, _, _ -> Material.BLACKSTONE }
    )
    val steampunkPalette: Map<Material, PaletteFunction> = mapOf(
        Material.BROWN_WOOL to { _, _, _, _ -> Material.COPPER_BLOCK },
        Material.ORANGE_WOOL to { _, _, _, _ -> Material.EXPOSED_COPPER },
        Material.YELLOW_WOOL to { _, _, _, _ -> Material.WAXED_COPPER_BLOCK }, // If using modded material
        Material.GRAY_WOOL to { _, _, _, _ -> Material.STONE_BRICKS },
        Material.RED_WOOL to { _, _, _, _ -> Material.RED_NETHER_BRICKS },
        Material.LIGHT_BLUE_WOOL to { _, _, _, _ -> Material.IRON_BARS },
        Material.PURPLE_WOOL to { _, _, _, _ -> Material.PISTON },
        Material.GREEN_WOOL to { _, _, _, _ -> Material.MOSSY_COBBLESTONE },
        Material.BLACK_WOOL to { _, _, _, _ -> Material.COAL_BLOCK },
        Material.WHITE_WOOL to { _, _, _, _ -> Material.CHISELED_QUARTZ_BLOCK }
    )

}