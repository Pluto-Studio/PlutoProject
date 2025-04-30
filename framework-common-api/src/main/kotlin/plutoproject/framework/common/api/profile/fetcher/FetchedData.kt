package plutoproject.framework.common.api.profile.fetcher

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import plutoproject.framework.common.util.data.convertShortUuidToLong
import plutoproject.framework.common.util.data.convertToUuid
import java.util.*

@Serializable
data class FetchedData(
    @SerialName("id") val shortUuid: String,
    val name: String,
) {
    val uuid: UUID
        get() = shortUuid.convertShortUuidToLong().convertToUuid()
}
