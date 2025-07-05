package plutoproject.feature.paper.sit.block.listeners

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.paper.api.sit.block.BlockSit
import plutoproject.feature.paper.sit.STAND_UP_TIP
import plutoproject.feature.paper.sit.block.InternalBlockSit

@Suppress("UnusedReceiverParameter")
object ServerListener : Listener, KoinComponent {
    private val internalBlockSit by inject<InternalBlockSit>()

    @EventHandler
    fun ServerTickEndEvent.e() {
        internalBlockSit.sitters.forEach {
            if (BlockSit.getOptions(it)!!.showStandTip) {
                it.sendActionBar(STAND_UP_TIP)
            }
            if (!it.isInsideVehicle && !it.isDead) {
                internalBlockSit.sitContexts[it]!!.seatEntity.addPassenger(it)
            }
        }
    }
}
