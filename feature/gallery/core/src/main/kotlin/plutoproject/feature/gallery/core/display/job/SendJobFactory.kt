package plutoproject.feature.gallery.core.display.job

import java.util.UUID

interface SendJobFactory {
    fun create(playerId: UUID): SendJob
}
