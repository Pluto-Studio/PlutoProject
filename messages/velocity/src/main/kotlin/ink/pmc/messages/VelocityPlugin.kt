package ink.pmc.messages

import com.electronwill.nightconfig.core.Config
import com.electronwill.nightconfig.core.file.FileConfig
import com.github.shynixn.mccoroutine.velocity.SuspendingPluginContainer
import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.player.ServerConnectedEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.RegisteredServer
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.miniMessage
import ink.pmc.advkt.component.replace
import ink.pmc.utils.platform.proxy
import ink.pmc.utils.platform.saveConfig
import net.kyori.adventure.text.Component
import java.io.File
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.jvm.optionals.getOrNull

var disabled = false
lateinit var pluginContainer: PluginContainer
lateinit var serverLogger: Logger
lateinit var dataDir: File
lateinit var config: FileConfig

@Suppress("UNUSED", "UNUSED_PARAMETER")
class VelocityPlugin @Inject constructor(suspendingPluginContainer: SuspendingPluginContainer) {

    private val groups = mutableSetOf<Group>()

    init {
        suspendingPluginContainer.initialize(this)
    }

    @Inject
    fun velocityMessages(server: ProxyServer, logger: Logger, @DataDirectory dataDirectoryPath: Path) {
        serverLogger = logger
        dataDir = dataDirectoryPath.toFile()

        if (!dataDir.exists()) {
            dataDir.mkdirs()
        }

        val configFile = File(dataDir, "proxy_config.conf")

        if (!configFile.exists()) {
            saveConfig(VelocityPlugin::class.java, "proxy_config.conf", configFile)
        }

        config = configFile.loadConfig()
        groups.addAll(loadGroups())
    }

    @Subscribe
    fun proxyInitializeEvent(event: ProxyInitializeEvent) {
        pluginContainer = proxy.pluginManager.getPlugin("transfer").get()
        disabled = false
    }

    @Subscribe
    fun proxyShutdownEvent(event: ProxyShutdownEvent) {
        disabled = true
    }

    private fun Group.broadcastJoinMessage(player: Player) {
        players.forEach {
            joinMessage?.let { m -> it.sendMessage(m.replace("<player>", Component.text(player.username))) }
        }
    }

    private fun Group.broadcastQuitMessage(player: Player) {
        players.forEach {
            quitMessage?.let { m -> it.sendMessage(m.replace("<player>", Component.text(player.username))) }
        }
    }

    @Subscribe
    fun ServerConnectedEvent.e() {
        val group = player.group
        val previousGroup = previousServer.getOrNull()?.group

        if (previousGroup != group) {
            group?.broadcastJoinMessage(player)
            previousGroup?.broadcastQuitMessage(player)
        }
    }

    @Subscribe
    fun DisconnectEvent.e() {
        val previousGroup = player.group
        previousGroup?.broadcastQuitMessage(player)
    }

    private val Group.players: MutableList<Player>
        get() {
            return mutableListOf<Player>().apply {
                servers.forEach {
                    addAll(it.playersConnected)
                }
            }
        }

    private val Player.group: Group?
        get() {
            return groups.firstOrNull {
                it.players.contains(this)
            }
        }

    private val RegisteredServer.group: Group?
        get() {
            return groups.firstOrNull {
                it.servers.contains(this)
            }
        }

    private fun File.loadConfig(): FileConfig {
        return FileConfig.builder(this)
            .async()
            .autoreload()
            .onAutoReload {
                groups.clear()
                groups.addAll(loadGroups())
                serverLogger.info("Reloaded group settings")
            }
            .build()
    }

    private fun loadGroups(): List<Group> {
        return mutableListOf<Group>().apply {
            val groupList = config.get<List<Config>>("groups")
            groupList.forEach {
                val name = it.get<String>("name")
                val servers = it.get<List<String>>("servers")
                    .filter { s -> proxy.getServer(s).getOrNull() != null }
                    .map { s -> proxy.getServer(s).get() }
                val joinMessage = it.get<String>("join.message")?.let { m -> component { miniMessage(m) } }
                val quitMessage = it.get<String>("quit.message")?.let { m -> component { miniMessage(m) } }
                add(Group(name, servers, joinMessage, quitMessage))
            }
        }
    }

}