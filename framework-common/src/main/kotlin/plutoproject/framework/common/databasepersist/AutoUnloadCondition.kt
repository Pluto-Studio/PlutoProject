package plutoproject.framework.common.databasepersist

import java.util.*

interface AutoUnloadCondition {
    fun shouldUnload(playerId: UUID): Boolean
}
