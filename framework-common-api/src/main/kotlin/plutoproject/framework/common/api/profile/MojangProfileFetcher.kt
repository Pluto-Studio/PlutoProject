package plutoproject.framework.common.api.profile

import com.google.gson.JsonParser
import okhttp3.Request
import plutoproject.framework.common.util.coroutine.withIO
import plutoproject.framework.common.util.data.convertShortUuidToLong
import plutoproject.framework.common.util.network.UrlConstants
import java.util.*

@Suppress("UNUSED")
object MojangProfileFetcher : AbstractProfileFetcher() {
    override val id: String = "mojang"

    override suspend fun fetch(name: String): ProfileData? = try {
        lookup(name)
    } catch (e: Exception) {
        null
    }

    private suspend fun lookup(name: String): ProfileData? = withIO {
        val request = Request.Builder()
            .url("${UrlConstants.MOJANG_API}users/profiles/minecraft/${name.lowercase()}")
            .build()
        val call = httpClient.newCall(request)
        val response = call.execute()
        val body = response.body
        val jsonObject = JsonParser.parseString(body.string()).asJsonObject ?: return@withIO null
        val resultId = jsonObject.get("id").asString.convertShortUuidToLong()
        val resultName = jsonObject.get("name").asString ?: return@withIO null
        val uuid = UUID.fromString(resultId)
        ProfileData(uuid, resultName)
    }
}
