package ink.pmc.essentials.listeners

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import ink.pmc.essentials.api.teleport.random.RandomTeleportManager
import ink.pmc.essentials.teleport.random.PerfTest
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Suppress("UNUSED", "UnusedReceiverParameter")
object RandomTeleportListener : Listener, KoinComponent {
    private val manager by inject<RandomTeleportManager>()

    @EventHandler
    suspend fun ServerTickEndEvent.e() {
        manager.tick()
    }

    @EventHandler
    fun PlayerQuitEvent.e() {
        if (PerfTest.isInTest(player)) {
            PerfTest.endTest(player)
        }
    }
}