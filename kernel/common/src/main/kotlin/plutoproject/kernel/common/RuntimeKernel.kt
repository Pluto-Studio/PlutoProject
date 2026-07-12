package plutoproject.kernel.common

import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
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
    companion object {
        private val activeKernel = AtomicReference<RuntimeKernel?>()
    }

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

    val management = ModuleManagementService(manager)

    val warnings: List<String>
        get() = manager.plan.warnings

    suspend fun load(): Map<String, ModuleOperationResult> {
        check(activeKernel.get() === this || activeKernel.compareAndSet(null, this)) {
            "Another RuntimeKernel is already active in this JVM"
        }
        return try {
            manager.loadStartup()
        } catch (cause: Throwable) {
            terminateAfterStartupFailure(cause)
            throw cause
        }
    }

    suspend fun enable(): Map<String, ModuleOperationResult> = try {
        manager.enableStartup()
    } catch (cause: Throwable) {
        terminateAfterStartupFailure(cause)
        throw cause
    }

    suspend fun shutdown() {
        try {
            manager.shutdown()
        } finally {
            releaseProcessSlot()
        }
    }

    private fun releaseProcessSlot() {
        activeKernel.compareAndSet(this, null)
    }

    private suspend fun terminateAfterStartupFailure(primary: Throwable) {
        try {
            withContext(NonCancellable) { manager.shutdown() }
        } catch (cleanupFailure: Throwable) {
            if (cleanupFailure !== primary) primary.addSuppressed(cleanupFailure)
        } finally {
            releaseProcessSlot()
        }
    }
}
