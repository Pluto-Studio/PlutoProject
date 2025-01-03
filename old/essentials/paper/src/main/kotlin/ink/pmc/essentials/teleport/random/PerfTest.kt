package ink.pmc.essentials.teleport.random

import ink.pmc.essentials.api.teleport.random.RandomTeleportManager
import ink.pmc.essentials.essentialsScope
import ink.pmc.framework.time.ticks
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.ConcurrentHashMap

object PerfTest : KoinComponent {
    private val manager by inject<RandomTeleportManager>()
    private val inTest = ConcurrentHashMap.newKeySet<Player>()

    fun isInTest(player: Player): Boolean {
        return inTest.contains(player)
    }

    fun startTest(player: Player) {
        if (isInTest(player)) {
            return
        }
        inTest.add(player)
        essentialsScope.launch {
            while (isInTest(player)) {
                manager.launchSuspend(player, player.world)
                delay(5.ticks)
            }
        }
    }

    fun endTest(player: Player) {
        if (!isInTest(player)) {
            return
        }
        inTest.remove(player)
    }
}