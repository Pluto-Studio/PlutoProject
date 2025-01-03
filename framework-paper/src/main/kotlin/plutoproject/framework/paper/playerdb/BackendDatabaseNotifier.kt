package plutoproject.framework.paper.playerdb

import org.koin.core.component.KoinComponent
import plutoproject.framework.common.playerdb.DatabaseNotifier
import java.util.*

class BackendDatabaseNotifier : DatabaseNotifier, KoinComponent {
    override suspend fun notify(id: UUID) {
        sendUpdateNotification(id)
    }
}
