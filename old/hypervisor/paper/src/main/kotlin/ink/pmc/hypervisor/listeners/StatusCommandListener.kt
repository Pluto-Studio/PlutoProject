package ink.pmc.hypervisor.listeners

import ink.pmc.hypervisor.config.HypervisorConfig
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Suppress("UNUSED")
object StatusCommandListener : Listener, KoinComponent {
    private val config by inject<HypervisorConfig>()

    @EventHandler(ignoreCancelled = true)
    fun PlayerCommandPreprocessEvent.e() {
        if (message == "/tps" && config.statusCommand.overrideTpsCommand) {
            player.performCommand("plutoproject_hypervisor:tps")
            isCancelled = true
            return
        }
        if (message == "/mspt" && config.statusCommand.overrideMsptCommand) {
            player.performCommand("plutoproject_hypervisor:mspt")
            isCancelled = true
            return
        }
    }
}