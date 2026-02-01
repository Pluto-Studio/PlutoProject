package plutoproject.feature.paper.back

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.paper.api.back.BackManager
import plutoproject.feature.paper.api.home.HomeTeleportEvent
import plutoproject.feature.paper.api.randomTeleport.events.RandomTeleportEvent
import plutoproject.feature.paper.api.teleport.RequestState
import plutoproject.feature.paper.api.teleport.events.RequestStateChangeEvent
import plutoproject.feature.paper.api.warp.WarpTeleportEvent

@Suppress("UNUSED", "UnusedReceiverParameter")
object PlayerListener : Listener, KoinComponent {
    private val config by inject<BackConfig>()

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    suspend fun HomeTeleportEvent.e() {
        if (from.world.name in config.blacklistedWorlds) return
        if (!player.hasPermission("plutoproject.back.record_back")) return
        BackManager.set(player, from)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    suspend fun WarpTeleportEvent.e() {
        if (from.world.name in config.blacklistedWorlds) return
        if (!player.hasPermission("plutoproject.back.record_back")) return
        BackManager.set(player, from)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    suspend fun RandomTeleportEvent.e() {
        if (from.world.name in config.blacklistedWorlds) return
        if (!player.hasPermission("plutoproject.back.record_back")) return
        BackManager.set(player, from)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    suspend fun PlayerDeathEvent.e() {
        val location = player.location
        if (location.world.name in config.blacklistedWorlds) return
        if (!player.hasPermission("plutoproject.back.record_back")) return
        BackManager.set(player, location)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    suspend fun RequestStateChangeEvent.e() {
        if (after != RequestState.ACCEPTED) return
        val player = request.source
        val location = player.location
        if (location.world.name in config.blacklistedWorlds) return
        if (!player.hasPermission("plutoproject.back.record_back")) return
        BackManager.set(player, location)
    }

    private val validTeleportCauses = arrayOf(
        TeleportCause.COMMAND,
        TeleportCause.END_GATEWAY,
        TeleportCause.END_PORTAL,
        TeleportCause.NETHER_PORTAL,
        TeleportCause.SPECTATE,
        TeleportCause.UNKNOWN,
    )

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    suspend fun PlayerTeleportEvent.e() {
        if (!validTeleportCauses.contains(cause)) return
        val location = player.location
        if (location.world.name in config.blacklistedWorlds) return
        if (!player.hasPermission("plutoproject.back.record_back")) return
        BackManager.set(player, location)
    }
}
