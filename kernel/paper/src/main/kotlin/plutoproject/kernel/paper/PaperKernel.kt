package plutoproject.kernel.paper

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.bukkit.plugin.Plugin
import plutoproject.kernel.api.ModuleDescriptor
import plutoproject.kernel.api.ModuleOperationResult
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.paper.PaperModuleContext
import plutoproject.kernel.common.ModuleContextFactory
import plutoproject.kernel.common.ModuleOperationReporter
import plutoproject.kernel.common.RuntimeKernel

class PaperKernel(
    private val plugin: Plugin,
    dataFolder: Path,
    featureRoots: Collection<String>,
    classLoader: ClassLoader = plugin.javaClass.classLoader,
) {
    private val kernel = RuntimeKernel(
        platform = Platform.PAPER,
        featureRoots = featureRoots,
        classLoader = classLoader,
        contextFactory = ModuleContextFactory { descriptor -> createContext(descriptor, dataFolder) },
        reporter = ModuleOperationReporter(::report),
    )

    init {
        kernel.warnings.forEach(plugin.logger::warning)
    }

    suspend fun load(): Map<String, ModuleOperationResult> = kernel.load()

    suspend fun enable(): Map<String, ModuleOperationResult> = kernel.enable()

    suspend fun shutdown() = kernel.shutdown()

    private fun createContext(descriptor: ModuleDescriptor, dataFolder: Path): PaperModuleContext {
        val moduleDataFolder = dataFolder.resolve("modules").resolve(descriptor.id)
        Files.createDirectories(moduleDataFolder)
        return PaperContext(plugin, descriptor.id, moduleDataFolder)
    }

    private fun report(result: ModuleOperationResult) {
        when (result) {
            is ModuleOperationResult.Failed -> plugin.logger.log(
                java.util.logging.Level.SEVERE,
                "Runtime module '${result.id}' failed during ${result.phase}",
                result.cause,
            )
            is ModuleOperationResult.Rejected -> plugin.logger.warning(
                "Runtime module '${result.id}' operation ${result.operation} was rejected: ${result.reason}",
            )
            is ModuleOperationResult.Success -> plugin.logger.info(
                "Runtime module '${result.id}' completed ${result.operation}",
            )
        }
    }
}

private class PaperContext(
    override val plugin: Plugin,
    override val id: String,
    override val dataFolder: Path,
) : PaperModuleContext {
    override val logger: System.Logger = System.getLogger("PlutoProject/$id")
    override val coroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineName("PlutoProject/$id"),
    )
}
