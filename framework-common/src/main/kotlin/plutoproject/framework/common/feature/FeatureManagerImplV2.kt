package plutoproject.framework.common.feature

import kotlinx.coroutines.cancel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import plutoproject.framework.common.PlutoConfig
import plutoproject.framework.common.api.feature.*
import plutoproject.framework.common.api.feature.metadata.AbstractFeature
import plutoproject.framework.common.api.feature.metadata.DependencyMetadata
import plutoproject.framework.common.feature.dependency.FeatureDependencyGraph
import plutoproject.framework.common.util.featureDataFolder
import plutoproject.framework.common.util.jvm.findClass
import plutoproject.framework.common.util.platformType
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.reflect.full.createInstance

class FeatureManagerImplV2 : FeatureManager, KoinComponent {
    private val config by lazy { get<PlutoConfig>().feature }
    private val _loadedFeatures = mutableMapOf<String, Feature>()
    private val manifest = readManifest()
    private val metadata = manifest.metadata
    private val dependencyGraph by lazy { FeatureDependencyGraph(metadata, logger) }
    private val loadResults = mutableMapOf<String, DetailedResult>()
    private val enableResults = mutableMapOf<String, DetailedResult>()
    private val reloadResults = mutableMapOf<String, DetailedResult>()
    private val disableResults = mutableMapOf<String, DetailedResult>()
    private val loadingFeatures = mutableSetOf<String>()
    private val enablingFeatures = mutableSetOf<String>()

    init {
        manifest.globalErrors.forEach { errorMessage ->
            logger.severe(errorMessage)
        }
    }

    override val loadedFeatures: Collection<Feature>
        get() = _loadedFeatures.values

    override val enabledFeatures: Collection<Feature>
        get() = loadedFeatures.filter { it.state == State.ENABLED }

    override val disabledFeatures: Collection<Feature>
        get() = loadedFeatures.filter { it.state == State.DISABLED }

    private fun readManifest(): FeatureManifestLoader.ManifestLoadResult {
        return FeatureManifestLoader.load(platformType)
    }

    override fun getMetadata(id: String): FeatureMetadata? = metadata[id]

    private fun createInstance(metadata: FeatureMetadata): AbstractFeature {
        val entryClass = findClass(metadata.entrypoint)?.kotlin
            ?: error("无法找到 Feature 类：${metadata.entrypoint}")
        val feature = entryClass.createInstance() as? AbstractFeature
            ?: error("Feature ${metadata.id} 的入口类不是 AbstractFeature：${metadata.entrypoint}")

        feature.init(
            metadata.id,
            Logger.getLogger("PlutoProject/${metadata.id}"),
            featureDataFolder.resolve("${metadata.id}${File.separator}"),
        )
        return feature
    }

    private fun cacheResult(
        action: Action,
        id: String,
        result: DetailedResult,
        log: Boolean = true,
    ): DetailedResult {
        when (action) {
            Action.LOAD -> loadResults[id] = result
            Action.ENABLE -> enableResults[id] = result
            Action.RELOAD -> reloadResults[id] = result
            Action.DISABLE -> disableResults[id] = result
        }

        if (log) {
            logResult(action, id, result)
        }
        return result
    }

    private fun logResult(action: Action, id: String, result: DetailedResult) {
        val actionName = when (action) {
            Action.LOAD -> "加载"
            Action.ENABLE -> "启用"
            Action.RELOAD -> "重载"
            Action.DISABLE -> "卸载"
        }

        when (result.status) {
            FeatureProcessResult.SUCCESS -> logger.info("${actionName}功能模块 $id：成功")
            FeatureProcessResult.SKIPPED -> logger.info("${actionName}功能模块 $id：跳过，原因：${result.reason}")
            FeatureProcessResult.FAILED -> {
                val message = "${actionName}功能模块 $id：失败，原因：${result.reason}"
                if (result.cause != null) {
                    logger.log(Level.SEVERE, message, result.cause)
                } else {
                    logger.severe(message)
                }
            }
        }
    }

    private fun resetOperationResults() {
        loadResults.clear()
        enableResults.clear()
        reloadResults.clear()
        disableResults.clear()
    }

