package plutoproject.framework.velocity.databasepersist

import plutoproject.framework.common.databasepersist.AutoUnloadCondition
import plutoproject.framework.velocity.util.server
import java.util.*

class AutoUnloadConditionImpl : AutoUnloadCondition {
    override fun shouldUnload(playerId: UUID): Boolean {
        return server.getPlayer(playerId).isEmpty
    }
}
