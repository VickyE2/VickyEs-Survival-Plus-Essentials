package org.vicky.vspe.systems.dimension

import org.vicky.platform.PlatformPlayer
import org.vicky.platform.world.PlatformLocation
import org.vicky.vspe.platform.systems.dimension.PlatformBaseDimension
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

interface DimensionSpawnStrategy<T, N> {
    fun resolveSpawn(
        player: PlatformPlayer,
        dimension: PlatformBaseDimension<T, N>,
        portalContext: PortalContext<T, N>? = null
    ): PlatformLocation?
}

/**
 * @param sourceLocation
 * @param player
 * @param beforeSpawning Things the context should do like placing portal frame...etc
 */
data class PortalContext<T, N>(
    val sourceLocation: PlatformLocation,
    val player: PlatformPlayer,
    val beforeSpawning: ((Int, Int, Int, PlatformBaseDimension<T, N>) -> Unit)?
)

class SafeRandomLandSpawnStrategy<T, N>(
    private val maxAttempts: Int = 20,
    private val xzRadius: Int = 256,
    private val minY: Int = 64,
    private val maxY: Int = 128
) : DimensionSpawnStrategy<T, N> {
    override fun resolveSpawn(
        player: PlatformPlayer,
        dimension: PlatformBaseDimension<T, N>,
        portalContext: PortalContext<T, N>?
    ): PlatformLocation? {
        val random = Random(player.uniqueId().mostSignificantBits xor System.currentTimeMillis())

        repeat(maxAttempts) {
            val x = random.nextInt(-xzRadius, xzRadius)
            val z = random.nextInt(-xzRadius, xzRadius)
            for (y in maxY downTo minY) {
                val location = dimension.locationAt(x.toDouble(), y.toDouble(), z.toDouble())
                if (dimension.isSafeSpawnLocation(location)) {
                    portalContext?.beforeSpawning?.invoke(x.toInt(), y.toInt() + 1, z.toInt(), dimension)
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

class FixedRingSpawnStrategy<T, N>(
    private val center: PlatformLocation,
    private val radius: Double,
    private val playerIndexProvider: (PlatformPlayer) -> Int
) : DimensionSpawnStrategy<T, N> {

    override fun resolveSpawn(
        player: PlatformPlayer,
        dimension: PlatformBaseDimension<T, N>,
        portalContext: PortalContext<T, N>?
    ): PlatformLocation? {
        val index = playerIndexProvider(player)
        val angle = (index * 137.5) % 360.0 // golden angle spacing

        val rad = Math.toRadians(angle)
        val x = center.x + radius * cos(rad)
        val z = center.z + radius * sin(rad)
        val groundY = dimension.findGroundYAt(x.toInt(), z.toInt()) ?: return null

        portalContext?.beforeSpawning?.invoke(
            x.toInt(),
            groundY.toInt() + 1,
            z.toInt(),
            dimension
        ) // setup logic (like placing a frame)
        return dimension.locationAt(x, groundY + 1.0, z)
    }
}

class PortalLinkedStrategy<T, N>(
    private val scaleFactor: Double = 8.0 // nether-style mapping
) : DimensionSpawnStrategy<T, N> {
    override fun resolveSpawn(
        player: PlatformPlayer,
        dimension: PlatformBaseDimension<T, N>,
        portalContext: PortalContext<T, N>?
    ): PlatformLocation? {
        if (portalContext == null) return null

        val source = portalContext.sourceLocation
        val mappedX = source.x * scaleFactor
        val mappedZ = source.z * scaleFactor
        val groundY = dimension.findGroundYAt(mappedX.toInt(), mappedZ.toInt()) ?: 80.0

        portalContext.beforeSpawning?.invoke(
            mappedX.toInt(),
            groundY.toInt() + 1,
            mappedZ.toInt(),
            dimension
        ) // setup logic (like placing a frame)

        return dimension.locationAt(mappedX, groundY + 1.0, mappedZ)
    }
}