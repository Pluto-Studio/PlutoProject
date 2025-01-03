package plutoproject.framework.common.api.feature

import plutoproject.framework.common.util.inject.inlinedGet

interface FeatureManager {
    companion object : FeatureManager by inlinedGet()

    val manifestMetadata: List<FeatureMetadata>
    val loadedFeatures: List<Feature<*, *>>

    fun getMetadataFromManifest(id: String): FeatureMetadata?

    fun getFeatureState(id: String): State?

    fun getFeatureState(metadata: FeatureMetadata): State?

    fun getFeature(id: String): Feature<*, *>?

    fun getFeature(metadata: FeatureMetadata): Feature<*, *>?

    fun loadFeature(metadata: FeatureMetadata)

    fun loadFeatures(metadata: Iterable<FeatureMetadata>)

    fun enableFeature(metadata: FeatureMetadata)

    fun reloadFeature(metadata: FeatureMetadata)

    fun disableFeature(metadata: FeatureMetadata)
}
