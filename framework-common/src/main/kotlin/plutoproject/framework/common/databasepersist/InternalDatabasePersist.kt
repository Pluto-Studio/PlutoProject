package plutoproject.framework.common.databasepersist

import plutoproject.framework.common.api.databasepersist.DatabasePersist
import plutoproject.framework.common.api.databasepersist.PersistContainer
import java.time.Instant

interface InternalDatabasePersist : DatabasePersist {
    fun getLastUsedTimestamp(container: PersistContainer): Instant

    fun setUsed(container: PersistContainer)

    fun close()
}
