package ink.pmc.whitelist.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class MemberModel(
    @SerialName("_id") @Contextual val objectId: ObjectId,
    val id: String,
    val rawName: String,
    val whitelistStatus: WhitelistState,
    val authType: AuthType,
    val isHidden: Boolean? = false,
    val createdAt: Long,
)

enum class WhitelistState {
    WHITELISTED, WHITELISTED_BEFORE
}

enum class AuthType {
    OFFICIAL, BEDROCK_ONLY, LITTLESKIN
}