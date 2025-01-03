package plutoproject.framework.common.playerdb

import java.util.*

interface DatabaseNotifier {
    suspend fun notify(id: UUID)
}
