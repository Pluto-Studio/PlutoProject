package ink.pmc.whitelist

import ink.pmc.framework.utils.currentUnixTimestamp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

fun createWhitelistModel(uuid: UUID, name: String): WhitelistModel {
    return WhitelistModel(uuid.toString(), name, currentUnixTimestamp)
}

@Serializable
data class WhitelistModel(
    @SerialName("_id") val id: String,
    var rawName: String,
    val addedAt: Long,
) {
    val name: String = rawName.lowercase()
}