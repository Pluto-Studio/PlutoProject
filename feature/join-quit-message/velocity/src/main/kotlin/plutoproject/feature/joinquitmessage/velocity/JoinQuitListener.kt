package plutoproject.feature.joinquitmessage.velocity

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.player.ServerPostConnectEvent
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder

internal class JoinQuitListener(
    private val proxyServer: ProxyServer,
    private val config: JoinQuitMessageConfig,
) {
    @Subscribe
    fun ServerPostConnectEvent.onServerPostConnect() {
        if (previousServer != null || !shouldDisplayMessage(player)) return
        broadcast(config.join, player)
    }

    @Subscribe
    fun DisconnectEvent.onDisconnect() {
        if (player.currentServer.isEmpty || !shouldDisplayMessage(player)) return
        broadcast(config.quit, player)
    }

    private fun broadcast(message: String, player: Player) {
        val component = MiniMessage.miniMessage().deserialize(
            message,
            Placeholder.unparsed("player", player.username),
        )
        proxyServer.allPlayers.forEach { it.sendMessage(component) }
    }

    private fun shouldDisplayMessage(player: Player): Boolean =
        player.hasPermission(PERMISSION_SEND_JOIN_QUIT_BROADCAST)
}
