package plutoproject.feature.randomteleport.paper

import org.bukkit.block.Biome
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO

data class RandomTeleportConfig(
    val cacheInterval: Int = 100,
    val cooldown: Duration = Duration.parse("30s"),
    val grayTest: GrayTestConfig = GrayTestConfig(),
    val default: Options = Options(),
    val worlds: Map<String, Options> = emptyMap(),
    val enabledWorlds: List<String> = listOf("world")
)

data class GrayTestConfig(
    val enabled: Boolean = false,
    val cooldown: Duration = Duration.parse("300s"),
    val defaultCost: Double = 10.0,
    val worldCosts: Map<String, Double> = emptyMap(),
    val existingPlayersEnabled: Boolean = false,
    val existingPlayersPercentage: Int = 0,
) {
    init {
        require(cooldown >= ZERO) { "gray-test.cooldown must not be negative" }
        require(defaultCost >= 0.0) { "gray-test.default-cost must not be negative" }
        require(worldCosts.values.all { it >= 0.0 }) { "gray-test.world-costs must not contain negative values" }
        require(existingPlayersPercentage in 0..100) {
            "gray-test.existing-players-percentage must be between 0 and 100"
        }
    }
}

data class Options(
    val spawnpointAsCenter: Boolean = true,
    val center: Center = Center(),
    val cacheAmount: Int = 5,
    val chunkPreserveRadius: Int = -1,
    val startRadius: Int = 0,
    val endRadius: Int = 10000,
    val maxHeight: Int = -1,
    val minHeight: Int = -1,
    val noCover: Boolean = true,
    val maxAttempts: Int = 5,
    val cost: Double = 0.0,
    val blacklistedBiomes: List<Biome> = emptyList(),
)

data class Center(
    val x: Double = 0.0,
    val z: Double = 0.0
)
