package plutoproject.framework.paper.options

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import plutoproject.framework.common.api.options.OptionsManager

@Suppress("UNUSED", "UNUSED_PARAMETER", "UnusedReceiverParameter")
object PaperOptionsListener : Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    suspend fun PlayerJoinEvent.e() {
        OptionsManager.Companion.getOptions(player.uniqueId)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    suspend fun PlayerQuitEvent.e() {
        if (!OptionsManager.Companion.isPlayerLoaded(player.uniqueId)) return
        OptionsManager.Companion.save(player.uniqueId)
        OptionsManager.Companion.unloadPlayer(player.uniqueId)
    }
}