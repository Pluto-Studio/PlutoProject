package plutoproject.framework.common.feature

import kotlinx.serialization.json.Json
import okio.buffer
import okio.source
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import plutoproject.framework.common.api.feature.*
import plutoproject.framework.common.api.feature.metadata.AbstractFeature
import plutoproject.framework.common.PlutoConfig
import plutoproject.framework.common.util.featureDataFolder
import plutoproject.framework.common.util.jvm.findClass
import plutoproject.framework.common.util.jvm.getResourceAsStreamFromJar
import plutoproject.framework.common.util.platformType
import java.io.File
import java.util.*
import java.util.logging.Logger
import kotlin.reflect.full.createInstance

class FeatureManagerImpl : FeatureManager, KoinComponent {
    private val config by lazy { get<PlutoConfig>().feature }
    private val _loadedFeatures = mutableMapOf<String, Feature>()
    private val metadata = readManifest()

    override val loadedFeatures: Collection<Feature>
        get() = _loadedFeatures.values
    override val enabledFeatures: Collection<Feature>
        get() = loadedFeatures.filter { it.state == State.ENABLED }
    override val disabledFeatures: Collection<Feature>
        get() = loadedFeatures.filter { it.state == State.DISABLED }

    private fun readManifest(): Map<String, FeatureMetadata> {
        val fileName = "${platformType.identifier}-features.json"
        val inputStream = getResourceAsStreamFromJar(fileName) ?: return emptyMap()
        val content = inputStream.source().buffer().readUtf8()
        val features = Json.decodeFromString<List<FeatureMetadata>>(content)
        return features.associateBy { it.id }
    }

    override fun getMetadata(id: String): FeatureMetadata? = metadata[id]

    private fun getMetadataOrThrow(id: String) = getMetadata(id) ?: error("Feature metadata not found: $id")

    private fun createInstance(metadata: FeatureMetadata): Feature {
        val entrypoint = metadata.entrypoint
        val entryClass = findClass(entrypoint)?.kotlin ?: error("Unable to find feature class: $entrypoint")
        val feature = entryClass.createInstance() as AbstractFeature
        val id = metadata.id
        feature.init(
            id,
            Logger.getLogger("feature/${platformType.identifier}/$id"),
            featureDataFolder.resolve("$id${File.separator}")
        )
        return feature
    }

    private fun FeatureMetadata.checkDependencies() =
        dependencies.filter { it.required }.forEach { getMetadataOrThrow(it.id) }

    private fun FeatureMetadata.loadDependencies(stage: Load) {
        when (stage) {
            Load.BEFORE -> dependencies.filter { it.load == Load.BEFORE }
            Load.AFTER -> dependencies.filter { it.load == Load.AFTER }
        }.forEach {
            if (!it.required && getMetadata(it.id) == null) {
                return@forEach
            }
            loadFeature(it.id)
        }
    }

    private fun FeatureMetadata.enableDependencies(stage: Load) {
        when (stage) {
            Load.BEFORE -> dependencies.filter { it.load == Load.BEFORE }
            Load.AFTER -> dependencies.filter { it.load == Load.AFTER }
        }.forEach {
            if (!it.required && getMetadata(it.id) == null) {
                return@forEach
            }
            enableFeature(it.id)
        }
    }

    private fun FeatureMetadata.disableDependencies() {
        metadata.values
            .filter { it.dependencies.any { dep -> dep.id == this.id } }
            .forEach {
                if (getMetadata(it.id) == null) {
                    return@forEach
                }
                disableFeature(it.id)
            }
    }

    // TODO: 使用 Graph 实现依赖关系
    private val loadingStack = Stack<String>()

    override fun loadFeature(id: String): Feature {
        if (isLoaded(id)) return getFeature(id) ?: error("Unexpected")
        check(isEnabledInConfig(id)) { "Feature $id not enabled in config" }

        val metadata = getMetadataOrThrow(id)

        if (id in loadingStack) {
            val circularPath = loadingStack.joinToString(" -> ") { it }
            throw IllegalStateException("Circular dependency detected: $circularPath -> $id")
        }
        loadingStack.push(id)
        metadata.checkDependencies()
        metadata.loadDependencies(Load.BEFORE)
        val instance = createInstance(metadata).apply {
            this as AbstractFeature
            onLoad()
            updateState(State.LOADED)
        }
        metadata.loadDependencies(Load.AFTER)

        logger.info("Loaded $id")
        _loadedFeatures[id] = instance
        loadingStack.pop()
        return instance
    }

    override fun enableFeature(id: String): Feature {
        if (isEnabled(id)) return getFeature(id) ?: error("Unexpected")
        check(isLoaded(id)) { "Feature $id must be loaded before enable" }

        val metadata = getMetadataOrThrow(id)
        metadata.checkDependencies()
        metadata.enableDependencies(Load.BEFORE)
        val instance = getFeature(metadata.id)!!.apply {
            this as AbstractFeature
            onEnable()
            updateState(State.ENABLED)
        }
        metadata.enableDependencies(Load.AFTER)

        logger.info("Enabled $id")
        return instance
    }

    override fun reloadFeature(id: String): Feature {
        check(isEnabled(id)) { "Feature $id must be enabled before reload" }
        val instance = getFeature(id)!!.apply { onReload() }
        logger.info("Reloaded $id")
        return instance
    }

    override fun disableFeature(id: String): Feature {
        if (isDisabled(id)) return getFeature(id) ?: error("Unexpected")
        check(isEnabled(id)) { "Feature $id must be enabled before disable" }

        val metadata = getMetadataOrThrow(id)
        metadata.checkDependencies()
        metadata.disableDependencies()
        val instance = getFeature(metadata.id)!!.apply {
            this as AbstractFeature
            onDisable()
            updateState(State.DISABLED)
        }

        logger.info("Disabled $id")
        return instance
    }

    override fun loadFeatures(vararg ids: String) = ids.forEach { loadFeature(it) }

    override fun enableFeatures(vararg ids: String) = ids.forEach { enableFeature(it) }

    override fun reloadFeatures(vararg ids: String) = ids.forEach { reloadFeature(it) }

    override fun disableFeatures(vararg ids: String) = ids.forEach { disableFeature(it) }

    override fun loadAll() = loadFeatures(*config.autoLoad.toTypedArray())

    override fun enableAll() = enableFeatures(*loadedFeatures.map { it.id }.toTypedArray())

    override fun reloadAll() = reloadFeatures(*enabledFeatures.map { it.id }.toTypedArray())

    override fun disableAll() = disableFeatures(*enabledFeatures.map { it.id }.toTypedArray())

    override fun getFeature(id: String) = _loadedFeatures[id]

    override fun isEnabledInConfig(id: String): Boolean = id in config.enabled || id in config.autoLoad

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
