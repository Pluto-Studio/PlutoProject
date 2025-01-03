package plutoproject.framework.common.api.feature

interface FeatureManager {
    val manifestMetadata: List<FeatureMetadata>
    val loadedFeatures: List<Feature<*, *>>

    fun getMetadataFromManifest(id: String): FeatureMetadata?

    fun isFeatureLoaded(id: String): Boolean

    fun isFeatureLoaded(metadata: FeatureManager): Boolean

    fun loadFeature(metadata: FeatureMetadata): Feature<*, *>

    fun loadFeatures(metadata: Iterable<FeatureManager>)

    fun enableFeature(metadata: FeatureMetadata): Feature<*, *>

    fun reloadFeature(metadata: FeatureMetadata): Feature<*, *>

    fun disableFeature(metadata: FeatureMetadata): Feature<*, *>
}