    private fun summarize(action: Action, results: Map<String, FeatureProcessResult>) {
        if (results.isEmpty()) {
            return
        }

        val successCount = results.values.count { it == FeatureProcessResult.SUCCESS }
        val skippedCount = results.values.count { it == FeatureProcessResult.SKIPPED }
        val failedCount = results.values.count { it == FeatureProcessResult.FAILED }
        val actionName = when (action) {
            Action.LOAD -> "加载"
            Action.ENABLE -> "启用"
            Action.RELOAD -> "重载"
            Action.DISABLE -> "卸载"
        }
        logger.info("功能模块批量${actionName}完成：成功 $successCount，跳过 $skippedCount，失败 $failedCount")
    }

    private fun invalidManifestResult(id: String): DetailedResult? {
        val reason = manifest.invalidFeatures[id] ?: return null
        return DetailedResult(
            status = FeatureProcessResult.SKIPPED,
            phase = Phase.MANIFEST,
            reason = reason,
        )
    }

    private fun configDisabledResult(id: String): DetailedResult {
        return DetailedResult(
            status = FeatureProcessResult.SKIPPED,
            phase = Phase.CONFIG,
            reason = "功能模块未在配置中启用",
        )
    }

    private fun cycleResult(id: String): DetailedResult {
        val cycleMembers = dependencyGraph.getCycleGroup(id)
            .ifEmpty { setOf(id) }
            .sorted()
        val cycleMessage = if (cycleMembers.isEmpty()) id else cycleMembers.joinToString(", ")
        return DetailedResult(
            status = FeatureProcessResult.SKIPPED,
            phase = Phase.DEPENDENCY,
            reason = "存在循环依赖，相关功能模块：$cycleMessage",
        )
    }

    private fun dependencySkippedResult(
        id: String,
        dependencyId: String,
        action: Action,
        dependencyResult: DetailedResult
    ): DetailedResult {
        val actionName = when (action) {
            Action.LOAD -> "加载"
            Action.ENABLE -> "启用"
            Action.RELOAD -> "重载"
            Action.DISABLE -> "禁用"
        }
        val reason = if (id == dependencyId) {
            "前置处理结果为 ${dependencyResult.status}：${dependencyResult.reason}"
        } else {
            "依赖功能模块 $dependencyId 的处理结果为 ${dependencyResult.status}：${dependencyResult.reason}"
        }
        return DetailedResult(
            status = FeatureProcessResult.SKIPPED,
            phase = Phase.DEPENDENCY,
            reason = reason,
        )
    }

    private fun successResult(action: Action, id: String): DetailedResult {
        val actionName = when (action) {
            Action.LOAD -> "已成功加载"
            Action.ENABLE -> "已成功启用"
            Action.RELOAD -> "已成功重载"
            Action.DISABLE -> "已成功卸载"
        }
        return DetailedResult(
            status = FeatureProcessResult.SUCCESS,
            phase = Phase.NONE,
            reason = actionName,
        )
    }

    private fun loadDependency(metadata: FeatureMetadata, dependency: DependencyMetadata, stage: Load) {
        if (dependency.load != stage) {
            return
        }

        val dependencyId = dependency.id
        if (!dependency.required && getMetadata(dependencyId) == null) {
            logger.info("加载功能模块 ${metadata.id} 时跳过可选依赖 $dependencyId：依赖元数据不存在")
            return
        }

        val dependencyResult = loadFeatureDetailed(dependencyId)
        if (dependency.required && dependencyResult.status != FeatureProcessResult.SUCCESS) {
            throw DependencyNotReadyException(dependencyId, dependencyResult)
        }

        if (!dependency.required && dependencyResult.status != FeatureProcessResult.SUCCESS) {
            logger.info("加载功能模块 ${metadata.id} 时跳过可选依赖 $dependencyId：${dependencyResult.reason}")
        }
    }

