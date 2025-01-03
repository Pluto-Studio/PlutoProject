package ink.pmc.essentials.listeners

import ink.pmc.essentials.config.EssentialsConfig
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

@Suppress("UNUSED", "UnusedReceiverParameter")
object DisableJoinQuitMessageListener : Listener, KoinComponent {
    private val config by lazy { get<EssentialsConfig>().disableJoinQuitMessage }

    @EventHandler
    fun PlayerJoinEvent.e() {
        if (config.enabled) joinMessage(null)
    }

    @EventHandler
    fun PlayerQuitEvent.e() {
        if (config.enabled) quitMessage(null)
    }
}