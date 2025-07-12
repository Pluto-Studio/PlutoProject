package plutoproject.framework.common.databasepersist

import kotlinx.serialization.Serializable
import plutoproject.framework.common.util.data.serializers.bson.UuidBinarySerializer
import java.util.*

@Serializable
data class UpdateInfo(
    val playerId: @Serializable(UuidBinarySerializer::class) UUID,
    val server: String,
    val timestamp: Long,
)
