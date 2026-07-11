package plutoproject.kernel.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import plutoproject.kernel.api.FeatureController
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.ModuleDescriptor
import plutoproject.kernel.api.ModuleOperation
import plutoproject.kernel.api.ModuleOperationResult
import plutoproject.kernel.api.ModuleState
import plutoproject.kernel.api.ModuleType
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule

fun interface ModuleContextFactory {
    fun create(descriptor: ModuleDescriptor): ModuleContext
}

class RuntimeModuleManager(
    platform: Platform,
    descriptors: Collection<ModuleDescriptor>,
    featureRoots: Collection<String>,
    private val moduleFactory: RuntimeModuleFactory,
    private val contextFactory: ModuleContextFactory,
    private val reporter: ModuleOperationReporter = ModuleOperationReporter.NONE,
) : FeatureController {
    private data class LiveModule(
        val module: RuntimeModule,
        val context: ModuleContext,
        var loadCompleted: Boolean = false,
        var disableStarted: Boolean = false,
    )

    private val validatedDescriptors = ModuleDescriptorValidator.validateForPlatform(platform, descriptors)
    private val graph = ModuleGraph(validatedDescriptors)
    val plan: ActivationPlan = ModulePlanner(graph).plan(featureRoots)
    val registry = ModuleRegistry(validatedDescriptors)

    private val lifecycleMutex = Mutex()
    private val liveModules = mutableMapOf<String, LiveModule>()
    @Volatile
    private var activeOptionalEdges: Set<ModuleEdge> = emptySet()
    private var loadStarted = false
    private var enableStarted = false
    private var shutdownStarted = false

    suspend fun loadStartup(): Map<String, ModuleOperationResult> = lifecycleMutex.withLock {
        if (loadStarted) return@withLock rejectPlan(ModuleOperation.LOAD, "Startup load already executed")
        check(!shutdownStarted) { "Runtime module manager is shutting down" }
        loadStarted = true
        try {
            buildMap {
                plan.orderedModules.forEach { descriptor ->
                    put(descriptor.id, loadOne(descriptor))
                }
            }
        } catch (cause: CancellationException) {
            terminateStartup(cause)
        }
    }

    suspend fun enableStartup(): Map<String, ModuleOperationResult> = lifecycleMutex.withLock {
        check(loadStarted) { "Startup load must run before startup enable" }
        if (enableStarted) return@withLock rejectPlan(ModuleOperation.ENABLE, "Startup enable already executed")
        check(!shutdownStarted) { "Runtime module manager is shutting down" }
        enableStarted = true
        try {
            val results = buildMap {
                plan.orderedModules.forEach { descriptor ->
                    val result = enableOne(descriptor)
                    if (result != null) put(descriptor.id, result)
                }
            }
            activeOptionalEdges = plan.proposedOptionalEdges.filterTo(linkedSetOf()) {
                registry.isEnabled(it.dependent) && registry.isEnabled(it.dependency)
            }
            results
        } catch (cause: CancellationException) {
            terminateStartup(cause)
        }
    }

    override suspend fun disable(id: String): ModuleOperationResult = lifecycleMutex.withLock {
        val descriptor = registry.descriptor(id)
            ?: return@withLock rejected(id, ModuleOperation.DISABLE, "Unknown module '$id'")
        if (descriptor.type == ModuleType.CAPABILITY) {
            return@withLock rejected(id, ModuleOperation.DISABLE, "Capabilities cannot be disabled at runtime")
        }
        if (!enableStarted || shutdownStarted) {
            return@withLock rejected(id, ModuleOperation.DISABLE, "Runtime feature disable is unavailable")
        }
        if (registry.state(id) != ModuleState.ENABLED) {
            return@withLock rejected(id, ModuleOperation.DISABLE, "Feature '$id' is not enabled")
        }
        val enabledFeatures = registry.snapshots()
            .filter { it.descriptor.type == ModuleType.FEATURE && it.state == ModuleState.ENABLED }
            .mapTo(linkedSetOf()) { it.descriptor.id }
        val blockers = graph.blockerPaths(id, enabledFeatures, activeOptionalEdges)
        if (blockers.isNotEmpty()) {
            return@withLock rejected(
                id,
                ModuleOperation.DISABLE,
                "Enabled features still depend on '$id'",
                blockers,
            )
        }
        disableOne(id, ModuleState.DISABLED)
    }

    suspend fun shutdown() = lifecycleMutex.withLock {
        if (shutdownStarted) return@withLock
        shutdownStarted = true
        var hookCancellation: CancellationException? = null
        withContext(NonCancellable) {
            featureShutdownOrder().forEach { id ->
                if (registry.state(id) == ModuleState.ENABLED) {
                    hookCancellation = disableDuringShutdown(id, hookCancellation)
                }
            }
            plan.capabilities.asReversed().forEach { descriptor ->
                if (registry.state(descriptor.id) == ModuleState.ENABLED) {
                    hookCancellation = disableDuringShutdown(descriptor.id, hookCancellation)
                }
            }
            activeOptionalEdges = emptySet()
        }
        hookCancellation?.let { throw it }
        currentCoroutineContext().ensureActive()
    }

    fun activeOptionalDependencies(): Set<ModuleEdge> = activeOptionalEdges

    private suspend fun loadOne(descriptor: ModuleDescriptor): ModuleOperationResult {
        dependencyFailurePath(descriptor.id, ModuleState.LOADED)?.let { path ->
            return blocked(descriptor, ModuleOperation.LOAD, path)
        }
        registry.begin(descriptor.id, ModuleOperation.LOAD)
        var context: ModuleContext? = null
        return try {
            context = contextFactory.create(descriptor)
            val module = moduleFactory.create(descriptor)
            val live = LiveModule(module, context)
            liveModules[descriptor.id] = live
            module.onLoad(context)
            live.loadCompleted = true
            succeeded(descriptor.id, ModuleOperation.LOAD, ModuleState.DISCOVERED, ModuleState.LOADED)
        } catch (cause: CancellationException) {
            if (liveModules[descriptor.id] == null) {
                withContext(NonCancellable) { context?.cancelScope() }
                val state = if (context == null) ModuleState.DISCOVERED else ModuleState.DISABLED
                registry.terminate(descriptor.id, state)
            }
            throw cause
        } catch (cause: Throwable) {
            if (liveModules[descriptor.id] == null) {
                context?.cancelScope()
            } else {
                cleanup(descriptor.id)
            }
            failed(descriptor.id, ModuleOperation.LOAD, "onLoad", cause, ModuleState.FAILED)
        }
    }

    private suspend fun enableOne(descriptor: ModuleDescriptor): ModuleOperationResult? {
        if (registry.state(descriptor.id) != ModuleState.LOADED) return null
        dependencyFailurePath(descriptor.id, ModuleState.ENABLED)?.let { path ->
            val live = liveModules[descriptor.id]
            try {
                live?.disableStarted = true
                live?.module?.onDisable(live.context)
            } catch (cause: CancellationException) {
                throw cause
            } catch (_: Throwable) {
                // Dependency failure remains the primary result on this path.
            }
            cleanup(descriptor.id)
            return blocked(descriptor, ModuleOperation.ENABLE, path)
        }
        registry.begin(descriptor.id, ModuleOperation.ENABLE)
        val live = liveModules.getValue(descriptor.id)
        return try {
            live.module.onEnable(live.context)
            succeeded(descriptor.id, ModuleOperation.ENABLE, ModuleState.LOADED, ModuleState.ENABLED)
        } catch (cause: Throwable) {
            if (cause is CancellationException) throw cause
            try {
                live.disableStarted = true
                live.module.onDisable(live.context)
            } catch (cleanupCancellation: CancellationException) {
                cleanupCancellation.addSuppressed(cause)
                throw cleanupCancellation
            } catch (cleanupFailure: Throwable) {
                cause.addSuppressed(cleanupFailure)
            }
            try {
                cleanup(descriptor.id)
            } catch (cleanupCancellation: CancellationException) {
                cleanupCancellation.addSuppressed(cause)
                throw cleanupCancellation
            }
            failed(descriptor.id, ModuleOperation.ENABLE, "onEnable", cause, ModuleState.FAILED)
        }
    }

    private suspend fun disableOne(id: String, finalState: ModuleState): ModuleOperationResult {
        registry.begin(id, ModuleOperation.DISABLE)
        val previousState = registry.state(id) ?: ModuleState.DISCOVERED
        val live = liveModules[id]
        try {
            val hookFailure = try {
                live?.disableStarted = true
                live?.module?.onDisable(live.context)
                null
            } catch (cause: CancellationException) {
                throw cause
            } catch (cause: Throwable) {
                cause
            }
            cleanup(id)
            removeOptionalEdges(id)
            return if (hookFailure == null) {
                succeeded(id, ModuleOperation.DISABLE, previousState, finalState)
            } else {
                failed(id, ModuleOperation.DISABLE, "onDisable", hookFailure, finalState)
            }
        } catch (cause: CancellationException) {
            withContext(NonCancellable) {
                val cleanupFailure = runCleanup(id)
                removeOptionalEdges(id)
                val reportableFailure = cleanupFailure?.reportableFailure()
                if (reportableFailure == null) {
                    registry.terminate(id, finalState)
                } else {
                    failed(id, ModuleOperation.DISABLE, "cleanup", reportableFailure, finalState)
                }
                if (cleanupFailure != null) cause.addSuppressed(cleanupFailure)
            }
            throw cause
        }
    }

    private suspend fun cleanup(id: String) {
        val live = liveModules[id] ?: return
        live.context.cancelScope()
        liveModules.remove(id, live)
    }

    private suspend fun ModuleContext.cancelScope() {
        coroutineScope.coroutineContext[Job]?.cancelAndJoin()
    }

    private suspend fun terminateStartup(cause: CancellationException): Nothing {
        shutdownStarted = true
        withContext(NonCancellable) {
            plan.orderedModules.asReversed().forEach { descriptor ->
                val live = liveModules[descriptor.id] ?: return@forEach
                val cleanupFailure = terminateLiveModule(descriptor.id, live)
                if (cleanupFailure != null) cause.addSuppressed(cleanupFailure)
            }
            activeOptionalEdges = emptySet()
        }
        throw cause
    }

    private suspend fun terminateLiveModule(id: String, live: LiveModule): Throwable? {
        registry.begin(id, ModuleOperation.DISABLE)
        var failure: Throwable? = null
        if (live.loadCompleted && !live.disableStarted) {
            live.disableStarted = true
            failure = try {
                live.module.onDisable(live.context)
                null
            } catch (cause: Throwable) {
                cause
            }
        }
        failure = combineFailures(failure, runCleanup(id))
        removeOptionalEdges(id)
        completeTermination(id, failure)
        return failure
    }

    private suspend fun runCleanup(id: String): Throwable? = try {
        cleanup(id)
        null
    } catch (cause: Throwable) {
        cause
    }

    private fun completeTermination(id: String, failure: Throwable?) {
        val reportableFailure = failure?.reportableFailure()
        if (reportableFailure == null) {
            registry.terminate(id, ModuleState.DISABLED)
        } else {
            failed(id, ModuleOperation.DISABLE, "onDisable", reportableFailure, ModuleState.DISABLED)
        }
    }

    private fun Throwable.reportableFailure(): Throwable? = when {
        this !is CancellationException -> this
        else -> suppressed.firstNotNullOfOrNull { it.reportableFailure() }
    }

    private fun combineFailures(primary: Throwable?, secondary: Throwable?): Throwable? {
        if (primary == null) return secondary
        if (secondary != null && secondary !== primary) primary.addSuppressed(secondary)
        return primary
    }

    private fun removeOptionalEdges(id: String) {
        activeOptionalEdges = activeOptionalEdges.filterNotTo(linkedSetOf()) {
            it.dependent == id || it.dependency == id
        }
    }

    private suspend fun disableDuringShutdown(
        id: String,
        previousCancellation: CancellationException?,
    ): CancellationException? = try {
        disableOne(id, ModuleState.DISABLED)
        previousCancellation
    } catch (cause: CancellationException) {
        previousCancellation?.apply { addSuppressed(cause) } ?: cause
    }

    private fun dependencyFailurePath(id: String, expected: ModuleState): List<String>? {
        fun find(current: String, path: List<String>): List<String>? {
            for (dependency in graph.requiredDependencies(current)) {
                val nextPath = path + dependency
                if (registry.state(dependency) != expected) {
                    return find(dependency, nextPath) ?: nextPath
                }
            }
            return null
        }
        return find(id, listOf(id))
    }

    private fun featureShutdownOrder(): List<String> {
        val enabled = registry.snapshots()
            .filter { it.descriptor.type == ModuleType.FEATURE && it.state == ModuleState.ENABLED }
            .mapTo(linkedSetOf()) { it.descriptor.id }
        val optional = activeOptionalEdges.groupBy(ModuleEdge::dependent)
        val visited = mutableSetOf<String>()
        val dependencyFirst = mutableListOf<String>()
        fun visit(id: String) {
            if (!visited.add(id)) return
            val required = graph.descriptors.getValue(id).requiredFeatures.filter { it in enabled }
            val optionalIds = optional[id].orEmpty().map(ModuleEdge::dependency).filter { it in enabled }
            (required + optionalIds).forEach(::visit)
            dependencyFirst += id
        }
        enabled.forEach(::visit)
        return dependencyFirst.asReversed()
    }

    private fun blocked(
        descriptor: ModuleDescriptor,
        operation: ModuleOperation,
        path: List<String>,
    ) = rejected(
        descriptor.id,
        operation,
        "Required dependency is unavailable: ${path.joinToString(" -> ")}",
        listOf(path),
        ModuleState.BLOCKED,
    )

    private fun rejectPlan(operation: ModuleOperation, reason: String) = plan.orderedModules.associate {
        it.id to rejected(it.id, operation, reason)
    }

    private fun succeeded(
        id: String,
        operation: ModuleOperation,
        previous: ModuleState,
        state: ModuleState,
    ): ModuleOperationResult.Success {
        val result = ModuleOperationResult.Success(id, operation, previous, state)
        registry.complete(id, state, result)
        reporter.report(result)
        return result
    }

    private fun rejected(
        id: String,
        operation: ModuleOperation,
        reason: String,
        paths: List<List<String>> = emptyList(),
        state: ModuleState = registry.state(id) ?: ModuleState.DISCOVERED,
    ): ModuleOperationResult.Rejected {
        val result = ModuleOperationResult.Rejected(id, operation, reason, paths)
        if (registry.descriptor(id) != null) registry.complete(id, state, result, paths.firstOrNull().orEmpty())
        reporter.report(result)
        return result
    }

    private fun failed(
        id: String,
        operation: ModuleOperation,
        phase: String,
        cause: Throwable,
        state: ModuleState,
    ): ModuleOperationResult.Failed {
        val result = ModuleOperationResult.Failed(id, operation, phase, cause)
        registry.complete(id, state, result, failure = cause)
        reporter.report(result)
        return result
    }
}
