package plutoproject.feature.common.whitelist_v2.model

import kotlinx.serialization.Serializable
import plutoproject.framework.common.util.data.serializers.bson.UuidAsBsonBinarySerializer
import java.util.*

enum class WhitelistOperatorModelType {
    CONSOLE, ADMINISTRATOR
}

@Serializable
data class WhitelistOperatorModel(
    val type: WhitelistOperatorModelType,
    val administrator: @Serializable(UuidAsBsonBinarySerializer::class) UUID? = null
)
