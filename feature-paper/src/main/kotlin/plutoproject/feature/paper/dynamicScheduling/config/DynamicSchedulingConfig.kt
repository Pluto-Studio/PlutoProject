package plutoproject.feature.paper.dynamicScheduling.config

import plutoproject.feature.paper.dynamicScheduling.Double2IntSample
import plutoproject.feature.paper.dynamicScheduling.SpawnStrategy
import kotlin.time.Duration

data class DynamicSchedulingConfig(
    val cyclePeriod: Duration = Duration.parse("5s"),
    val viewDistance: ViewDistance = ViewDistance(),
    val simulateDistance: SimulateDistance = SimulateDistance(),
    val spawnLimits: SpawnSettings = SpawnSettings(),
    val ticksPerSpawn: SpawnSettings = SpawnSettings(),
)

data class ViewDistance(
    val enabled: Boolean = false,
    val virtualHosts: List<String> = emptyList(),
    val maximumPing: Double = 100.0,
    val standard: Int = 10,
    val boost: Int = 16,
)

data class SimulateDistance(
    val enabled: Boolean = false,
    val default: Double2IntSample = emptyList(),
    val world: Map<String, Double2IntSample> = emptyMap(),
)

data class SpawnSettings(
    val enabled: Boolean = false,
    val default: SpawnStrategy = emptyMap(),
    val world: Map<String, SpawnStrategy> = emptyMap(),
)
