package plutoproject.capability.databasepersist.common

import plutoproject.capability.databasepersist.api.DatabasePersist
import java.time.Instant

interface InternalDatabasePersist : DatabasePersist {
    fun getLastUsedTimestamp(container: InternalPersistContainer): Instant

    fun setUsed(container: InternalPersistContainer)

    fun removeLoadedContainer(container: InternalPersistContainer)
}
