package ink.pmc.essentials.listeners

import ink.pmc.essentials.api.teleport.TeleportManager
import ink.pmc.essentials.config.EssentialsConfig
import org.bukkit.event.Listener
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject

@Suppress("UNUSED", "UnusedReceiverParameter")
object ActionListener : Listener, KoinComponent {
    private val config by lazy { get<EssentialsConfig>().action }
    private val teleportManager by inject<TeleportManager>()
}