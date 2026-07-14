package plutoproject.feature.status.paper

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import plutoproject.kernel.api.koinInject

@Suppress("UNUSED")
object StatusListener : Listener {
    private val config by koinInject<StatusConfig>()

    @EventHandler(ignoreCancelled = true)
    fun PlayerCommandPreprocessEvent.e() {
        if (message == "/tps" && config.overrideTpsCommand) {
            player.performCommand("plutoproject:tps")
            isCancelled = true
            return
        }
        if (message == "/mspt" && config.overrideMsptCommand) {
            player.performCommand("plutoproject:mspt")
            isCancelled = true
            return
        }
    }
}
