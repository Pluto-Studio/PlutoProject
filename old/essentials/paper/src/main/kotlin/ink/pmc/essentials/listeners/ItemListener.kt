package ink.pmc.essentials.listeners

import ink.pmc.essentials.config.EssentialsConfig
import org.bukkit.event.Listener
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

@Suppress("UNUSED", "UnusedReceiverParameter")
object ItemListener : Listener, KoinComponent {
    private val config by lazy { get<EssentialsConfig>().item }
}