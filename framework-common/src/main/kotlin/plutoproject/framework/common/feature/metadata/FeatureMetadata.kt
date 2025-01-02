package plutoproject.framework.common.feature.metadata

import kotlinx.serialization.Serializable
import plutoproject.framework.common.api.feature.annotation.Platform

@Serializable
data class FeatureMetadata(
    val id: String,
    val mainClass: String,
    val platform: Platform,
    val dependencies: List<DependencyMetadata>,
)
