package plutoproject.kernel.common

import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.ModuleOperationResult
import plutoproject.kernel.api.Platform

class RuntimeKernel(
    platform: Platform,
    featureRoots: Collection<String>,
    contextFactory: ModuleContextFactory,
    classLoader: ClassLoader,
    reporter: ModuleOperationReporter = ModuleOperationReporter.NONE,
) {
    private val manager: RuntimeModuleManager

    init {
        val discovery = ModuleDiscovery(classLoader).discover(platform)
        require(discovery.errors.isEmpty()) {
            discovery.errors.joinToString("\n") { error ->
                "${error.source}: ${error.message}${error.cause?.message?.let { ": $it" }.orEmpty()}"
            }
        }
        manager = RuntimeModuleManager(
            platform = platform,
            descriptors = discovery.modules.map(DiscoveredModule::descriptor),
            featureRoots = featureRoots,
            moduleFactory = ReflectiveRuntimeModuleFactory(classLoader),
            contextFactory = contextFactory,
            reporter = reporter,
        )
    }

    val registry: ModuleRegistry
        get() = manager.registry

    val warnings: List<String>
        get() = manager.plan.warnings

    suspend fun load(): Map<String, ModuleOperationResult> = manager.loadStartup()

    suspend fun enable(): Map<String, ModuleOperationResult> = manager.enableStartup()

    suspend fun shutdown() = manager.shutdown()
}
