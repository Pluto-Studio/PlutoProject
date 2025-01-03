package plutoproject.framework.common.api.feature.metadata

import kotlinx.serialization.Serializable
import plutoproject.framework.common.api.feature.Load

@Serializable
data class DependencyMetadata(
    val id: String,
    val load: Load,
    val required: Boolean
)
