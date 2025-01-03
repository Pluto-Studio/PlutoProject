package ink.pmc.protocolchecker

import com.electronwill.nightconfig.core.file.FileConfig
import com.github.shynixn.mccoroutine.velocity.SuspendingPluginContainer
import com.google.inject.Inject
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PreLoginEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.network.ProtocolVersion
import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.ServerPing
import com.velocitypowered.api.proxy.server.ServerPing.SamplePlayer
import ink.pmc.framework.platform.proxy
import ink.pmc.framework.platform.saveDefaultConfig
import java.io.File
import java.nio.file.Path
import java.util.logging.Logger

var disabled = false
lateinit var pluginContainer: PluginContainer
lateinit var serverLogger: Logger
lateinit var dataDir: File
lateinit var config: FileConfig

lateinit var protocolRange: IntRange
var serverBrand: String? = null
var forwardPlayerList = false
var samplePlayersCount = 0
var maxPlayerCount = -1

val Int.gameVersion: List<String>
    get() {
        return ProtocolVersion.getProtocolVersion(this).versionsSupportedBy
    }

@Suppress("UNUSED", "UNUSED_PARAMETER")
class VelocityPlugin @Inject constructor(suspendingPluginContainer: SuspendingPluginContainer) {

    init {
        suspendingPluginContainer.initialize(this)
    }

    @Inject
    fun velocityProtocolChecker(server: ProxyServer, logger: Logger, @DataDirectory dataDirectoryPath: Path) {
        serverLogger = logger
        dataDir = dataDirectoryPath.toFile()
        if (!dataDir.exists()) {
            dataDir.mkdirs()
        }
        val configFile = saveDefaultConfig(this::class.java, dataDir)
        config = configFile.loadConfig()
        loadConfigValues()
    }

    @Subscribe
    fun proxyInitializeEvent(event: ProxyInitializeEvent) {
        pluginContainer = proxy.pluginManager.getPlugin("plutoproject_protocol-checker").get()
        disabled = false
    }

    @Subscribe
    fun proxyShutdownEvent(event: ProxyShutdownEvent) {
        disabled = true
    }

    @Subscribe(order = PostOrder.FIRST)
    fun ProxyPingEvent.e() {
        val clientVersion = connection.protocolVersion
        val reportVersion = if (clientVersion.protocol in protocolRange) clientVersion.protocol else protocolRange.first
        val version = ServerPing.Version(
            reportVersion,
            if (serverBrand != null) "$serverBrand $VERSION_RANGE" else VERSION_RANGE
        )
        ping = ping.asBuilder()
            .version(version)
            .apply {
                if (maxPlayerCount != -1) {
                    maximumPlayers(maxPlayerCount)
                }
                if (forwardPlayerList) {
                    val players = proxy.allPlayers
                        .map { SamplePlayer(it.username, it.uniqueId) }
                        .take(samplePlayersCount)
                    samplePlayers(*players.toTypedArray())
                }
            }
            .build()
    }

    @Subscribe(order = PostOrder.FIRST)
    fun PreLoginEvent.e() {
        val protocol = connection.protocolVersion.protocol
        if (!protocolRange.contains(protocol)) {
            result = PreLoginEvent.PreLoginComponentResult.denied(VERSION_NOT_SUPPORTED)
            return
        }
        if (proxy.playerCount + 1 > maxPlayerCount && maxPlayerCount != -1) {
            result = PreLoginEvent.PreLoginComponentResult.denied(SERVER_IS_FULL)
            return
        }
    }

    private fun loadConfigValues() {
        val list = config.get<List<Int>>("protocol-range")
        protocolRange = list[0]..list[1]
        serverBrand = config.get("server-brand")
        forwardPlayerList = config.get("forward-player-list") ?: false
        samplePlayersCount = config.get("sample-players-count") ?: 0
        maxPlayerCount = config.get("max-player-count") ?: -1
    }

    private fun File.loadConfig(): FileConfig {
        return FileConfig.builder(this)
            .autoreload()
            .onAutoReload {
                loadConfigValues()
                serverLogger.info("Reloaded protocol settings")
            }
            .async()
            .build()
            .apply { load() }
    }

}