    private fun enableDependency(metadata: FeatureMetadata, dependency: DependencyMetadata, stage: Load) {
        if (dependency.load != stage) {
            return
        }

        val dependencyId = dependency.id
        if (!dependency.required && getMetadata(dependencyId) == null) {
            logger.info("启用功能模块 ${metadata.id} 时跳过可选依赖 $dependencyId：依赖元数据不存在")
            return
        }

        val dependencyResult = enableFeatureDetailed(dependencyId)
        if (dependency.required && dependencyResult.status != FeatureProcessResult.SUCCESS) {
            throw DependencyNotReadyException(dependencyId, dependencyResult)
        }

        if (!dependency.required && dependencyResult.status != FeatureProcessResult.SUCCESS) {
            logger.info("启用功能模块 ${metadata.id} 时跳过可选依赖 $dependencyId：${dependencyResult.reason}")
        }
    }

    private fun loadFeatureDetailed(id: String): DetailedResult {
        if (isLoaded(id)) {
            return successResult(Action.LOAD, id)
        }

        loadResults[id]?.let { cached ->
            return cached
        }

        invalidManifestResult(id)?.let { result ->
            return cacheResult(Action.LOAD, id, result)
        }

        if (!isEnabledInConfig(id)) {
            return cacheResult(Action.LOAD, id, configDisabledResult(id))
        }

        if (dependencyGraph.isPermanentlyDisabled(id)) {
            return cacheResult(Action.LOAD, id, cycleResult(id))
        }

        val meta = getMetadata(id)
            ?: return cacheResult(
                Action.LOAD,
                id,
                DetailedResult(
                    status = FeatureProcessResult.SKIPPED,
                    phase = Phase.MANIFEST,
                    reason = "未找到功能模块元数据",
                ),
            )

        val missingDependencies = dependencyGraph.getRequiredDependencies(id)
            .map { it.id }
            .filter { getMetadata(it) == null }
        if (missingDependencies.isNotEmpty()) {
            return cacheResult(
                Action.LOAD,
                id,
                DetailedResult(
                    status = FeatureProcessResult.SKIPPED,
                    phase = Phase.DEPENDENCY,
                    reason = "缺少必需依赖：${missingDependencies.joinToString(", ")}",
                ),
            )
        }

        if (!loadingFeatures.add(id)) {
            return cacheResult(
                Action.LOAD,
                id,
                DetailedResult(
                    status = FeatureProcessResult.SKIPPED,
                    phase = Phase.DEPENDENCY,
                    reason = "检测到递归加载，已跳过",
                ),
            )
        }

        try {
            try {
                meta.dependencies.forEach { dependency ->
                    loadDependency(meta, dependency, Load.BEFORE)
                }
            } catch (e: DependencyNotReadyException) {
                return cacheResult(Action.LOAD, id, dependencySkippedResult(id, e.dependencyId, Action.LOAD, e.result))
            }

            val feature = try {
                createInstance(meta)
            } catch (t: Throwable) {
                return cacheResult(
                    Action.LOAD,
                    id,
                    DetailedResult(
                        status = FeatureProcessResult.FAILED,
                        phase = Phase.INSTANTIATE,
                        reason = "在加载功能模块 $id 时失败：创建实例失败",
                        cause = t,
                    ),
                )
            }

            try {
                feature.ensureCoroutineScopeActive()
                feature.onLoad()
                feature.updateState(State.LOADED)
                _loadedFeatures[id] = feature
            } catch (t: Throwable) {
                feature.coroutineScope.cancel(FeatureCancellationException(feature.id))
                return cacheResult(
                    Action.LOAD,
                    id,
                    DetailedResult(
                        status = FeatureProcessResult.FAILED,
                        phase = Phase.LOAD,
                        reason = "在加载功能模块 $id 时失败",
                        cause = t,
                    ),
                )
            }

            meta.dependencies.forEach { dependency ->
                if (dependency.load != Load.AFTER) {
                    return@forEach
                }

                if (!dependency.required && getMetadata(dependency.id) == null) {
                    logger.info("加载功能模块 $id 时跳过可选 AFTER 依赖 ${dependency.id}：依赖元数据不存在")
                    return@forEach
                }

                val dependencyResult = loadFeatureDetailed(dependency.id)
                if (dependency.required && dependencyResult.status != FeatureProcessResult.SUCCESS) {
                    logger.info("功能模块 $id 已完成加载，但必需 AFTER 依赖 ${dependency.id} 未加载成功：${dependencyResult.reason}")
                    return@forEach
                }
                if (!dependency.required && dependencyResult.status != FeatureProcessResult.SUCCESS) {
                    logger.info("加载功能模块 $id 时跳过可选 AFTER 依赖 ${dependency.id}：${dependencyResult.reason}")
                }
            }

            return cacheResult(Action.LOAD, id, successResult(Action.LOAD, id))
        } finally {
            loadingFeatures.remove(id)
        }
    }

