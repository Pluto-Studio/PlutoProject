package plutoproject.feature.paper.back

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bukkit.Location
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.paper.api.back.BackManager
import plutoproject.feature.paper.api.back.BackTeleportEvent
import plutoproject.feature.paper.api.teleport.TeleportManager
import plutoproject.framework.common.util.coroutine.PluginScope

class BackManagerImpl : BackManager, KoinComponent {
    private val config by inject<BackConfig>()
    private val repo by inject<BackRepository>()

    override suspend fun has(player: Player): Boolean {
        return repo.has(player)
    }

    override suspend fun get(player: Player): Location? {
        return repo.find(player)
    }

    override fun back(player: Player) {
        PluginScope.launch {
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
            val opt = TeleportManager.getWorldTeleportOptions(loc.world).copy(disableSafeCheck = true)
            TeleportManager.teleportSuspend(player, loc, opt)
        }
    }

    override suspend fun set(player: Player, location: Location) {
        require(location.world.name !in config.blacklistedWorlds) { "World ${location.world.name} is not available" }
        val loc = if (TeleportManager.isSafe(location)) {
            location
        } else {
            TeleportManager.searchSafeLocationSuspend(location) ?: return
        }
        repo.save(player, loc)
    }

    override suspend fun remove(player: Player) {
        repo.delete(player)
    }
}
