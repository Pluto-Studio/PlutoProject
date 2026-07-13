package plutoproject.feature.sit.paper.block.listeners

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import plutoproject.feature.sit.paper.block.InternalBlockSit

@Suppress("UnusedReceiverParameter")
object BlockSitServerListener : Listener {
    private val internalBlockSit by plutoproject.kernel.api.koinInject<InternalBlockSit>()

    @EventHandler
    fun ServerTickEndEvent.e() {
        internalBlockSit.sitters.forEach {
            if (!it.isInsideVehicle && !it.isDead) {
                internalBlockSit.sitContexts[it]!!.seatEntity.addPassenger(it)
            }
        }
    }
}