    private fun enableFeatureDetailed(id: String): DetailedResult {
        if (isEnabled(id)) {
            return successResult(Action.ENABLE, id)
        }

        enableResults[id]?.let { cached ->
            return cached
        }

        invalidManifestResult(id)?.let {
            return cacheResult(Action.ENABLE, id, it)
        }

        if (!isLoaded(id)) {
            val loadResult = loadFeatureDetailed(id)
            if (loadResult.status != FeatureProcessResult.SUCCESS) {
                return cacheResult(Action.ENABLE, id, dependencySkippedResult(id, id, Action.ENABLE, loadResult))
            }
        }

        val meta = getMetadata(id)
            ?: return cacheResult(
                Action.ENABLE,
                id,
                DetailedResult(
                    status = FeatureProcessResult.SKIPPED,
                    phase = Phase.MANIFEST,
                    reason = "未找到功能模块元数据",
                ),
            )

        meta.dependencies
            .filter { it.required && it.load == Load.AFTER }
            .forEach { dependency ->
                val dependencyLoadResult = loadFeatureDetailed(dependency.id)
                if (dependencyLoadResult.status != FeatureProcessResult.SUCCESS) {
                    return cacheResult(
                        Action.ENABLE,
                        id,
                        dependencySkippedResult(id, dependency.id, Action.ENABLE, dependencyLoadResult),
                    )
                }
            }

        if (!enablingFeatures.add(id)) {
            return cacheResult(
                Action.ENABLE,
                id,
                DetailedResult(
                    status = FeatureProcessResult.SKIPPED,
                    phase = Phase.DEPENDENCY,
                    reason = "检测到递归启用，已跳过",
                ),
            )
        }

        try {
            try {
                meta.dependencies.forEach { dependency ->
                    enableDependency(meta, dependency, Load.BEFORE)
                }
            } catch (e: DependencyNotReadyException) {
                return cacheResult(
                    Action.ENABLE,
                    id,
                    dependencySkippedResult(id, e.dependencyId, Action.ENABLE, e.result)
                )
            }

            val instance = getFeature(id) as? AbstractFeature
                ?: return cacheResult(
                    Action.ENABLE,
                    id,
                    DetailedResult(
                        status = FeatureProcessResult.SKIPPED,
                        phase = Phase.ENABLE,
                        reason = "功能模块尚未成功加载，无法启用",
                    ),
                )

            try {
                instance.ensureCoroutineScopeActive()
                instance.onEnable()
                instance.updateState(State.ENABLED)
            } catch (t: Throwable) {
                instance.coroutineScope.cancel(FeatureCancellationException(instance.id))
                return cacheResult(
                    Action.ENABLE,
                    id,
                    DetailedResult(
                        status = FeatureProcessResult.FAILED,
                        phase = Phase.ENABLE,
                        reason = "在启用功能模块 $id 时失败",
                        cause = t,
                    ),
                )
            }

            meta.dependencies.forEach { dependency ->
                if (dependency.load != Load.AFTER) {
                    return@forEach
                }

                if (!dependency.required && getMetadata(dependency.id) == null) {
                    logger.info("启用功能模块 $id 时跳过可选 AFTER 依赖 ${dependency.id}：依赖元数据不存在")
                    return@forEach
                }

                val dependencyResult = enableFeatureDetailed(dependency.id)
                if (dependency.required && dependencyResult.status != FeatureProcessResult.SUCCESS) {
                    logger.info("功能模块 $id 已完成启用，但必需 AFTER 依赖 ${dependency.id} 未启用成功：${dependencyResult.reason}")
                    return@forEach
                }
                if (!dependency.required && dependencyResult.status != FeatureProcessResult.SUCCESS) {
                    logger.info("启用功能模块 $id 时跳过可选 AFTER 依赖 ${dependency.id}：${dependencyResult.reason}")
                }
            }

            return cacheResult(Action.ENABLE, id, successResult(Action.ENABLE, id))
        } finally {
            enablingFeatures.remove(id)
        }
    }

