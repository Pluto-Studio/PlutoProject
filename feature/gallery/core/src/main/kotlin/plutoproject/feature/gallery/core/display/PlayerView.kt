package plutoproject.feature.gallery.core.display

import java.util.UUID

data class PlayerView(
    val id: UUID,
    val eye: Vec3,
    val viewDirection: Vec3
)
