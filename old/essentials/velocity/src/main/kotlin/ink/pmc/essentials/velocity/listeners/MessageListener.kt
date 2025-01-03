package ink.pmc.essentials.velocity.listeners

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.player.ServerPostConnectEvent
import ink.pmc.essentials.velocity.EssentialsProxyConfig
import ink.pmc.framework.chat.broadcast
import ink.pmc.framework.platform.proxy
import net.kyori.adventure.text.minimessage.MiniMessage
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

@Suppress("UNUSED")
object MessageListener : KoinComponent {
    private val config by lazy { get<EssentialsProxyConfig>().message }

    @Subscribe
    fun ServerPostConnectEvent.e() {
        if (!config.enabled) return
        if (previousServer != null) return
        proxy.broadcast(MiniMessage.miniMessage().deserialize(config.join.replace("\$player", player.username)))
    }

    @Subscribe
    fun DisconnectEvent.e() {
        if (!config.enabled) return
        if (player.currentServer.isEmpty) return
        proxy.broadcast(MiniMessage.miniMessage().deserialize(config.quit.replace("\$player", player.username)))
    }
}