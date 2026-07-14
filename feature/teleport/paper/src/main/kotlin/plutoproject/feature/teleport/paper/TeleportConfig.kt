package plutoproject.feature.teleport.paper

import org.bukkit.Material
import kotlin.time.Duration

data class TeleportConfig(
    val maxRequestsStored: Int = 50,
    val request: Request = Request(),
    val queueProcessPerTick: Int = 1,
    val chunkPrepareMethod: ChunkPrepareMethod = ChunkPrepareMethod.ASYNC,
    val default: Options = Options(),
    val worlds: Map<String, Options> = emptyMap(),
    val blacklistedWorlds: List<String> = emptyList(),
)

data class Options(
    val avoidVoid: Boolean = true,
    val safeLocationSearchRadius: Int = 20,
    val chunkPrepareRadius: Int = 0,
    val blacklistedBlocks: List<Material> = listOf(Material.WATER, Material.LAVA)
)

data class Request(
    val expireAfter: Duration = Duration.parse("1m"),
    val removeAfter: Duration = Duration.parse("10m")
)
