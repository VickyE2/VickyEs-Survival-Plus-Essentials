package org.vicky.vspe.systems.dimension

import org.vicky.platform.PlatformPlayer
import org.vicky.platform.world.PlatformWorld
import org.vicky.vspe.platform.VSPEPlatformPlugin

interface PlatformDimensionTickHandler {
    fun tick(players: List<PlatformPlayer>, world: PlatformWorld<*, *>)
}

object DimensionTickLoop : Runnable {
    override fun run() {
        VSPEPlatformPlugin.dimensionManager().getLoadedDimensions().forEach { it.tick() }
    }
}
