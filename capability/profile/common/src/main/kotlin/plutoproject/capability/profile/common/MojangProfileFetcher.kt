package plutoproject.capability.profile.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.*

private const val MINECRAFT_SERVICE_API = "https://api.minecraftservices.com/"
private const val MOJANG_API = "https://api.mojang.com/"

class MojangProfileFetcher(private val httpClient: OkHttpClient = OkHttpClient()) {
    suspend fun fetchByUuid(uuid: UUID): FetchedProfile? =
        requestApi("${MINECRAFT_SERVICE_API}minecraft/profile/lookup/${uuid.toString().replace("-", "")}")

    suspend fun fetchByName(name: String): FetchedProfile? =
        requestApi("${MOJANG_API}users/profiles/minecraft/${name.lowercase()}")

    private suspend fun requestApi(url: String): FetchedProfile? {
        val request = Request.Builder().url(url).build()
        return withContext(Dispatchers.IO) {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                runCatching { Json.decodeFromString<FetchedProfile>(response.body.string()) }.getOrNull()
            }
        }
    }
}

@Serializable
data class FetchedProfile(
    @SerialName("id") val shortUuid: String,
    val name: String,
) {
    val uuid: UUID
        get() = UUID.fromString(shortUuid.replace(
            Regex("([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{12})"),
            "$1-$2-$3-$4-$5",
        ))
}