    private fun reloadFeatureDetailed(id: String): DetailedResult {
        if (!isEnabled(id)) {
            return cacheResult(
                Action.RELOAD,
                id,
                DetailedResult(
                    status = FeatureProcessResult.SKIPPED,
                    phase = Phase.RELOAD,
                    reason = "功能模块未启用，无法重载",
                ),
            )
        }

        val instance = getFeature(id)
            ?: return cacheResult(
                Action.RELOAD,
                id,
                DetailedResult(
                    status = FeatureProcessResult.SKIPPED,
                    phase = Phase.RELOAD,
                    reason = "功能模块未加载，无法重载",
                ),
            )

        return try {
            instance.onReload()
            cacheResult(Action.RELOAD, id, successResult(Action.RELOAD, id))
        } catch (t: Throwable) {
            cacheResult(
                Action.RELOAD,
                id,
                DetailedResult(
                    status = FeatureProcessResult.FAILED,
                    phase = Phase.RELOAD,
                    reason = "在重载功能模块 $id 时失败",
                    cause = t,
                ),
            )
        }
    }

    private fun disableFeatureDetailed(id: String, cascading: Boolean): DetailedResult {
        if (isDisabled(id)) {
            return successResult(Action.DISABLE, id)
        }

        if (!isEnabled(id)) {
            return cacheResult(
                Action.DISABLE,
                id,
                DetailedResult(
                    status = FeatureProcessResult.SKIPPED,
                    phase = Phase.DISABLE,
                    reason = "功能模块未启用，无法卸载",
                ),
            )
        }

        if (cascading) {
            val disableOrder = dependencyGraph.getDisableOrderFor(id)
            val blockers = disableOrder
                .filter { it.id != id }
                .map { dependent -> dependent.id to disableFeatureDetailed(dependent.id, cascading = false) }
                .filter { (_, result) -> result.status == FeatureProcessResult.FAILED }

            if (blockers.isNotEmpty()) {
                val blockerIds = blockers.joinToString(", ") { it.first }
                return cacheResult(
                    Action.DISABLE,
                    id,
                    DetailedResult(
                        status = FeatureProcessResult.SKIPPED,
                        phase = Phase.DEPENDENCY,
                        reason = "依赖它的功能模块未成功卸载：$blockerIds",
                    ),
                )
            }
        }

        val instance = getFeature(id) as? AbstractFeature
            ?: return cacheResult(
                Action.DISABLE,
                id,
                DetailedResult(
                    status = FeatureProcessResult.SKIPPED,
                    phase = Phase.DISABLE,
                    reason = "功能模块未成功加载，无法卸载",
                ),
            )

        return try {
            instance.onDisable()
            instance.cancelCoroutineScope()
            instance.closeKoinApplication()
            instance.updateState(State.DISABLED)
            cacheResult(Action.DISABLE, id, successResult(Action.DISABLE, id))
        } catch (t: Throwable) {
            cacheResult(
                Action.DISABLE,
                id,
                DetailedResult(
                    status = FeatureProcessResult.FAILED,
                    phase = Phase.DISABLE,
                    reason = "在卸载功能模块 $id 时失败",
                    cause = t,
                ),
            )
        }
    }

    override fun loadFeature(id: String): FeatureProcessResult {
        resetOperationResults()
        return loadFeatureDetailed(id).status
    }

    override fun enableFeature(id: String): FeatureProcessResult {
        resetOperationResults()
        return enableFeatureDetailed(id).status
    }

