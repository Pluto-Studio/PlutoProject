package plutoproject.capability.profile.api

import java.util.*

interface ProfileLookup {
    suspend fun lookupByUuid(uuid: UUID, requestApi: Boolean = true): Profile?

    suspend fun lookupByName(name: String, requestApi: Boolean = true): Profile?

    suspend fun validate(uuid: UUID, name: String, requestApi: Boolean = true): Boolean
}
