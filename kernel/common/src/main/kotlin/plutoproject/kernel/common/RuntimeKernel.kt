package plutoproject.kernel.common

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import plutoproject.kernel.api.ModuleOperationResult
import plutoproject.kernel.api.Platform
import java.util.concurrent.atomic.AtomicReference

class RuntimeKernel(
    prepared: PreparedRuntimeModules,
    contextFactory: ModuleContextFactory,
    classLoader: ClassLoader,
    reporter: ModuleOperationReporter = ModuleOperationReporter.NONE,
) {
    constructor(
        platform: Platform,
        featureRoots: Collection<String>,
        contextFactory: ModuleContextFactory,
        classLoader: ClassLoader,
        reporter: ModuleOperationReporter = ModuleOperationReporter.NONE,
    ) : this(
        prepared = RuntimeModulePreparation.discover(platform, featureRoots, classLoader),
        contextFactory = contextFactory,
        classLoader = classLoader,
        reporter = reporter,
    )

    companion object {
        private val activeKernel = AtomicReference<RuntimeKernel?>()
    }

    private val manager = RuntimeModuleManager(
        prepared = prepared,
        moduleFactory = ReflectiveRuntimeModuleFactory(classLoader),
        contextFactory = contextFactory,
        reporter = reporter,
    )

    val registry: ModuleRegistry
        get() = manager.registry

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
