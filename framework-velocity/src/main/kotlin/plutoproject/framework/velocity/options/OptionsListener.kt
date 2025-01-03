package plutoproject.framework.velocity.options

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.PostLoginEvent
import org.koin.core.component.KoinComponent
import plutoproject.framework.common.api.options.OptionsManager

@Suppress("UNUSED", "UNUSED_PARAMETER", "UnusedReceiverParameter")
object OptionsListener : KoinComponent {
    @Subscribe(order = PostOrder.FIRST)
    suspend fun PostLoginEvent.e() {
        OptionsManager.getOptions(player.uniqueId)
    }

    @Subscribe(order = PostOrder.LAST)
    suspend fun DisconnectEvent.e() {
        if (!OptionsManager.isPlayerLoaded(player.uniqueId)) return
        OptionsManager.save(player.uniqueId)
        OptionsManager.unloadPlayer(player.uniqueId)
    }
}
