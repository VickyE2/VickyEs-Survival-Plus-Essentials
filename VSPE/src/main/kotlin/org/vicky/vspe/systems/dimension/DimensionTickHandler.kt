package org.vicky.vspe.systems.dimension

import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.vicky.vspe.utilities.global.GlobalResources.dimensionManager

interface DimensionTickHandler {
    fun tick(players: List<Player>, world: World)
}

object DimensionTickLoop : BukkitRunnable() {
    override fun run() {
        dimensionManager.LOADED_DIMENSIONS.forEach { it.tick() }
    }
}
