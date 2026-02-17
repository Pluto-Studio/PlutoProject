package plutoproject.framework.common.feature

import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import plutoproject.framework.common.PlutoConfig
import plutoproject.framework.common.api.feature.*
import plutoproject.framework.common.api.feature.metadata.AbstractFeature
import plutoproject.framework.common.feature.dependency.FeatureDependencyGraph
import plutoproject.framework.common.util.featureDataFolder
import plutoproject.framework.common.util.jvm.findClass
import plutoproject.framework.common.util.platformType
import java.io.File
import java.util.logging.Logger
import kotlin.reflect.full.createInstance

/**
 * FeatureManager 的 V2 实现
 * 
 * 改进点：
 * 1. 使用图算法替代 Stack 进行依赖管理和循环检测
 * 2. 启动时预检测所有循环依赖，避免运行时错误
 * 3. 使用拓扑排序确保正确的加载/启用/禁用顺序
 * 4. 统一的错误处理和日志记录
 * 5. 消除重复代码，提高可维护性
 */
class FeatureManagerImplV2 : FeatureManager, KoinComponent {
    private val config by lazy { get<PlutoConfig>().feature }
    private val _loadedFeatures = mutableMapOf<String, Feature>()
    private val metadata = readManifest()
    private val dependencyGraph by lazy { FeatureDependencyGraph(metadata, logger) }

    override val loadedFeatures: Collection<Feature>
        get() = _loadedFeatures.values

    override val enabledFeatures: Collection<Feature>
        get() = loadedFeatures.filter { it.state == State.ENABLED }

    override val disabledFeatures: Collection<Feature>
        get() = loadedFeatures.filter { it.state == State.DISABLED }

    /**
     * 从 JAR 中读取 feature 清单文件
     */
    private fun readManifest(): Map<String, FeatureMetadata> {
        return FeatureManifestLoader.load(platformType)
    }

    override fun getMetadata(id: String): FeatureMetadata? = metadata[id]

    private fun getMetadataOrThrow(id: String): FeatureMetadata {
        return getMetadata(id) ?: error("未找到 Feature 元数据：$id")
    }

    /**
     * 创建 Feature 实例
     */
    private fun createInstance(metadata: FeatureMetadata): Feature {
        val entrypoint = metadata.entrypoint
        val entryClass = findClass(entrypoint)?.kotlin 
            ?: error("无法找到 Feature 类：$entrypoint")
        val feature = entryClass.createInstance() as AbstractFeature
        val id = metadata.id
        feature.init(
            id,
            Logger.getLogger("PlutoProject/$id"),
            featureDataFolder.resolve("$id${File.separator}")
        )
        return feature
    }

    /**
     * 验证 feature 依赖是否完整
     */
    private fun validateDependencies(metadata: FeatureMetadata) {
        val missing = dependencyGraph.validateDependencies(metadata.id)
        if (missing.isNotEmpty()) {
            error("Feature ${metadata.id} 缺少必需的依赖：${missing.joinToString(", ")}")
        }
    }

    /**
     * 加载指定 feature 的依赖（BEFORE 类型）
     */
    private fun loadBeforeDependencies(metadata: FeatureMetadata) {
        dependencyGraph.getBeforeDependencies(metadata.id).forEach { depId ->
            if (!isLoaded(depId)) {
                loadFeature(depId)
            }
        }
    }

    /**
     * 加载指定 feature 的依赖（AFTER 类型）
     */
    private fun loadAfterDependencies(metadata: FeatureMetadata) {
        dependencyGraph.getAfterDependencies(metadata.id).forEach { depId ->
            if (!isLoaded(depId)) {
                loadFeature(depId)
            }
        }
    }

    /**
     * 启用指定 feature 的依赖（BEFORE 类型）
     */
    private fun enableBeforeDependencies(metadata: FeatureMetadata) {
        dependencyGraph.getBeforeDependencies(metadata.id).forEach { depId ->
            if (!isEnabled(depId)) {
                enableFeature(depId)
            }
        }
    }

    /**
     * 启用指定 feature 的依赖（AFTER 类型）
     */
    private fun enableAfterDependencies(metadata: FeatureMetadata) {
        dependencyGraph.getAfterDependencies(metadata.id).forEach { depId ->
            if (!isEnabled(depId)) {
                enableFeature(depId)
            }
        }
    }

