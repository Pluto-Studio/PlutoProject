package plutoproject.feature.dynamicscheduler.paper.listeners

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent
import plutoproject.feature.dynamicscheduler.api.paper.DynamicScheduler
import plutoproject.feature.dynamicscheduler.api.paper.DynamicViewDistanceState
import plutoproject.feature.dynamicscheduler.paper.config.DynamicSchedulerConfig
import plutoproject.foundation.common.network.toHostPortString

@Suppress("UNUSED")
object DynamicViewDistanceListener : Listener {
    private val config by lazy { plutoproject.kernel.api.koinGet<DynamicSchedulerConfig>().viewDistance }

    private fun Player.refreshViewDistance() {
        val boost = config.boost
        val standard = config.standard
        when {
            plutoproject.kernel.api.koinGet<DynamicScheduler>().getViewDistanceLocally(this) == DynamicViewDistanceState.ENABLED
                    && viewDistance < boost -> {
                viewDistance = boost
            }

            plutoproject.kernel.api.koinGet<DynamicScheduler>().getViewDistanceLocally(this) != DynamicViewDistanceState.ENABLED
                    && viewDistance > standard -> {
                viewDistance = standard
            }
        }
    }

    private val Player.formattedVhost: String?
        get() {
            return virtualHost?.toHostPortString()
        }

    @EventHandler
    fun PlayerJoinEvent.e() {
        val vhosts = config.virtualHosts
        val vhost = player.formattedVhost
        if (vhost != null && !vhosts.contains(vhost)) {
            plutoproject.kernel.api.koinGet<DynamicScheduler>().setViewDistanceLocally(player, DynamicViewDistanceState.DISABLED_DUE_VHOST)
        }
        player.refreshViewDistance()
    }

    @EventHandler
    fun PlayerTeleportEvent.e() {
        player.refreshViewDistance()
    }

    @EventHandler
    fun PlayerPostRespawnEvent.e() {
        player.refreshViewDistance()
    }

    @EventHandler
    fun PlayerChangedWorldEvent.e() {
        player.refreshViewDistance()
    }

    @EventHandler
    fun PlayerQuitEvent.e() {
        plutoproject.kernel.api.koinGet<DynamicScheduler>().removeLocalViewDistanceState(player)
    }
}
