package org.vicky.vspe

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
