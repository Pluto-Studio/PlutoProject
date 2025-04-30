package plutoproject.framework.common.api.profile

import plutoproject.framework.common.util.inject.Koin
import java.util.*

interface ProfileLookup {
    companion object : ProfileLookup by Koin.get()

    suspend fun lookupByUuid(uuid: UUID, requestApi: Boolean = true): Profile?

    suspend fun lookupByName(name: String, requestApi: Boolean = true): Profile?

    suspend fun validate(uuid: UUID, name: String, requestApi: Boolean = true): Boolean
}
