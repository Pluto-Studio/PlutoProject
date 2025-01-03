package ink.pmc.essentials.back

import ink.pmc.essentials.api.back.BackManager
import ink.pmc.essentials.api.back.BackTeleportEvent
import ink.pmc.essentials.api.teleport.TeleportManager
import ink.pmc.essentials.repositories.BackRepository
import ink.pmc.framework.concurrent.async
import ink.pmc.framework.concurrent.submitAsync
import org.bukkit.Location
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BackManagerImpl : BackManager, KoinComponent {
    private val teleport by inject<TeleportManager>()
    private val repo by inject<BackRepository>()

    override suspend fun has(player: Player): Boolean {
        return repo.has(player)
    }

    override suspend fun get(player: Player): Location? {
        return repo.find(player)
    }

    override fun back(player: Player) {
        submitAsync {
            backSuspend(player)
        }
    }

    override suspend fun backSuspend(player: Player) {
        async {
            val loc = requireNotNull(get(player)) { "Player ${player.name} doesn't have a back location" }
            // 必须异步触发
            val event = BackTeleportEvent(player, player.location, loc).apply { callEvent() }
            if (event.isCancelled) return@async
            set(player, player.location)
            val opt = teleport.getWorldTeleportOptions(loc.world).copy(disableSafeCheck = true)
            teleport.teleportSuspend(player, loc, opt)
        }
    }

    override suspend fun set(player: Player, location: Location) {
        val loc = if (teleport.isSafe(location)) {
            location
        } else {
            teleport.searchSafeLocationSuspend(location) ?: return
        }
        repo.save(player, loc)
    }

    override suspend fun remove(player: Player) {
        repo.delete(player)
    }
}