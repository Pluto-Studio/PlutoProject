package plutoproject.framework.common.api.profile

import okhttp3.OkHttpClient
import java.util.*

@Suppress("UNUSED")
abstract class AbstractProfileFetcher : ProfileFetcher {
    internal val httpClient = OkHttpClient()

    override suspend fun validate(name: String, uuid: UUID): Boolean {
        val fetched = fetch(name)
        return !(fetched == null || fetched.uuid != uuid)
    }

    override suspend fun validate(name: String, uuid: String): Boolean = validate(name, UUID.fromString(uuid))
}
