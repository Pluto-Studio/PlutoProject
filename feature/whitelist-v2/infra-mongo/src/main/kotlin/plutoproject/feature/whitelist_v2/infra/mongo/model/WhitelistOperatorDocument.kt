package plutoproject.feature.whitelist_v2.infra.mongo.model

import kotlinx.serialization.Serializable
import plutoproject.framework.common.util.data.serializers.bson.UuidAsBsonBinarySerializer
import java.util.UUID

@Serializable
data class WhitelistOperatorDocument(
    val type: WhitelistOperatorDocumentType,
    val administrator: @Serializable(UuidAsBsonBinarySerializer::class) UUID? = null,
)

@Serializable
enum class WhitelistOperatorDocumentType {
    CONSOLE,
    ADMINISTRATOR,
}
