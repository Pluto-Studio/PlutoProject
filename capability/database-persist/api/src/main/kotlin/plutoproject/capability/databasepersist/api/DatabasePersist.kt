package plutoproject.capability.databasepersist.api

import java.util.*

interface DatabasePersist {
    fun getContainer(playerId: UUID): PersistContainer

    fun unloadContainer(playerId: UUID): Boolean
}
