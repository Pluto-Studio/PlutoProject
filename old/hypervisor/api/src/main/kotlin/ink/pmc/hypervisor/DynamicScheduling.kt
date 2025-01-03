package ink.pmc.hypervisor

import ink.pmc.framework.inject.inlinedGet
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.entity.SpawnCategory

interface DynamicScheduling {
    companion object : DynamicScheduling by inlinedGet()

    val enabled: Boolean
    val viewDistanceEnabled: Boolean
    val simulateDistanceEnabled: Boolean
    val spawnLimitsEnabled: Boolean
    val ticksPerSpawnEnabled: Boolean
    val isRunning: Boolean

    fun start()

    fun stop()

    fun setViewDistance(player: Player, state: Boolean)

    fun setViewDistanceLocally(player: Player, state: DynamicViewDistanceState)

    fun toggleViewDistance(player: Player)

    fun getViewDistance(player: Player): Boolean

    fun getViewDistanceLocally(player: Player): DynamicViewDistanceState

    fun removeLocalViewDistanceState(player: Player)

    fun calculateCurves()

    fun getSimulateDistanceWhen(millsPerSecond: Double, world: World?): Int

    fun getSimulateDistanceCurve(world: World?): PolynomialFunction

    fun getSpawnLimitWhen(millsPerSecond: Double, world: World? = null, category: SpawnCategory): Int

    fun getSpawnLimitsCurve(world: World? = null, category: SpawnCategory): PolynomialFunction?

    fun getTicksPerSpawnWhen(millsPerSecond: Double, world: World? = null, category: SpawnCategory): Int

    fun getTicksPerSpawnCurve(world: World? = null, category: SpawnCategory): PolynomialFunction?
}
