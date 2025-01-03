package ink.pmc.hypervisor.config

import ink.pmc.hypervisor.Double2IntSample
import ink.pmc.hypervisor.SpawnStrategy
import kotlin.time.Duration

data class DynamicScheduling(
    val enabled: Boolean,
    val cyclePeriod: Duration,
    val viewDistance: ViewDistance,
    val simulateDistance: SimulateDistance,
    val spawnLimits: SpawnSettings,
    val ticksPerSpawn: SpawnSettings,
)

data class ViewDistance(
    val enabled: Boolean,
    val virtualHosts: List<String>,
    val maximumPing: Double,
    val standard: Int,
    val boost: Int
)

data class SimulateDistance(
    val enabled: Boolean,
    val default: Double2IntSample,
    val world: Map<String, Double2IntSample>,
)

data class SpawnSettings(
    val enabled: Boolean,
    val default: SpawnStrategy,
    val world: Map<String, SpawnStrategy>
)