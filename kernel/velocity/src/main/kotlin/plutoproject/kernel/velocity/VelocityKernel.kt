package plutoproject.kernel.velocity

import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.proxy.ProxyServer
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.Koin
import plutoproject.kernel.api.*
import plutoproject.kernel.api.velocity.VelocityModuleContext
import plutoproject.kernel.common.ModuleContextFactory
import plutoproject.kernel.common.ModuleOperationReporter
import plutoproject.kernel.common.ModuleResourceSaver
import plutoproject.kernel.common.RuntimeKernel
import java.nio.file.Path
import java.util.logging.Level
import java.util.logging.Logger

class VelocityKernel(
    private val proxyServer: ProxyServer,
    private val pluginContainer: PluginContainer,
    private val logger: Logger,
    dataFolder: Path,
    featureRoots: Collection<String>,
    registerCommands: Boolean = true,
    private val classLoader: ClassLoader = VelocityKernel::class.java.classLoader,
) {
    private val kernel = RuntimeKernel(
        platform = Platform.VELOCITY,
        featureRoots = featureRoots,
        classLoader = classLoader,
        contextFactory = ModuleContextFactory { descriptor, koin, services ->
            createContext(descriptor, dataFolder, koin, services)
        },
        reporter = ModuleOperationReporter(::report),
    )

    init {
        kernel.warnings.forEach(logger::warning)
        if (registerCommands) {
            val command = BrigadierCommand(createManagementCommand(kernel.management).build())
            proxyServer.commandManager.register(
                proxyServer.commandManager.metaBuilder(command).plugin(pluginContainer).build(),
                command,
            )
        }
    }

    suspend fun load(): Map<String, ModuleOperationResult> = kernel.load()

    suspend fun enable(): Map<String, ModuleOperationResult> = kernel.enable()

    suspend fun shutdown() = kernel.shutdown()

    private fun createContext(
        descriptor: ModuleDescriptor,
        dataFolder: Path,
        koin: Koin,
        services: ModuleServices,
    ): VelocityModuleContext {
        val moduleDataFolder = dataFolder.resolve("modules").resolve(descriptor.id)
        return VelocityContext(
            proxyServer,
            pluginContainer,
            descriptor.id,
            moduleDataFolder,
            classLoader,
            koin,
            services
        )
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
    classLoader: ClassLoader,
    override val koin: Koin,
    override val services: ModuleServices,
) : VelocityModuleContext {
    private val resources = ModuleResourceSaver(id, dataFolder, classLoader)
    override val logger: Logger = Logger.getLogger("PlutoProject/$id")
    override val coroutineScope: CoroutineScope by lazy {
        CoroutineScope(
            SupervisorJob() + Dispatchers.Default + CoroutineName("PlutoProject/$id") + ModuleContextElement(this),
        )
    }

    override fun saveResource(path: String, output: Path, resourcePrefix: String?, replace: Boolean): Path =
        resources.save(path, output, resourcePrefix, replace)
}
