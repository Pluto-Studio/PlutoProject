package plutoproject.feature.versionchecker.velocity

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PreLoginEvent
import com.velocitypowered.api.event.player.ServerConnectedEvent
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import com.velocitypowered.api.proxy.server.ServerPing
import net.kyori.adventure.text.Component
import plutoproject.capability.databasepersist.api.DatabasePersist
import plutoproject.capability.databasepersist.api.adapters.IntTypeAdapter

internal class PingListener(
    private val config: VersionCheckerConfig,
    private val databasePersist: DatabasePersist,
) {
    @Subscribe(order = PostOrder.FIRST)
    fun ProxyPingEvent.onProxyPing() {
        val clientVersion = connection.protocolVersion
        val reportVersion = if (clientVersion.protocol in config.compatibleProtocolRange) {
            clientVersion.protocol
        } else {
            config.minimumSupportedProtocol
        }
        val version = ServerPing.Version(reportVersion, "${config.serverBrand} ${getVersionRange(config)}")
        ping = ping.asBuilder().version(version).build()
    }

    @Subscribe(order = PostOrder.FIRST)
    fun PreLoginEvent.onPreLogin() {
        if (connection.protocolVersion.protocol !in config.compatibleProtocolRange) {
            result = PreLoginEvent.PreLoginComponentResult.denied(getVersionNotCompatible(config))
        }
    }

    @Subscribe(order = PostOrder.FIRST)
    suspend fun ServerConnectedEvent.onServerConnected() {
        if (!config.enableVersionWarning || player.protocolVersion.protocol in config.supportedProtocolRange) return

        val container = databasePersist.getContainer(player.uniqueId)
        val ignoredProtocol = container.get(IGNORED_PROTOCOL_PERSIST_KEY, IntTypeAdapter)
        if (ignoredProtocol == config.minimumSupportedProtocol) return

        player.sendMessage(Component.empty())
        player.sendMessage(getUnsupportedVersionWarning(config))
        player.sendMessage(Component.empty())
    }
}
