package plutoproject.capability.databasepersist.common

import plutoproject.capability.databasepersist.api.PersistContainer

interface InternalPersistContainer : PersistContainer {
    fun close()
}
