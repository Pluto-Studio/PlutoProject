package plutoproject.framework.common.api.profile.fetcher

import kotlinx.serialization.json.Json
import okhttp3.Request
import plutoproject.framework.common.util.coroutine.withIO
import plutoproject.framework.common.util.data.toShortUuidString
import plutoproject.framework.common.util.network.MINECRAFT_SERVICE_API
import plutoproject.framework.common.util.network.MOJANG_API
import java.util.*

@Suppress("UNUSED")
object MojangProfileFetcher : AbstractProfileFetcher() {
    override val id: String = "mojang"

    override suspend fun fetchByUuid(uuid: UUID): FetchedData? =
        requestApi("${MINECRAFT_SERVICE_API}minecraft/profile/lookup/${uuid.toShortUuidString()}")

    override suspend fun fetchByName(name: String): FetchedData? =
        requestApi("${MOJANG_API}users/profiles/minecraft/${name.lowercase()}")

    private suspend fun requestApi(url: String): FetchedData? = withIO {
        val request = Request.Builder().url(url).build()
        val call = httpClient.newCall(request)
        val response = call.execute()
        val body = response.body
        runCatching { Json.decodeFromString<FetchedData>(body.string()) }.getOrNull()
    }
}
