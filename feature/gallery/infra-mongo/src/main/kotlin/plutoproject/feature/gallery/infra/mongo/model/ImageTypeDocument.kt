package plutoproject.feature.gallery.infra.mongo.model

import kotlinx.serialization.Serializable

@Serializable
enum class ImageTypeDocument {
    STATIC,
    ANIMATED,
}
