package plutoproject.framework.common.api.feature

import kotlinx.serialization.Serializable
import plutoproject.framework.common.api.feature.metadata.DependencyMetadata

@Serializable
data class FeatureMetadata(
    val id: String,
    val mainClass: String,
    val platform: Platform,
    val dependencies: List<DependencyMetadata>,
)
