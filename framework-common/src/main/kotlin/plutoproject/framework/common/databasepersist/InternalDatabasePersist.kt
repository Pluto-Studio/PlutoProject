package plutoproject.framework.common.databasepersist

import plutoproject.framework.common.api.databasepersist.DatabasePersist

interface InternalDatabasePersist : DatabasePersist {
    fun close()
}
