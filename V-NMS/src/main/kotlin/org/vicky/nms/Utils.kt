package org.vicky.nms

import net.minecraft.core.BlockPos
import net.minecraft.util.RandomSource
import org.bukkit.Material

typealias PaletteFunction = (
    pos: BlockPos,
    height: Int,
    distanceFromCenter: Int,
    random: RandomSource
) -> Material