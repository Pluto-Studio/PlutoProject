package plutoproject.framework.common.api.databasepersist

import plutoproject.framework.common.util.inject.Koin

interface DatabasePersist {
    companion object : DatabasePersist by Koin.get()
}
