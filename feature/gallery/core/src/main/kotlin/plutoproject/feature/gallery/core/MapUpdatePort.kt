package plutoproject.feature.gallery.core

import java.util.UUID

interface MapUpdatePort {
    fun send(playerId: UUID, update: MapUpdate)
}
