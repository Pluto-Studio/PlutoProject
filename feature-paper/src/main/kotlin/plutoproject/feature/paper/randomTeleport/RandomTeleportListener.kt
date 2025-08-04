package plutoproject.feature.paper.randomTeleport

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import kotlinx.coroutines.launch
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.koin.core.component.KoinComponent
import plutoproject.feature.paper.api.randomTeleport.RandomTeleportManager
import plutoproject.framework.common.util.coroutine.PluginScope

@Suppress("UNUSED", "UnusedReceiverParameter")
object RandomTeleportListener : Listener, KoinComponent {
    @EventHandler
    fun ServerTickEndEvent.e() {
        PluginScope.launch {
            RandomTeleportManager.tick()
        }
    }
}
