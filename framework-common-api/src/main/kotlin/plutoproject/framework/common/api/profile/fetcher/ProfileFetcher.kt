package plutoproject.framework.common.api.profile.fetcher

import java.util.*

@Suppress("UNUSED")
interface ProfileFetcher {
    val id: String

    suspend fun fetchByUuid(uuid: UUID): FetchedData?

    suspend fun fetchByName(name: String): FetchedData?

    suspend fun validate(uuid: UUID, name: String): Boolean

    suspend fun validate(uuid: String, name: String): Boolean
}
