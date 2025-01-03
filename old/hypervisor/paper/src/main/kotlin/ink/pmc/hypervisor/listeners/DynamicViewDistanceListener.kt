package ink.pmc.hypervisor.listeners

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import ink.pmc.hypervisor.DynamicScheduling
import ink.pmc.hypervisor.DynamicViewDistanceState
import ink.pmc.hypervisor.config.HypervisorConfig
import ink.pmc.framework.network.formatted
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

@Suppress("UNUSED")
object DynamicViewDistanceListener : Listener, KoinComponent {
    private val config by lazy { get<HypervisorConfig>().dynamicScheduling }

    private fun Player.refreshViewDistance() {
        val boost = config.viewDistance.boost
        val standard = config.viewDistance.standard
        when {
            DynamicScheduling.getViewDistanceLocally(this) == DynamicViewDistanceState.ENABLED
                    && viewDistance < boost -> {
                viewDistance = boost
            }

            DynamicScheduling.getViewDistanceLocally(this) != DynamicViewDistanceState.ENABLED
                    && viewDistance > standard -> {
                viewDistance = standard
            }
        }
    }

    private val Player.formattedVhost: String?
        get() {
            return virtualHost?.formatted
        }

    @EventHandler
    fun PlayerJoinEvent.e() {
        val vhosts = config.viewDistance.virtualHosts
        val vhost = player.formattedVhost
        if (vhost != null && !vhosts.contains(vhost)) {
            DynamicScheduling.setViewDistanceLocally(player, DynamicViewDistanceState.DISABLED_DUE_VHOST)
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
        DynamicScheduling.removeLocalViewDistanceState(player)
    }
}