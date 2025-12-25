package plutoproject.framework.common.api.feature

import plutoproject.framework.common.api.feature.FeatureManager.Companion.isEnabledInConfig
import plutoproject.framework.common.util.inject.Koin

/**
 * 用于管理环境中 Feature 的类，仅会存在一个实例。
 */
interface FeatureManager {
    /**
     * [FeatureManager] 实例访问入口。
     */
    companion object : FeatureManager by Koin.get()

    /**
     * 所有已加载的模块。
     */
    val loadedFeatures: Collection<Feature>

    /**
     * 所有已启用的模块。
     */
    val enabledFeatures: Collection<Feature>

    /**
     * 所有已关闭的模块。
     */
    val disabledFeatures: Collection<Feature>

    /**
     * 获取指定 ID 的 Feature 元数据。
     *
     * @return 指定 Feature 的 [FeatureMetadata]，若不存在则返回空
     */
    fun getMetadata(id: String): FeatureMetadata?

    /**
     * 查找指定 ID 的 Feature 元数据并加载。
     *
     * 该 Feature 必须在配置中启用，可通过 [isEnabledInConfig] 检查。
     *
     * @throws IllegalStateException 指定 Feature 的 ID 不存在、循环依赖或未在配置中启用。
     */
    fun loadFeature(id: String): Feature

    /**
     * 启用指定 ID 的 Feature，若已加载则跳过。
     *
     * @throws IllegalStateException 指定 ID 的 Feature 未加载或不存在。
     */
    fun enableFeature(id: String): Feature

    /**
     * 重载指定 ID 的 Feature。
     *
     * @throws IllegalStateException 指定 ID 的 Feature 未启用或不存在。
     */
    fun reloadFeature(id: String): Feature

    /**
     * 关闭指定 ID 的 Feature，若已关闭则跳过。
     *
     * @throws IllegalStateException 指定 ID 的 Feature 未启用或不存在。
     */
    fun disableFeature(id: String): Feature

    /**
     * 查找指定 ID 的 Feature 元数据并加载。
     *
     * 这些 Feature 必须在配置中启用，可通过 [isEnabledInConfig] 检查。
     *
     * @throws IllegalStateException 指定 Feature 的 ID 不存在、循环依赖或未在配置中启用。
     */
    fun loadFeatures(vararg ids: String)

    /**
     * 启用指定 ID 的 Feature，若已加载则跳过。
     *
     * @throws IllegalStateException 指定 ID 的 Feature 未加载或不存在。
     */
    fun enableFeatures(vararg ids: String)

    /**
     * 重载指定 ID 的 Feature。
     *
     * @throws IllegalStateException 指定 ID 的 Feature 未启用或不存在。
     */
    fun reloadFeatures(vararg ids: String)

    /**
     * 关闭指定 ID 的 Feature，若已关闭则跳过。
     *
     * @throws IllegalStateException 指定 ID 的 Feature 未启用或不存在。
     */
    fun disableFeatures(vararg ids: String)

    /**
     * 加载所有在配置文件中启用自动加载的模块。
     */
    fun loadAll()

    /**
     * 启用所有已加载的模块。
     */
    fun enableAll()

    /**
     * 重载所有已启用的模块。
     */
    fun reloadAll()

    /**
     * 关闭所有已启用的模块。
     */
    fun disableAll()

    /**
     * 获取指定 ID 的 Feature 实例。
     *
     * @return Feature 实例，若不存在则返回空
     */
    fun getFeature(id: String): Feature?

    /**
     * 获取指定 ID 的 Feature 是否在配置中启用。
     */
    fun isEnabledInConfig(id: String): Boolean

    /**
     * 检查指定 ID 的 Feature 是否已加载。
     */
    fun isLoaded(id: String): Boolean

    /**
     * 检查指定 ID 的 Feature 是否已启用。
     */
    fun isEnabled(id: String): Boolean

    /**
     * 检查指定 ID 的 Feature 是否已关闭。
     */
    fun isDisabled(id: String): Boolean
}
