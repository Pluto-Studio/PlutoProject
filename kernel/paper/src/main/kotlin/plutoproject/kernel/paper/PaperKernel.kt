package plutoproject.kernel.paper

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.bukkit.plugin.Plugin
import org.koin.core.Koin
import plutoproject.kernel.api.*
import plutoproject.kernel.api.paper.PaperModuleContext
import plutoproject.kernel.common.ModuleContextFactory
import plutoproject.kernel.common.ModuleOperationReporter
import plutoproject.kernel.common.ModuleResourceSaver
import plutoproject.kernel.common.RuntimeKernel
import plutoproject.kernel.common.formatModuleLifecycleSummary
import plutoproject.kernel.common.isDependencyBlocked
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.time.TimeSource

class PaperKernel(
    private val plugin: Plugin,
    dataFolder: Path,
    featureRoots: Collection<String>,
    private val classLoader: ClassLoader = PaperKernel::class.java.classLoader,
) {
    private val kernel = RuntimeKernel(
        platform = Platform.PAPER,
        featureRoots = featureRoots,
        classLoader = classLoader,
        contextFactory = ModuleContextFactory { descriptor, koin, services ->
            createContext(descriptor, dataFolder, koin, services)
        },
        reporter = ModuleOperationReporter(::report),
    )
    init {
        kernel.warnings.forEach(plugin.logger::warning)
    }

    suspend fun load(): Map<String, ModuleOperationResult> {
        val startedAt = TimeSource.Monotonic.markNow()
        val results = kernel.load()
        formatModuleLifecycleSummary(ModuleOperation.LOAD, results.values, startedAt.elapsedNow())
            .forEach(plugin.logger::info)
        return results
    }

    suspend fun enable(): Map<String, ModuleOperationResult> {
        val startedAt = TimeSource.Monotonic.markNow()
        val results = kernel.enable()
        formatModuleLifecycleSummary(ModuleOperation.ENABLE, results.values, startedAt.elapsedNow())
            .forEach(plugin.logger::info)
        return results
    }

    suspend fun shutdown() = kernel.shutdown()

    private fun createContext(
        descriptor: ModuleDescriptor,
        dataFolder: Path,
        koin: Koin,
        services: ModuleServices,
    ): PaperModuleContext {
        val moduleDataFolder = dataFolder.resolve("modules").resolve(descriptor.id)
        return PaperContext(plugin, descriptor.id, moduleDataFolder, classLoader, koin, services)
    }

    private fun report(result: ModuleOperationResult) {
        when (result) {
            is ModuleOperationResult.Failed -> plugin.logger.log(
                java.util.logging.Level.SEVERE,
                "Runtime module '${result.id}' failed during ${result.phase}",
                result.cause,
            )

            is ModuleOperationResult.Rejected -> if (!result.isDependencyBlocked()) {
                plugin.logger.warning(
                    "Runtime module '${result.id}' operation ${result.operation} was rejected: ${result.reason}",
                )
            }

            is ModuleOperationResult.Success -> if (result.operation == ModuleOperation.DISABLE) {
                plugin.logger.info("Runtime module '${result.id}' completed ${result.operation}")
            }
        }
    }
}

private class PaperContext(
    override val plugin: Plugin,
    override val id: String,
    override val dataFolder: Path,
    classLoader: ClassLoader,
    override val koin: Koin,
    override val services: ModuleServices,
) : PaperModuleContext {
    private val resources = ModuleResourceSaver(id, dataFolder, classLoader)
    override val logger: Logger = Logger.getLogger("PlutoProject/$id")
    override val coroutineScope: CoroutineScope by lazy {
        CoroutineScope(
            SupervisorJob() + Dispatchers.Default + CoroutineName("PlutoProject/$id"),
        )
    }

    override fun saveResource(path: String, output: Path, resourcePrefix: String?, replace: Boolean): Path =
        resources.save(path, output, resourcePrefix, replace)
}
