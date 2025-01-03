package plutoproject.framework.common.feature

import com.google.common.graph.GraphBuilder
import plutoproject.framework.common.api.feature.Feature
import plutoproject.framework.common.api.feature.FeatureManager
import plutoproject.framework.common.api.feature.FeatureMetadata
import plutoproject.framework.common.api.feature.State
import plutoproject.framework.common.api.feature.metadata.AbstractFeature
import java.util.*

abstract class CommonFeatureManager : FeatureManager {
    private val graph = GraphBuilder.directed().build<String>()
    private val loadOrder = mutableListOf<String>()
    private val _manifestMetadata = mutableListOf<FeatureMetadata>()
    private val _loadedFeatures = mutableListOf<Feature<*, *>>()
    private var hasCircular: Boolean = false

    override val manifestMetadata: List<FeatureMetadata> get() = _manifestMetadata
    override val loadedFeatures: List<Feature<*, *>> get() = _loadedFeatures

    abstract fun createAndInitFeature(metadata: FeatureMetadata): AbstractFeature<*, *>

    private fun updateLoadOrder(feature: List<FeatureMetadata>) {
        feature.forEach { feature ->
            graph.addNode(feature.id)
            feature.dependencies.forEach { dependency ->
                if (!graph.hasEdgeConnecting(dependency.id, feature.id)) {
                    graph.putEdge(dependency.id, feature.id)
                }
            }
        }

        val loadOrder = mutableListOf<String>()
        val path = Stack<String>()
        val visited = mutableListOf<String>()

        fun dfs(id: String) {
            if (path.contains(id)) {
                throwCircularException(path + id)
                return
            }
            if (visited.contains(id)) {
                return
            }

            path.push(id)
            visited.add(id)

            graph.predecessors(id).forEach { predecessors ->
                dfs(predecessors)
            }

            path.pop()
            loadOrder.add(id)
        }

        graph.nodes().forEach { id ->
            if (!visited.contains(id)) {
                dfs(id)
            }
        }

        this.loadOrder.clear()
        this.loadOrder.addAll(loadOrder)
    }

    private fun throwCircularException(cycle: List<String>) {
        val path = buildString {
            cycle.forEachIndexed { i, id ->
                append(cycle)
                if (i != cycle.lastIndex) {
                    append(" -> ")
                }
            }
        }
        logger.error("Circular feature dependency detected: $path")
        hasCircular = true
    }

    override fun getMetadataFromManifest(id: String) = _manifestMetadata.find { it.id == id }

    override fun getFeatureState(id: String): State? = getFeature(id)?.state

    override fun getFeatureState(metadata: FeatureMetadata): State? = getFeature(metadata)?.state

    override fun getFeature(id: String) = _loadedFeatures.find { it.id == id }

    override fun getFeature(metadata: FeatureMetadata) = getFeature(metadata.id)

    override fun loadFeature(metadata: FeatureMetadata) = loadFeatures(listOf(metadata))

    override fun loadFeatures(metadata: Iterable<FeatureMetadata>) {
        updateLoadOrder(metadata.toList())
        val before = _loadedFeatures.size
        loadOrder.forEach {
            val stage = getFeatureState(it)?.stage ?: 0
            if (stage >= State.LOADED.stage) return@forEach
            val metadata = getMetadataFromManifest(it) ?: error("Metadata of feature $it not found")
            val feature = createAndInitFeature(metadata)
            feature.onLoad()
            logger.info("Loaded $it")
            _loadedFeatures.add(feature)
        }
        logger.info("Loaded ${_loadedFeatures.size - before} feature(s)")
    }

    override fun enableFeature(metadata: FeatureMetadata) {
        if (hasCircular) return
        check(getFeatureState(metadata) == State.LOADED) { "Feature ${metadata.id} state must be: LOADED" }
    }

    override fun reloadFeature(metadata: FeatureMetadata) {
        if (hasCircular) return
        TODO("Not yet implemented")
    }

    override fun disableFeature(metadata: FeatureMetadata) {
        if (hasCircular) return
        check(getFeatureState(metadata) == State.ENABLED) { "Feature ${metadata.id} state must be: ENABLED" }
    }
}