    override fun loadFeature(id: String): Feature {
        // 已加载则直接返回
        if (isLoaded(id)) {
            return getFeature(id) ?: error("Feature $id 标记为已加载但未找到")
        }

        // 检查是否在配置中启用
        check(isEnabledInConfig(id)) { "Feature $id 未在配置中启用" }

        // 检查是否因循环依赖被永久禁用
        if (dependencyGraph.isPermanentlyDisabled(id)) {
            error("Feature $id 因循环依赖被永久禁用")
        }

        val meta = getMetadataOrThrow(id)

        // 验证依赖完整性
        validateDependencies(meta)

        // 加载 BEFORE 依赖
        loadBeforeDependencies(meta)

        // 创建实例并加载
        val instance = createInstance(meta).apply {
            this as AbstractFeature
            onLoad()
            updateState(State.LOADED)
        }

        // 加载 AFTER 依赖
        loadAfterDependencies(meta)

        logger.info("已加载 Feature：$id")
        _loadedFeatures[id] = instance
        return instance
    }

    override fun enableFeature(id: String): Feature {
        // 已启用则直接返回
        if (isEnabled(id)) {
            return getFeature(id) ?: error("Feature $id 标记为已启用但未找到")
        }

        // 检查是否已加载
        check(isLoaded(id)) { "必须先加载 Feature $id 才能启用" }

        val meta = getMetadataOrThrow(id)

        // 验证依赖完整性
        validateDependencies(meta)

        // 启用 BEFORE 依赖
        enableBeforeDependencies(meta)

        // 启用当前 feature
        val instance = getFeature(meta.id)!!.apply {
            this as AbstractFeature
            onEnable()
            updateState(State.ENABLED)
        }

        // 启用 AFTER 依赖
        enableAfterDependencies(meta)

        logger.info("已启用 Feature：$id")
        return instance
    }

    override fun reloadFeature(id: String): Feature {
        check(isEnabled(id)) { "必须先启用 Feature $id 才能重载" }
        
        val instance = getFeature(id)!!.apply { onReload() }
        logger.info("已重载 Feature：$id")
        return instance
    }

    override fun disableFeature(id: String): Feature {
        // 已禁用则直接返回
        if (isDisabled(id)) {
            return getFeature(id) ?: error("Feature $id 标记为已禁用但未找到")
        }

        // 检查是否已启用
        check(isEnabled(id)) { "必须先启用 Feature $id 才能禁用" }

        val meta = getMetadataOrThrow(id)

        // 获取需要禁用的所有 features（包括依赖它的）
        val disableOrder = dependencyGraph.getDisableOrderFor(id)

        // 按照依赖顺序禁用
        disableOrder.forEach { metadata ->
            if (metadata.id == id || isEnabled(metadata.id)) {
                getFeature(metadata.id)?.apply {
                    this as AbstractFeature
                    onDisable()
                    updateState(State.DISABLED)
                }
                logger.info("已禁用 Feature：${metadata.id}")
            }
        }

        return getFeature(id)!!
    }

    override fun loadFeatures(vararg ids: String) {
        ids.forEach { loadFeature(it) }
    }

    override fun enableFeatures(vararg ids: String) {
        ids.forEach { enableFeature(it) }
    }

    override fun reloadFeatures(vararg ids: String) {
        ids.forEach { reloadFeature(it) }
    }

    override fun disableFeatures(vararg ids: String) {
        ids.forEach { disableFeature(it) }
    }

    override fun loadAll() {
        loadFeatures(*config.autoLoad.toTypedArray())
    }

    override fun enableAll() {
        // 使用拓扑排序确保正确的启用顺序
        val order = dependencyGraph.getEnableOrder()
        order.forEach { metadata ->
            if (isLoaded(metadata.id) && !isEnabled(metadata.id)) {
                enableFeature(metadata.id)
            }
        }
    }

    override fun reloadAll() {
        reloadFeatures(*enabledFeatures.map { it.id }.toTypedArray())
    }

    override fun disableAll() {
        // 使用反向拓扑排序确保正确的禁用顺序
        val order = dependencyGraph.getDisableOrder()
        order.forEach { metadata ->
            if (isEnabled(metadata.id)) {
                disableFeature(metadata.id)
            }
        }
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
}
