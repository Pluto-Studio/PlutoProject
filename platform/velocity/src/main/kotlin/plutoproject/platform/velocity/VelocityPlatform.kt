package plutoproject.platform.velocity

import com.github.shynixn.mccoroutine.velocity.SuspendingPluginContainer
import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import plutoproject.kernel.velocity.VelocityKernel
import plutoproject.platform.common.resolvePlatformConfig
import java.nio.file.Files
import java.nio.file.Path
import java.util.logging.Logger

@Suppress("UNUSED")
class VelocityPlatform {
    private var kernel: VelocityKernel

    @Inject
    constructor(
        plugin: PluginContainer,
        server: ProxyServer,
        logger: Logger,
        @DataDirectory dataDirectory: Path
    ) {
        Files.createDirectories(dataDirectory)
        val configPath = dataDirectory.resolve("config.conf")
        if (Files.notExists(configPath)) {
            requireNotNull(javaClass.classLoader.getResourceAsStream("config.conf")) {
                "Platform configuration resource 'config.conf' doesn't exist"
            }.use { Files.copy(it, configPath) }
        }
        SuspendingPluginContainer(plugin, server, LoggerFactory.getLogger("PlutoProject/MCCoroutine"))
            .initialize(this)
        kernel = VelocityKernel(
            proxyServer = server,
            pluginContainer = plugin,
            logger = logger,
            dataFolder = dataDirectory,
            featureRoots = resolvePlatformConfig(configPath).enableFeatures
        )
        runBlocking { kernel.load() }
    }

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        runBlocking { kernel.enable() }
    }

    @Subscribe
    fun onProxyShutdown(event: ProxyShutdownEvent) {
        runBlocking { kernel.shutdown() }
    }
}
