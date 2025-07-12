package plutoproject.framework.paper.databasepersist

import plutoproject.framework.common.databasepersist.AutoUnloadCondition
import plutoproject.framework.paper.util.server
import java.util.*

class AutoUnloadConditionImpl : AutoUnloadCondition {
    override fun shouldUnload(playerId: UUID): Boolean {
        return server.getPlayer(playerId) == null
    }
}
