package plutoproject.framework.common.databasepersist

import plutoproject.framework.common.api.databasepersist.PersistContainer

interface InternalPersistContainer : PersistContainer {
    fun close()
}
