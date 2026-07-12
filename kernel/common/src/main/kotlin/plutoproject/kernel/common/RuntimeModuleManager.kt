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
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import plutoproject.kernel.api.FeatureController
import plutoproject.kernel.api.InternalKernelApi
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.ModuleContextBinding
import plutoproject.kernel.api.ModuleContextElement
import plutoproject.kernel.api.ModuleServices
import plutoproject.kernel.api.ModuleDescriptor
import plutoproject.kernel.api.ModuleOperation
import plutoproject.kernel.api.ModuleOperationResult
import plutoproject.kernel.api.ModuleState
import plutoproject.kernel.api.ModuleType
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule

fun interface ModuleContextFactory {
    fun create(descriptor: ModuleDescriptor, koin: Koin, services: ModuleServices): ModuleContext
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
        val koinApplication: KoinApplication,
        val services: RuntimeServiceRegistry.OwnerServices,
        var loadCompleted: Boolean = false,
        var disableStarted: Boolean = false,
    )

    private data class ModuleResources(
        val context: ModuleContext?,
        val koinApplication: KoinApplication?,
        val services: RuntimeServiceRegistry.OwnerServices?,
        val entrypoint: String?,
    )

    private val validatedDescriptors = ModuleDescriptorValidator.validateForPlatform(platform, descriptors)
    private val graph = ModuleGraph(validatedDescriptors)
    val plan: ActivationPlan = ModulePlanner(graph).plan(featureRoots)
    val registry = ModuleRegistry(validatedDescriptors)

    private val lifecycleMutex = Mutex()
    private val serviceRegistry = RuntimeServiceRegistry()
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
            plan.orderedModules.asReversed().forEach { descriptor ->
                val live = liveModules[descriptor.id] ?: return@forEach
                val failure = terminateLiveModule(descriptor.id, live)
                if (failure is CancellationException) {
                    hookCancellation = hookCancellation?.apply { addSuppressed(failure) } ?: failure
                }
            }
            activeOptionalEdges = emptySet()
        }
        hookCancellation?.let { throw it }
        currentCoroutineContext().ensureActive()
    }

    fun activeOptionalDependencies(): Set<ModuleEdge> = activeOptionalEdges

    @OptIn(plutoproject.kernel.api.InternalKernelApi::class)
    private suspend fun loadOne(descriptor: ModuleDescriptor): ModuleOperationResult {
        dependencyFailurePath(descriptor.id, ModuleState.LOADED)?.let { path ->
            return blocked(descriptor, ModuleOperation.LOAD, path)
        }
        registry.begin(descriptor.id, ModuleOperation.LOAD)
        var context: ModuleContext? = null
        var koinApplication: KoinApplication? = null
        var services: RuntimeServiceRegistry.OwnerServices? = null
        var contextRegistered = false
        return try {
            koinApplication = koinApplication()
            services = serviceRegistry.owner(descriptor.id, koinApplication.koin)
            context = contextFactory.create(descriptor, koinApplication.koin, services)
            koinApplication.logger(ModuleKoinLogger(context.logger))
            ModuleContextBinding.register(context, descriptor.entrypoint)
            contextRegistered = true
            val module = ModuleContextBinding.withContext(context) { moduleFactory.create(descriptor) }
            services.activate()
            val live = LiveModule(module, context, koinApplication, services)
            liveModules[descriptor.id] = live
            invokeInContext(context) { module.onLoad(context) }
            live.loadCompleted = true
            succeeded(descriptor.id, ModuleOperation.LOAD, ModuleState.DISCOVERED, ModuleState.LOADED)
        } catch (cause: CancellationException) {
            if (liveModules[descriptor.id] == null) {
                withContext(NonCancellable) {
                    cleanupBeforeLive(descriptor, context, koinApplication, services, contextRegistered)
                        ?.let(cause::addSuppressed)
                }
                val state = if (context == null) ModuleState.DISCOVERED else ModuleState.DISABLED
                registry.terminate(descriptor.id, state)
            }
            throw cause
        } catch (cause: Throwable) {
            val cleanupFailure = if (liveModules[descriptor.id] == null) {
                cleanupBeforeLive(descriptor, context, koinApplication, services, contextRegistered)
            } else {
                runCleanup(descriptor.id)
            }
            if (cleanupFailure != null) cause.addSuppressed(cleanupFailure)
            failed(descriptor.id, ModuleOperation.LOAD, "onLoad", cause, ModuleState.FAILED)
        }
    }

    @OptIn(plutoproject.kernel.api.InternalKernelApi::class)
    private suspend fun cleanupBeforeLive(
        descriptor: ModuleDescriptor,
        context: ModuleContext?,
        koinApplication: KoinApplication?,
        services: RuntimeServiceRegistry.OwnerServices?,
        contextRegistered: Boolean,
    ): Throwable? = cleanupResources(
        ModuleResources(
            context = context,
            koinApplication = koinApplication,
            services = services,
            entrypoint = descriptor.entrypoint.takeIf { contextRegistered },
        ),
    )

    private suspend fun enableOne(descriptor: ModuleDescriptor): ModuleOperationResult? {
        if (registry.state(descriptor.id) != ModuleState.LOADED) return null
        dependencyFailurePath(descriptor.id, ModuleState.ENABLED)?.let { path ->
            val live = liveModules[descriptor.id]
            try {
                live?.let { disableLive(it) }
            } catch (cause: CancellationException) {
                throw cause
            } catch (_: Throwable) {
                // Dependency failure remains the primary result on this path.
            }
            runCleanup(descriptor.id)
            return blocked(descriptor, ModuleOperation.ENABLE, path)
        }
        registry.begin(descriptor.id, ModuleOperation.ENABLE)
        val live = liveModules.getValue(descriptor.id)
        return try {
            invokeInContext(live.context) { live.module.onEnable(live.context) }
            succeeded(descriptor.id, ModuleOperation.ENABLE, ModuleState.LOADED, ModuleState.ENABLED)
        } catch (cause: Throwable) {
            if (cause is CancellationException) throw cause
            rollbackEnable(descriptor.id, live, cause)
        }
    }

    private suspend fun rollbackEnable(
        id: String,
        live: LiveModule,
        enableFailure: Throwable,
    ): ModuleOperationResult.Failed {
        try {
            disableLive(live)
        } catch (cause: CancellationException) {
            cause.addSuppressed(enableFailure)
            throw cause
        } catch (cause: Throwable) {
            enableFailure.addSuppressed(cause)
        }
        val cleanupFailure = runCleanup(id)
        if (cleanupFailure is CancellationException) {
            cleanupFailure.addSuppressed(enableFailure)
            throw cleanupFailure
        }
        if (cleanupFailure != null) enableFailure.addSuppressed(cleanupFailure)
        return failed(id, ModuleOperation.ENABLE, "onEnable", enableFailure, ModuleState.FAILED)
    }

    private suspend fun disableOne(id: String, finalState: ModuleState): ModuleOperationResult {
        registry.begin(id, ModuleOperation.DISABLE)
        val previousState = registry.state(id) ?: ModuleState.DISCOVERED
        val live = liveModules[id]
        try {
            val hookFailure = try {
                live?.let { disableLive(it) }
                null
            } catch (cause: CancellationException) {
                throw cause
            } catch (cause: Throwable) {
                cause
            }
            val cleanupFailure = runCleanup(id)
            removeOptionalEdges(id)
            val failure = combineFailures(hookFailure, cleanupFailure)
            return if (failure == null) {
                succeeded(id, ModuleOperation.DISABLE, previousState, finalState)
            } else {
                failed(
                    id,
                    ModuleOperation.DISABLE,
                    if (hookFailure == null) "cleanup" else "onDisable",
                    failure,
                    finalState,
                )
            }
        } catch (cause: CancellationException) {
            completeCancelledDisable(id, finalState, cause)
        }
    }

    private suspend fun completeCancelledDisable(
        id: String,
        finalState: ModuleState,
        cancellation: CancellationException,
    ): Nothing {
        withContext(NonCancellable) {
            val cleanupFailure = runCleanup(id)
            removeOptionalEdges(id)
            val reportableFailure = cleanupFailure?.reportableFailure()
            if (reportableFailure == null) {
                registry.terminate(id, finalState)
            } else {
                failed(id, ModuleOperation.DISABLE, "cleanup", reportableFailure, finalState)
            }
            if (cleanupFailure != null) cancellation.addSuppressed(cleanupFailure)
        }
        throw cancellation
    }

    @OptIn(InternalKernelApi::class)
    private suspend fun cleanup(id: String) {
        val live = liveModules[id] ?: return
        val failure = cleanupResources(
            ModuleResources(
                context = live.context,
                koinApplication = live.koinApplication,
                services = live.services,
                entrypoint = registry.descriptor(id)!!.entrypoint,
            ),
        )
        liveModules.remove(id, live)
        if (failure != null) throw failure
    }

    @OptIn(InternalKernelApi::class)
    private suspend fun cleanupResources(resources: ModuleResources): Throwable? {
        var failure: Throwable? = null
        suspend fun runStep(block: suspend () -> Unit) {
            try {
                block()
            } catch (cause: Throwable) {
                failure = combineFailures(failure, cause)
            }
        }

        runStep { resources.services?.beginClosing() }
        runStep { resources.context?.cancelScope() }
        runStep { resources.koinApplication?.close() }
        runStep { resources.services?.close() }
        runStep { resources.entrypoint?.let(ModuleContextBinding::close) }
        return failure
    }

    private suspend fun ModuleContext.cancelScope() {
        coroutineScope.coroutineContext[Job]?.cancelAndJoin()
    }

    private suspend fun <T> invokeInContext(context: ModuleContext, block: suspend () -> T): T {
        var hookCancellation: CancellationException? = null
        return try {
            withContext(ModuleContextElement(context)) {
                try {
                    block()
                } catch (cause: CancellationException) {
                    hookCancellation = cause
                    throw cause
                }
            }
        } catch (cause: CancellationException) {
            throw hookCancellation ?: cause
        }
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
            failure = try {
                disableLive(live)
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

    private suspend fun disableLive(live: LiveModule) {
        live.disableStarted = true
        live.services.beginClosing()
        invokeInContext(live.context) { live.module.onDisable(live.context) }
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
