package plutoproject.feature.paper.sit

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import plutoproject.feature.paper.api.sit.block.BlockSit
import plutoproject.feature.paper.api.sit.player.PlayerSit
import plutoproject.framework.paper.util.server

@Suppress("UnusedReceiverParameter")
object CommonServerListener : Listener {
    @EventHandler
    fun ServerTickEndEvent.e() {
        server.onlinePlayers.forEach {
            when {
                BlockSit.isSitting(it)
                        && PlayerSit.isCarrier(it)
                        && PlayerSit.getStack(it)!!.options.showCastOffTip -> it.sendActionBar(CAST_OFF_TIP)

                BlockSit.isSitting(it)
                        && BlockSit.getOptions(it)!!.showStandTip -> it.sendActionBar(STAND_UP_TIP)

                PlayerSit.isCarrier(it)
                        && PlayerSit.getStack(it)!!.options.showCastOffTip -> it.sendActionBar(CAST_OFF_TIP)

                PlayerSit.isPassenger(it)
                        && PlayerSit.getOptions(it)!!.showStandTip -> it.sendActionBar(STAND_UP_TIP)
            }
        }
    }
}
