package plutoproject.feature.gallery.infra.mongo.model

import kotlinx.serialization.Serializable

@Serializable
enum class ItemFrameFacingDocument {
    NORTH,
    SOUTH,
    EAST,
    WEST,
    UP,
    DOWN,
}
