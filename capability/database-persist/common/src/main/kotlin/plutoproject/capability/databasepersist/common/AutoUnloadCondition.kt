package plutoproject.capability.databasepersist.common

import java.util.*

interface AutoUnloadCondition {
    fun shouldUnload(playerId: UUID): Boolean
}