    override fun reloadFeature(id: String): FeatureProcessResult {
        resetOperationResults()
        return reloadFeatureDetailed(id).status
    }

    override fun disableFeature(id: String): FeatureProcessResult {
        resetOperationResults()
        return disableFeatureDetailed(id, cascading = true).status
    }

    override fun loadFeatures(vararg ids: String): Map<String, FeatureProcessResult> {
        resetOperationResults()
        val results = LinkedHashMap<String, FeatureProcessResult>()
        ids.forEach { id ->
            results[id] = loadFeatureDetailed(id).status
        }
        summarize(Action.LOAD, results)
        return results
    }

    override fun enableFeatures(vararg ids: String): Map<String, FeatureProcessResult> {
        resetOperationResults()
        val results = LinkedHashMap<String, FeatureProcessResult>()
        ids.forEach { id ->
            results[id] = enableFeatureDetailed(id).status
        }
        summarize(Action.ENABLE, results)
        return results
    }

    override fun reloadFeatures(vararg ids: String): Map<String, FeatureProcessResult> {
        resetOperationResults()
        val results = LinkedHashMap<String, FeatureProcessResult>()
        ids.forEach { id ->
            results[id] = reloadFeatureDetailed(id).status
        }
        summarize(Action.RELOAD, results)
        return results
    }

    override fun disableFeatures(vararg ids: String): Map<String, FeatureProcessResult> {
        resetOperationResults()
        val results = LinkedHashMap<String, FeatureProcessResult>()
        ids.forEach { id ->
            results[id] = disableFeatureDetailed(id, cascading = true).status
        }
        summarize(Action.DISABLE, results)
        return results
    }

    override fun loadAll(): Map<String, FeatureProcessResult> {
        return loadFeatures(*config.autoLoad.toTypedArray())
    }

    override fun enableAll(): Map<String, FeatureProcessResult> {
        val results = LinkedHashMap<String, FeatureProcessResult>()
        dependencyGraph.getEnableOrder().forEach { featureMetadata ->
            if (isLoaded(featureMetadata.id) && !isEnabled(featureMetadata.id)) {
                results[featureMetadata.id] = enableFeatureDetailed(featureMetadata.id).status
            }
        }
        summarize(Action.ENABLE, results)
        return results
    }

    override fun reloadAll(): Map<String, FeatureProcessResult> {
        return reloadFeatures(*enabledFeatures.map { it.id }.toTypedArray())
    }

    override fun disableAll(): Map<String, FeatureProcessResult> {
        val results = LinkedHashMap<String, FeatureProcessResult>()
        dependencyGraph.getDisableOrder().forEach { featureMetadata ->
            if (isEnabled(featureMetadata.id)) {
                results[featureMetadata.id] = disableFeatureDetailed(featureMetadata.id, cascading = false).status
            }
        }
        summarize(Action.DISABLE, results)
        return results
    }

    override fun getFeature(id: String): Feature? = _loadedFeatures[id]

    override fun isEnabledInConfig(id: String): Boolean {
        return id in config.enabled || id in config.autoLoad
    }

    override fun isLoaded(id: String): Boolean {
        val feature = _loadedFeatures[id] ?: return false
        return feature.state >= State.LOADED
    }

    override fun isEnabled(id: String): Boolean {
        val feature = _loadedFeatures[id] ?: return false
        return feature.state == State.ENABLED
    }

    override fun isDisabled(id: String): Boolean {
        val feature = _loadedFeatures[id] ?: return false
        return feature.state == State.DISABLED
    }

    private enum class Action {
        LOAD,
        ENABLE,
        RELOAD,
        DISABLE,
    }

    private enum class Phase {
        NONE,
        MANIFEST,
        CONFIG,
        DEPENDENCY,
        INSTANTIATE,
        LOAD,
        ENABLE,
        RELOAD,
        DISABLE,
    }

    private data class DetailedResult(
        val status: FeatureProcessResult,
        val phase: Phase,
        val reason: String,
        val cause: Throwable? = null,
    )

    private class DependencyNotReadyException(
        val dependencyId: String,
        val result: DetailedResult,
    ) : RuntimeException()
}
