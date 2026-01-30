package plutoproject.feature.velocity.joinQuitMessage

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.player.ServerPostConnectEvent
import com.velocitypowered.api.proxy.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.common.api.whitelist_v2.Whitelist
import plutoproject.framework.common.api.feature.FeatureManager
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.velocity.util.broadcast
import plutoproject.framework.velocity.util.server

@Suppress("UNUSED")
object JoinQuitListener : KoinComponent {
    private val config by inject<JoinQuitMessageConfig>()

    @Subscribe
    fun ServerPostConnectEvent.e() {
        if (previousServer != null) return
        if (!shouldDisplayMessage(player)) return
        server.broadcast(config.join.replace("<player>", player.username))
    }

    @Subscribe
    fun DisconnectEvent.e() {
        if (player.currentServer.isEmpty) return
        if (!shouldDisplayMessage(player)) return
        server.broadcast(config.quit.replace("<player>", player.username))
    }

    private fun shouldDisplayMessage(player: Player): Boolean {
        if (!FeatureManager.isEnabled("whitelist_v2")) {
            return true
        }
        return !Whitelist.isKnownVisitor(player.uniqueId)
    }
}
