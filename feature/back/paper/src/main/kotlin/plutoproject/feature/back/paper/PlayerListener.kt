package plutoproject.feature.back.paper

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause
import plutoproject.kernel.api.koinInject
import plutoproject.feature.back.api.paper.BackManager
import plutoproject.feature.home.api.paper.HomeTeleportEvent
import plutoproject.feature.randomteleport.api.paper.events.RandomTeleportEvent
import plutoproject.feature.teleport.api.paper.RequestState
import plutoproject.feature.teleport.api.paper.events.RequestStateChangeEvent
import plutoproject.feature.warp.api.paper.WarpTeleportEvent

@Suppress("UNUSED", "UnusedReceiverParameter")
object PlayerListener : Listener {
    private val config by koinInject<BackConfig>()

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    suspend fun HomeTeleportEvent.e() {
        if (from.world.name in config.blacklistedWorlds) return
        if (!player.hasPermission("plutoproject.back.record_back")) return
        backManager.set(player, from)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    suspend fun WarpTeleportEvent.e() {
        if (from.world.name in config.blacklistedWorlds) return
        if (!player.hasPermission("plutoproject.back.record_back")) return
        backManager.set(player, from)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    suspend fun RandomTeleportEvent.e() {
        if (from.world.name in config.blacklistedWorlds) return
        if (!player.hasPermission("plutoproject.back.record_back")) return
        backManager.set(player, from)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    suspend fun PlayerDeathEvent.e() {
        val location = player.location
        if (location.world.name in config.blacklistedWorlds) return
        if (!player.hasPermission("plutoproject.back.record_back")) return
        backManager.set(player, location)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    suspend fun RequestStateChangeEvent.e() {
        if (after != RequestState.ACCEPTED) return
        val player = request.source
        val location = player.location
        if (location.world.name in config.blacklistedWorlds) return
        if (!player.hasPermission("plutoproject.back.record_back")) return
        backManager.set(player, location)
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
        backManager.set(player, location)
    }
}
