package plutoproject.feature.velocity.versionchecker

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PreLoginEvent
import com.velocitypowered.api.event.player.ServerConnectedEvent
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import com.velocitypowered.api.proxy.server.ServerPing
import net.kyori.adventure.text.Component
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.framework.common.api.databasepersist.DatabasePersist
import plutoproject.framework.common.api.databasepersist.adapters.IntTypeAdapter

@Suppress("UNUSED")
object PingListener : KoinComponent {
    private val config by inject<VersionCheckerConfig>()

    @Subscribe(order = PostOrder.FIRST)
    fun ProxyPingEvent.e() {
        val clientVersion = connection.protocolVersion
        val reportVersion = if (clientVersion.protocol in config.compatibleProtocolRange) {
            clientVersion.protocol
        } else {
            config.minimumSupportedProtocol
        }
        val version = ServerPing.Version(
            reportVersion,
            "${config.serverBrand} $VERSION_RANGE"
        )
        ping = ping.asBuilder().version(version).build()
    }

    @Subscribe(order = PostOrder.FIRST)
    fun PreLoginEvent.e() {
        val protocol = connection.protocolVersion.protocol
        if (protocol !in config.compatibleProtocolRange) {
            result = PreLoginEvent.PreLoginComponentResult.denied(VERSION_NOT_COMPATIBLE)
        }
    }

    @Subscribe(order = PostOrder.FIRST)
    suspend fun ServerConnectedEvent.e() {
        if (player.protocolVersion.protocol in config.supportedProtocolRange) {
            return
        }

        val container = DatabasePersist.getContainer(player.uniqueId)
        val ignoredProtocol = container.get(IGNORED_PROTOCOL_PERSIST_KEY, IntTypeAdapter)

        if (ignoredProtocol != null && ignoredProtocol == config.minimumSupportedProtocol) {
            return
        }

        player.sendMessage(Component.empty())
        player.sendMessage(UNSUPPORTED_VERSION_WARNING)
        player.sendMessage(Component.empty())
    }
}
