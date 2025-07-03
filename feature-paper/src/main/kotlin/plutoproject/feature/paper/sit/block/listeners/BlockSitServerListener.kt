package plutoproject.feature.paper.sit.block.listeners

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import plutoproject.feature.paper.api.sit.block.BlockSit
import plutoproject.feature.paper.sit.STAND_UP_TIP

@Suppress("UnusedReceiverParameter")
object BlockSitServerListener : Listener {
    @EventHandler
    fun ServerTickEndEvent.e() {
        BlockSit.sitters.forEach {
            if (BlockSit.getOptions(it)!!.showStandTip) {
                it.sendActionBar(STAND_UP_TIP)
            }
            if (!it.isInsideVehicle) {
                BlockSit.standUp(it)
            }
        }
    }
}
