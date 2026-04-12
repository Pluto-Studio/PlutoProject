package plutoproject.feature.gallery.infra.mongo.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MapIdSystemInformationDocument(
    @SerialName("_id") val id: String,
    val lastAllocatedId: Int,
)
