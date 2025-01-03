package ink.pmc.serverselector

import ink.pmc.framework.currentUnixTimestamp
import ink.pmc.framework.inject.koin
import org.bukkit.*
import java.io.File

private val config = koin.get<ServerSelectorConfig>().lobby
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
    val start = currentUnixTimestamp
    plugin.logger.info("Start loading lobby world...")
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
    plugin.logger.info("Lobby world loaded in ${currentUnixTimestamp - start}ms")
}