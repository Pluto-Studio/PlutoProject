package plutoproject.feature.paper.sit.block.listeners

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.paper.sit.block.InternalBlockSit

@Suppress("UnusedReceiverParameter")
object BlockSitServerListener : Listener, KoinComponent {
    private val internalBlockSit by inject<InternalBlockSit>()

    @EventHandler
    fun ServerTickEndEvent.e() {
        internalBlockSit.sitters.forEach {
            if (!it.isInsideVehicle && !it.isDead) {
                internalBlockSit.sitContexts[it]!!.seatEntity.addPassenger(it)
            }
        }
    }
}
