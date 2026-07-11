package plutoproject.kernel.velocity

import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.proxy.ProxyServer
import java.nio.file.Files
import java.nio.file.Path
import java.util.logging.Level
import java.util.logging.Logger
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import plutoproject.kernel.api.ModuleDescriptor
import plutoproject.kernel.api.ModuleOperationResult
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.velocity.VelocityModuleContext
import plutoproject.kernel.common.ModuleContextFactory
import plutoproject.kernel.common.ModuleOperationReporter
import plutoproject.kernel.common.RuntimeKernel

class VelocityKernel(
    private val proxyServer: ProxyServer,
    private val pluginContainer: PluginContainer,
    private val logger: Logger,
    dataFolder: Path,
    featureRoots: Collection<String>,
    classLoader: ClassLoader = pluginContainer.javaClass.classLoader,
) {
    private val kernel = RuntimeKernel(
        platform = Platform.VELOCITY,
        featureRoots = featureRoots,
        classLoader = classLoader,
        contextFactory = ModuleContextFactory { descriptor -> createContext(descriptor, dataFolder) },
        reporter = ModuleOperationReporter(::report),
    )

    init {
        kernel.warnings.forEach(logger::warning)
    }

    suspend fun load(): Map<String, ModuleOperationResult> = kernel.load()

    suspend fun enable(): Map<String, ModuleOperationResult> = kernel.enable()

    suspend fun shutdown() = kernel.shutdown()

    private fun createContext(descriptor: ModuleDescriptor, dataFolder: Path): VelocityModuleContext {
        val moduleDataFolder = dataFolder.resolve("modules").resolve(descriptor.id)
        Files.createDirectories(moduleDataFolder)
        return VelocityContext(proxyServer, pluginContainer, descriptor.id, moduleDataFolder)
    }

    private fun report(result: ModuleOperationResult) {
        when (result) {
            is ModuleOperationResult.Failed -> logger.log(
                Level.SEVERE,
                "Runtime module '${result.id}' failed during ${result.phase}",
                result.cause,
            )
            is ModuleOperationResult.Rejected -> logger.warning(
                "Runtime module '${result.id}' operation ${result.operation} was rejected: ${result.reason}",
            )
            is ModuleOperationResult.Success -> logger.info(
                "Runtime module '${result.id}' completed ${result.operation}",
            )
        }
    }
}

private class VelocityContext(
    override val proxyServer: ProxyServer,
    override val pluginContainer: PluginContainer,
    override val id: String,
    override val dataFolder: Path,
) : VelocityModuleContext {
    override val logger: System.Logger = System.getLogger("PlutoProject/$id")
    override val coroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineName("PlutoProject/$id"),
    )
}
