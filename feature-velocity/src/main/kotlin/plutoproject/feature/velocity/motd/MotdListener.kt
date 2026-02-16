package plutoproject.feature.velocity.motd

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Suppress("UNUSED")
object MotdListener : KoinComponent {
    private val service by inject<MotdService>()

    @Subscribe(order = PostOrder.LAST)
    fun ProxyPingEvent.e() {
        ping = ping.asBuilder()
            .description(service.renderMotd())
            .build()
    }
}
