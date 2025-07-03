package plutoproject.feature.paper.randomTeleport

import org.bukkit.block.Biome
import kotlin.time.Duration

data class RandomTeleportConfig(
    val cacheInterval: Int = 100,
    val cooldown: Duration = Duration.parse("30s"),
    val default: Options = Options(),
    val worlds: Map<String, Options> = emptyMap(),
    val enabledWorlds: List<String> = listOf("world")
)

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
