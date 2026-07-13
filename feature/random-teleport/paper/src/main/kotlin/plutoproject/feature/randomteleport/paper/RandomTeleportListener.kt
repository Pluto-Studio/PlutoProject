package plutoproject.feature.randomteleport.paper

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import kotlinx.coroutines.launch
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import plutoproject.kernel.api.koinInject
import plutoproject.feature.randomteleport.api.paper.RandomTeleportManager
import plutoproject.feature.randomteleport.paper.moduleScope

@Suppress("UNUSED", "UnusedReceiverParameter")
object RandomTeleportListener : Listener {
    @EventHandler
    fun ServerTickEndEvent.e() {
        moduleScope.launch {
            randomTeleportManager.tick()
        }
    }
}
