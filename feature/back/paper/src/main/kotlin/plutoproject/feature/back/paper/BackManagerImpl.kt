package plutoproject.feature.back.paper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bukkit.Location
import org.bukkit.entity.Player
import plutoproject.kernel.api.koinInject
import plutoproject.feature.back.api.paper.BackManager
import plutoproject.feature.back.api.paper.BackTeleportEvent
import plutoproject.feature.teleport.api.paper.TeleportManager
import plutoproject.feature.back.paper.moduleScope

class BackManagerImpl : BackManager {
    private val config by koinInject<BackConfig>()
    private val repo by koinInject<BackRepository>()

    override suspend fun has(player: Player): Boolean {
        return repo.has(player)
    }

    override suspend fun get(player: Player): Location? {
        return repo.find(player)
    }

    override fun back(player: Player) {
        moduleScope.launch {
            backSuspend(player)
        }
    }

    override suspend fun backSuspend(player: Player) {
        withContext(Dispatchers.Default) {
            val loc = requireNotNull(get(player)) { "Player ${player.name} doesn't have a back location" }
            // 必须异步触发
            val event = BackTeleportEvent(player, player.location, loc).apply { callEvent() }
            if (event.isCancelled) return@withContext
            set(player, player.location)
            val opt = teleportManager.getWorldTeleportOptions(loc.world).copy(disableSafeCheck = true)
            teleportManager.teleportSuspend(player, loc, opt)
        }
    }

    override suspend fun set(player: Player, location: Location) {
        require(location.world.name !in config.blacklistedWorlds) { "World ${location.world.name} is not available" }
        val loc = if (teleportManager.isSafe(location)) {
            location
        } else {
            teleportManager.searchSafeLocationSuspend(location) ?: return
        }
        repo.save(player, loc)
    }

    override suspend fun remove(player: Player) {
        repo.delete(player)
    }
}
