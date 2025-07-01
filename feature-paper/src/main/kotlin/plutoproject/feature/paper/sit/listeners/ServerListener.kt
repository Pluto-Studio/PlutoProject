package plutoproject.feature.paper.sit.listeners

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import plutoproject.feature.paper.api.sit.Sit
import plutoproject.feature.paper.sit.STAND_UP_TIP

@Suppress("UnusedReceiverParameter")
object ServerListener : Listener {
    @EventHandler
    fun ServerTickEndEvent.e() {
        Sit.sittingPlayers.forEach {
            if (Sit.getOptions(it)!!.showStandTip) {
                it.sendActionBar(STAND_UP_TIP)
            }
            if (!it.isInsideVehicle) {
                Sit.standUp(it)
            }
        }
    }
}
