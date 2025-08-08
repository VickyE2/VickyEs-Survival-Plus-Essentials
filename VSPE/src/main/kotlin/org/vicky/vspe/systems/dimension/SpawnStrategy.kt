package org.vicky.vspe.systems.dimension

import org.vicky.platform.PlatformPlayer
import org.vicky.platform.world.PlatformLocation
import org.vicky.vspe.platform.systems.dimension.PlatformBaseDimension
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

interface DimensionSpawnStrategy {
    fun resolveSpawn(
        player: PlatformPlayer,
        dimension: PlatformBaseDimension,
        portalContext: PortalContext? = null
    ): PlatformLocation?
}

/**
 * @param sourceLocation
 * @param sourceDimension
 * @param targetDimension
 * @param player
 * @param beforeSpawning Things the context should do like placing portal frame...etc
 */
data class PortalContext(
    val sourceLocation: PlatformLocation,
    val sourceDimension: PlatformBaseDimension,
    val targetDimension: PlatformBaseDimension,
    val player: PlatformPlayer,
    val beforeSpawning: (() -> Unit)?
)



class SafeRandomLandSpawnStrategy(
    private val maxAttempts: Int = 20,
    private val xzRadius: Int = 256,
    private val minY: Int = 64,
    private val maxY: Int = 128
) : DimensionSpawnStrategy {

    override fun resolveSpawn(
        player: PlatformPlayer,
        dimension: PlatformBaseDimension,
        portalContext: PortalContext?
    ): PlatformLocation? {
        val random = Random(player.uniqueId().mostSignificantBits xor System.currentTimeMillis())

        repeat(maxAttempts) {
            val x = random.nextInt(-xzRadius, xzRadius)
            val z = random.nextInt(-xzRadius, xzRadius)
            for (y in maxY downTo minY) {
                val location = dimension.locationAt(x.toDouble(), y.toDouble(), z.toDouble())
                if (dimension.isSafeSpawnLocation(location)) {
                    portalContext?.beforeSpawning?.invoke() // setup logic (like placing a frame)
                    return location.offset(0.5, 1.0, 0.5) // center and elevate
                }
            }
        }
        return null // failed to find a safe spot
    }
}

private fun PlatformLocation?.offset(
    x: Double,
    y: Double,
    z: Double
): PlatformLocation? {
    return if (this != null) PlatformLocation(this.world, this.x - x, this.y - y, this.z - z) else null
}


class FixedRingSpawnStrategy(
    private val center: PlatformLocation,
    private val radius: Double,
    private val playerIndexProvider: (PlatformPlayer) -> Int
) : DimensionSpawnStrategy {

    override fun resolveSpawn(
        player: PlatformPlayer,
        dimension: PlatformBaseDimension,
        portalContext: PortalContext?
    ): PlatformLocation? {
        val index = playerIndexProvider(player)
        val angle = (index * 137.5) % 360.0 // golden angle spacing

        val rad = Math.toRadians(angle)
        val x = center.x + radius * cos(rad)
        val z = center.z + radius * sin(rad)

        portalContext?.beforeSpawning?.invoke() // setup logic (like placing a frame)

        val groundY = dimension.findGroundYAt(x.toInt(), z.toInt()) ?: return null
        return dimension.locationAt(x, groundY + 1.0, z)
    }
}



class PortalLinkedStrategy(
    private val scaleFactor: Double = 8.0 // nether-style mapping
) : DimensionSpawnStrategy {

    override fun resolveSpawn(
        player: PlatformPlayer,
        dimension: PlatformBaseDimension,
        portalContext: PortalContext?
    ): PlatformLocation? {
        if (portalContext == null) return null

        val source = portalContext.sourceLocation
        val mappedX = source.x * scaleFactor
        val mappedZ = source.z * scaleFactor
        val groundY = dimension.findGroundYAt(mappedX.toInt(), mappedZ.toInt()) ?: 80.0

        portalContext.beforeSpawning?.invoke() // setup logic (like placing a frame)

        return dimension.locationAt(mappedX, groundY + 1.0, mappedZ)
    }
}

