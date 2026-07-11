package plutoproject.kernel.common

import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private data class LiveModule(val module: RuntimeModule, val context: ModuleContext)

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
        buildMap {
            plan.orderedModules.forEach { descriptor ->
                put(descriptor.id, loadOne(descriptor))
            }
        }
    }

    suspend fun enableStartup(): Map<String, ModuleOperationResult> = lifecycleMutex.withLock {
        check(loadStarted) { "Startup load must run before startup enable" }
        if (enableStarted) return@withLock rejectPlan(ModuleOperation.ENABLE, "Startup enable already executed")
        check(!shutdownStarted) { "Runtime module manager is shutting down" }
        enableStarted = true
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
        featureShutdownOrder().forEach { id ->
            if (registry.state(id) == ModuleState.ENABLED) disableOne(id, ModuleState.DISABLED)
        }
        plan.capabilities.asReversed().forEach { descriptor ->
            if (registry.state(descriptor.id) == ModuleState.ENABLED) {
                disableOne(descriptor.id, ModuleState.DISABLED)
            }
        }
        activeOptionalEdges = emptySet()
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
            liveModules[descriptor.id] = LiveModule(module, context)
            module.onLoad(context)
            succeeded(descriptor.id, ModuleOperation.LOAD, ModuleState.DISCOVERED, ModuleState.LOADED)
        } catch (cause: Throwable) {
            liveModules.remove(descriptor.id)
            context?.cancelScope()
            failed(descriptor.id, ModuleOperation.LOAD, "onLoad", cause, ModuleState.FAILED)
        }
    }

    private suspend fun enableOne(descriptor: ModuleDescriptor): ModuleOperationResult? {
        if (registry.state(descriptor.id) != ModuleState.LOADED) return null
        dependencyFailurePath(descriptor.id, ModuleState.ENABLED)?.let { path ->
            val live = liveModules[descriptor.id]
            runCatching { live?.module?.onDisable(live.context) }
            cleanup(descriptor.id)
            return blocked(descriptor, ModuleOperation.ENABLE, path)
        }
        registry.begin(descriptor.id, ModuleOperation.ENABLE)
        val live = liveModules.getValue(descriptor.id)
        return try {
            live.module.onEnable(live.context)
            succeeded(descriptor.id, ModuleOperation.ENABLE, ModuleState.LOADED, ModuleState.ENABLED)
        } catch (cause: Throwable) {
            runCatching { live.module.onDisable(live.context) }
            cleanup(descriptor.id)
            failed(descriptor.id, ModuleOperation.ENABLE, "onEnable", cause, ModuleState.FAILED)
        }
    }

    private suspend fun disableOne(id: String, finalState: ModuleState): ModuleOperationResult {
        registry.begin(id, ModuleOperation.DISABLE)
        val previousState = registry.state(id) ?: ModuleState.DISCOVERED
        val live = liveModules[id]
        val hookFailure = runCatching { live?.module?.onDisable(live.context) }.exceptionOrNull()
        cleanup(id)
        activeOptionalEdges = activeOptionalEdges.filterNotTo(linkedSetOf()) {
            it.dependent == id || it.dependency == id
        }
        return if (hookFailure == null) {
            succeeded(id, ModuleOperation.DISABLE, previousState, finalState)
        } else {
            failed(id, ModuleOperation.DISABLE, "onDisable", hookFailure, finalState)
        }
    }

    private suspend fun cleanup(id: String) {
        liveModules.remove(id)?.context?.cancelScope()
    }

    private suspend fun ModuleContext.cancelScope() {
        coroutineScope.coroutineContext[Job]?.cancelAndJoin()
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
