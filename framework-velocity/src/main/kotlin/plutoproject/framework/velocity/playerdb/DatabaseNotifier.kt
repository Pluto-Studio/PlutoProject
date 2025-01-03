package plutoproject.framework.velocity.playerdb

import plutoproject.framework.common.playerdb.DatabaseNotifier
import plutoproject.framework.velocity.playerdb.proto.PlayerDBRpc
import java.util.*

class DatabaseNotifier : DatabaseNotifier {
    override suspend fun notify(id: UUID) {
        PlayerDBRpc.notifyDatabaseUpdate(id)
    }
}
