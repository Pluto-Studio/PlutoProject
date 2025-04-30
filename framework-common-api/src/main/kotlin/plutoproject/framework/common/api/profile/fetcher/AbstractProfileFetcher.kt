package plutoproject.framework.common.api.profile.fetcher

import okhttp3.OkHttpClient
import java.util.*

@Suppress("UNUSED")
abstract class AbstractProfileFetcher : ProfileFetcher {
    internal val httpClient = OkHttpClient()

    override suspend fun validate(uuid: UUID, name: String): Boolean {
        val fetched = fetchByName(name)
        return fetched != null && fetched.uuid == uuid
    }

    override suspend fun validate(uuid: String, name: String): Boolean =
        validate(UUID.fromString(uuid), name)
}
