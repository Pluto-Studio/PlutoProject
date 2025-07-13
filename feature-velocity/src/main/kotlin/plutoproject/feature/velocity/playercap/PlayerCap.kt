package plutoproject.feature.velocity.playercap

import com.github.shynixn.mccoroutine.velocity.registerSuspend
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PreLoginEvent
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import com.velocitypowered.api.proxy.server.ServerPing.SamplePlayer
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.config.loadConfig
import plutoproject.framework.common.util.inject.configureKoin
import plutoproject.framework.velocity.api.feature.VelocityFeature
import plutoproject.framework.velocity.util.plugin
import plutoproject.framework.velocity.util.server

@Feature(
    id = "player_cap",
    platform = Platform.VELOCITY,
)
@Suppress("UNUSED")
class PlayerCap : VelocityFeature(), KoinComponent {
    private val config by inject<PlayerCapConfig>()
    private val featureModule = module {
        single<PlayerCapConfig> { loadConfig(saveConfig()) }
    }

    override fun onEnable() {
        configureKoin {
            modules(featureModule)
        }
        server.eventManager.registerSuspend(plugin, this)
    }

    @Subscribe
    fun ProxyPingEvent.e() {
        ping = ping.asBuilder().apply {
            if (config.maxPlayerCount != -1) {
                maximumPlayers(config.maxPlayerCount)
            }
            if (config.forwardPlayerList) {
                val players = server.allPlayers
                    .map { SamplePlayer(it.username, it.uniqueId) }
                    .take(config.samplePlayersCount)
                samplePlayers(*players.toTypedArray())
            }
        }.build()
    }

    @Subscribe
    fun PreLoginEvent.e() {
        if (server.playerCount + 1 > config.maxPlayerCount && config.maxPlayerCount != -1) {
            result = PreLoginEvent.PreLoginComponentResult.denied(SERVER_IS_FULL)
        }
    }
}
