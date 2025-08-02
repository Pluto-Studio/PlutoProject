package plutoproject.framework.common.databasepersist

import kotlinx.serialization.Serializable
import plutoproject.framework.common.util.data.serializers.bson.UuidAsBsonBinarySerializer
import java.util.*

@Serializable
data class UpdateInfo(
    val playerId: @Serializable(UuidAsBsonBinarySerializer::class) UUID,
    val server: String,
    val timestamp: Long,
)
