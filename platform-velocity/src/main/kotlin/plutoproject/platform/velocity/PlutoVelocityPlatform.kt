package plutoproject.platform.velocity

import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.proxy.ProxyServer
import java.nio.file.Path
import java.util.logging.Logger
import kotlinx.coroutines.runBlocking
import plutoproject.framework.common.FrameworkCommonModule
import plutoproject.framework.common.PlutoConfig
import plutoproject.framework.common.api.feature.FeatureManager
import plutoproject.framework.common.util.coroutine.shutdownCoroutineEnvironment
import plutoproject.framework.common.util.inject.globalKoin
import plutoproject.framework.velocity.FrameworkVelocityModule
import plutoproject.framework.velocity.disableFrameworkModules
import plutoproject.framework.velocity.enableFrameworkModules
import plutoproject.framework.velocity.loadFrameworkModules
import plutoproject.kernel.velocity.VelocityKernel

class PlutoVelocityPlatform(
    private val plugin: PluginContainer,
    private val server: ProxyServer,
    private val logger: Logger,
    private val dataFolder: Path,
) {
    private lateinit var kernel: VelocityKernel

    fun load() {
        globalKoin {
            modules(FrameworkCommonModule, FrameworkVelocityModule)
        }
        kernel = VelocityKernel(
            server,
            plugin,
            logger,
            dataFolder,
            globalKoin.get<PlutoConfig>().feature.autoLoad,
        )
        runBlocking { kernel.load() }
        loadFrameworkModules()
        FeatureManager.loadAll()
    }

    fun enable() {
        runBlocking { kernel.enable() }
        enableFrameworkModules()
        FeatureManager.enableAll()
    }

    fun disable() {
        runBlocking { kernel.shutdown() }
        FeatureManager.disableAll()
        disableFrameworkModules()
        shutdownCoroutineEnvironment()
    }
}
