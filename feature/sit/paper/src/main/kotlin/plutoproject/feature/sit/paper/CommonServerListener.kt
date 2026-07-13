package plutoproject.feature.sit.paper

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import plutoproject.feature.sit.api.paper.block.BlockSit
import plutoproject.feature.sit.api.paper.player.PlayerSit
import plutoproject.kernel.api.currentModuleContext
import plutoproject.kernel.api.koinInject
import plutoproject.kernel.api.paper.PaperModuleContext

@Suppress("UnusedReceiverParameter")
object CommonServerListener : Listener {
    private val blockSit by koinInject<BlockSit>()
    private val playerSit by koinInject<PlayerSit>()

    @EventHandler
    fun ServerTickEndEvent.e() {
        (currentModuleContext() as PaperModuleContext).plugin.server.onlinePlayers.forEach {
            when {
                blockSit.isSitting(it)
                        && playerSit.isCarrier(it)
                        && playerSit.getStack(it)!!.options.showCastOffTip -> it.sendActionBar(CAST_OFF_TIP)

                blockSit.isSitting(it)
                        && blockSit.getOptions(it)!!.showStandTip -> it.sendActionBar(STAND_UP_TIP)

                playerSit.isCarrier(it)
                        && playerSit.getStack(it)!!.options.showCastOffTip -> it.sendActionBar(CAST_OFF_TIP)

                playerSit.isPassenger(it)
                        && playerSit.getOptions(it)!!.showStandTip -> it.sendActionBar(STAND_UP_TIP)
            }
        }
    }
}
