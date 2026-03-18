package plutoproject.feature.gallery.core.display

import java.util.UUID

interface MapUpdatePort {
    fun send(playerId: UUID, update: MapUpdate)
}
