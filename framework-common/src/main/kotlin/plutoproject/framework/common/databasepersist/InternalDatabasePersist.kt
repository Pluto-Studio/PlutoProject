package plutoproject.framework.common.databasepersist

import plutoproject.framework.common.api.databasepersist.DatabasePersist
import plutoproject.framework.common.api.databasepersist.PersistContainer
import java.time.Instant

interface InternalDatabasePersist : DatabasePersist {
    fun getLastUsedTimestamp(container: InternalPersistContainer): Instant

    fun setUsed(container: InternalPersistContainer)

    fun removeLoadedContainer(container: InternalPersistContainer)

    fun close()
}
