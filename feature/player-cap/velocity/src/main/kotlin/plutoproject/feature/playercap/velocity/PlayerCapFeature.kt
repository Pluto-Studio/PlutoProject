package plutoproject.feature.playercap.velocity

import com.github.shynixn.mccoroutine.velocity.registerSuspend
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.hocon.HoconParser
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PreLoginEvent
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.ServerPing.SamplePlayer
import plutoproject.kernel.api.Feature
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule
import plutoproject.kernel.api.velocity.VelocityModuleContext

@Feature(id = "player_cap", platform = Platform.VELOCITY)
@Suppress("UNUSED")
@OptIn(ExperimentalHoplite::class)
class PlayerCapFeature : RuntimeModule {
    private lateinit var config: PlayerCapConfig
    private lateinit var proxyServer: ProxyServer

    override suspend fun onLoad(context: ModuleContext) {
        context.dataFolder.toFile().mkdirs()
        val configFile = context.saveResource("config.conf")
        config = ConfigLoaderBuilder.empty()
            .withClassLoader(PlayerCapFeature::class.java.classLoader)
            .withExplicitSealedTypes()
            .addDefaults()
            .addParser("conf", HoconParser())
            .addPropertySource(PropertySource.file(configFile.toFile()))
            .build()
            .loadConfigOrThrow()
    }

    override suspend fun onEnable(context: ModuleContext) {
        context as VelocityModuleContext
        proxyServer = context.proxyServer
        context.proxyServer.eventManager.registerSuspend(context.pluginContainer, this)
    }

    @Subscribe
    fun ProxyPingEvent.onProxyPing() {
        ping = ping.asBuilder().apply {
            if (config.maxPlayerCount != -1) {
                maximumPlayers(config.maxPlayerCount)
            }
            if (config.forwardPlayerList) {
                val players = proxyServer.allPlayers
                    .map { SamplePlayer(it.username, it.uniqueId) }
                    .take(config.samplePlayersCount)
                samplePlayers(*players.toTypedArray())
            }
        }.build()
    }

    @Subscribe
    fun PreLoginEvent.onPreLogin() {
        if (config.maxPlayerCount != -1 && proxyServer.playerCount + 1 > config.maxPlayerCount) {
            result = PreLoginEvent.PreLoginComponentResult.denied(SERVER_IS_FULL)
        }
    }

    override suspend fun onDisable(context: ModuleContext) {
        context as VelocityModuleContext
        context.proxyServer.eventManager.unregisterListener(context.pluginContainer, this)
    }
}
