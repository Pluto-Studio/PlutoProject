package plutoproject.feature.motd.velocity

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyPingEvent

internal class MotdListener(private val service: MotdService) {
    @Subscribe(order = PostOrder.LAST)
    fun ProxyPingEvent.onProxyPing() {
        ping = ping.asBuilder()
            .description(service.renderMotd())
            .build()
    }
}
