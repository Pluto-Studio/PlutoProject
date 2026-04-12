package plutoproject.feature.paper.serverSelector

import org.bukkit.*
import plutoproject.framework.common.util.inject.globalKoin
import java.io.File
import kotlin.system.measureTimeMillis

private val config = globalKoin.get<ServerSelectorConfig>().lobby
private val worldName = config.world

val lobbyWorld: World
    get() = Bukkit.getWorld(worldName) ?: error("Unable to get lobby world")

val lobbyWorldSpawn
    get() = Location(
        lobbyWorld,
        config.spawnpoint.x,
        config.spawnpoint.y,
        config.spawnpoint.z,
        config.spawnpoint.yaw,
        config.spawnpoint.pitch
    )

fun loadLobbyWorld() {
    if (Bukkit.getWorld(worldName) != null) return
    if (!File(Bukkit.getWorldContainer(), "$worldName${File.separator}").exists()) {
        error("Lobby world folder not found")
    }
    val time = measureTimeMillis {
        val world = Bukkit.createWorld(WorldCreator(worldName)) ?: error("Unable to load lobby world")
        world.apply {
            difficulty = Difficulty.PEACEFUL
            spawnLocation = lobbyWorldSpawn
            setGameRule(GameRule.SPAWN_RADIUS, 0)
            setGameRule(GameRule.RANDOM_TICK_SPEED, 0)
            setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false)
            setGameRule(GameRule.DO_WEATHER_CYCLE, false)
            setGameRule(GameRule.DO_MOB_SPAWNING, false)
            setGameRule(GameRule.DO_FIRE_TICK, false)
            setGameRule(GameRule.MOB_GRIEFING, false)
            setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true)
            setGameRule(GameRule.KEEP_INVENTORY, true)
            setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false)
        }
    }
    featureLogger.info("Lobby world loaded in ${time}ms")
}
