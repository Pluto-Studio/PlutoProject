package plutoproject.feature.gallery.infra.mongo.model

import kotlinx.serialization.Serializable

@Serializable
data class MapIdSystemInformationDocument(
    val _id: String,
    val lastAllocatedId: Int,
)
