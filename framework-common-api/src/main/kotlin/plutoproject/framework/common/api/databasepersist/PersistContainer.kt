package plutoproject.framework.common.api.databasepersist

import java.util.*

interface PersistContainer {
    val containerId: UUID
    val playerId: UUID

    suspend fun get()

    suspend fun save()
}
