package plutoproject.feature.warp.paper

import plutoproject.feature.warp.paper.warpManager

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import plutoproject.kernel.api.koinInject
import plutoproject.feature.warp.api.paper.WarpManager

@Suppress("UNUSED")
object PlayerListener : Listener {
    @EventHandler
    suspend fun PlayerJoinEvent.e() {
        if (warpManager.getPreferredSpawn(player) != null) return
        val default = warpManager.getDefaultSpawn() ?: return
        warpManager.setPreferredSpawn(player, default)
    }
}
