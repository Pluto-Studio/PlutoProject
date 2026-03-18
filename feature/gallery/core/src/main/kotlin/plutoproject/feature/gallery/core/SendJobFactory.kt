package plutoproject.feature.gallery.core

import java.util.UUID

interface SendJobFactory {
    fun create(playerId: UUID): SendJob
}
