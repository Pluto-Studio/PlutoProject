package plutoproject.framework.common.feature.dependency

import plutoproject.framework.common.api.feature.FeatureMetadata
import plutoproject.framework.common.api.feature.Load
import java.util.logging.Logger

/**
 * Feature 依赖图管理类
 * 负责构建依赖关系图、检测循环依赖、计算加载/禁用顺序
 */
class FeatureDependencyGraph(
    private val metadata: Map<String, FeatureMetadata>,
    private val logger: Logger
) {
    // 依赖边：feature -> 它所依赖的其他 features
    private val dependencyEdges: Map<String, Set<String>>
    
    // BEFORE 依赖边：feature -> 需要在它之前加载的 features
    private val beforeEdges: Map<String, Set<String>>
    
    // AFTER 依赖边：feature -> 需要在它之后加载的 features
    private val afterEdges: Map<String, Set<String>>
    
    // 反向依赖边：feature -> 依赖它的其他 features（用于禁用）
    private val reverseEdges: Map<String, Set<String>>
    
    // 存在循环依赖被禁用的 features
    private val permanentlyDisabled = mutableSetOf<String>()
    
    init {
        // 构建所有边
        val depEdges = mutableMapOf<String, MutableSet<String>>()
        val befEdges = mutableMapOf<String, MutableSet<String>>()
        val aftEdges = mutableMapOf<String, MutableSet<String>>()
        val revEdges = mutableMapOf<String, MutableSet<String>>()
        
        metadata.keys.forEach { id ->
            depEdges[id] = mutableSetOf()
            befEdges[id] = mutableSetOf()
            aftEdges[id] = mutableSetOf()
            revEdges[id] = mutableSetOf()
        }
        
        metadata.values.forEach { meta ->
            meta.dependencies.forEach { dep ->
                if (dep.id in metadata) {
                    // 添加反向边
                    revEdges[dep.id]?.add(meta.id)
                    
                    // 根据 load 类型分类，不再重复添加到 depEdges
                    when (dep.load) {
                        Load.BEFORE -> befEdges[meta.id]?.add(dep.id)
                        Load.AFTER -> aftEdges[meta.id]?.add(dep.id)
                    }
                }
            }
        }
        
        dependencyEdges = depEdges
        beforeEdges = befEdges
        afterEdges = aftEdges
        reverseEdges = revEdges
        
        // 检测并处理循环依赖
        detectAndDisableCycles()
    }
    
    /**
     * 检测循环依赖并将相关 feature 标记为永久禁用
     */
    private fun detectAndDisableCycles() {
        val cycles = TopologicalSort.detectCycles(metadata.keys, dependencyEdges)
        
        cycles.forEach { cycle ->
            logger.severe("检测到循环依赖：${cycle.joinToString(" -> ")} -> ${cycle.first()}")
            permanentlyDisabled.addAll(cycle)
        }

        if (permanentlyDisabled.isNotEmpty()) {
            logger.severe("以下 Feature 因循环依赖被禁用：${permanentlyDisabled.joinToString(", ")}")
        }
    }
    
    /**
     * 获取所有可用的 feature IDs（排除永久禁用的）
     */
    fun getAvailableFeatures(): Set<String> {
        return metadata.keys - permanentlyDisabled
    }
    
    /**
     * 检查 feature 是否因循环依赖被永久禁用
     */
    fun isPermanentlyDisabled(id: String): Boolean {
        return id in permanentlyDisabled
    }
    
    /**
     * 获取 feature 的元数据
     */
    fun getMetadata(id: String): FeatureMetadata? {
        return metadata[id]
    }
    
    /**
     * 获取 feature 的所有依赖（包括 BEFORE 和 AFTER）
     */
    fun getDependencies(id: String): Set<String> {
        return dependencyEdges[id] ?: emptySet()
    }
    
    /**
     * 获取 feature 的 BEFORE 依赖
     */
    fun getBeforeDependencies(id: String): Set<String> {
        return beforeEdges[id] ?: emptySet()
    }
    
    /**
     * 获取 feature 的 AFTER 依赖
     */
    fun getAfterDependencies(id: String): Set<String> {
        return afterEdges[id] ?: emptySet()
    }
    
    /**
     * 获取依赖此 feature 的所有其他 features
     */
    fun getDependents(id: String): Set<String> {
        return reverseEdges[id] ?: emptySet()
    }
    
    /**
     * 计算加载顺序（拓扑排序）
     * 确保 BEFORE 依赖先加载，AFTER 依赖后加载
     */
    fun getLoadOrder(): List<FeatureMetadata> {
        val available = getAvailableFeatures()
        val order = TopologicalSort.sort(available, dependencyEdges, beforeEdges, afterEdges)
        
        return order.mapNotNull { metadata[it] }
    }
    
    /**
     * 计算启用顺序
     * 与加载顺序相同
     */
    fun getEnableOrder(): List<FeatureMetadata> {
        return getLoadOrder()
    }
    
    /**
     * 计算禁用顺序
     * 与加载顺序相反：先禁用依赖他人的 feature，后禁用被依赖的 feature
     */
    fun getDisableOrder(): List<FeatureMetadata> {
        // 直接使用加载顺序的反向
        return getLoadOrder().reversed()
    }
    
    /**
     * 获取指定 feature 的禁用顺序（包括所有依赖它的 features）
     */
    fun getDisableOrderFor(id: String): List<FeatureMetadata> {
        if (id in permanentlyDisabled) return emptyList()
        
        // 收集所有需要禁用的 features（依赖它的所有 features）
        val toDisable = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(id)
        
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current in toDisable || current in permanentlyDisabled) continue
            
            toDisable.add(current)
            reverseEdges[current]?.forEach { dependent ->
                if (dependent !in permanentlyDisabled) {
                    queue.add(dependent)
                }
            }
        }
        
        // 反向拓扑排序
        val order = TopologicalSort.reverseSort(toDisable, reverseEdges)
        return order.mapNotNull { metadata[it] }
    }
    
    /**
     * 验证指定 feature 的依赖是否都存在（排除可选依赖）
     */
    fun validateDependencies(id: String): List<String> {
        val meta = metadata[id] ?: return emptyList()
        val missing = mutableListOf<String>()
        
        meta.dependencies.forEach { dep ->
            if (dep.required && dep.id !in metadata) {
                missing.add(dep.id)
            }
        }
        
        return missing
    }
}
