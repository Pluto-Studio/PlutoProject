package plutoproject.framework.common.databasepersist

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.BsonDocument
import org.bson.types.ObjectId
import plutoproject.framework.common.util.data.serializers.bson.UuidBinarySerializer
import java.util.*

@Serializable
data class ContainerModel(
    @SerialName("_id") val objectId: @Contextual ObjectId = ObjectId(),
    val playerId: @Serializable(UuidBinarySerializer::class) UUID,
    val createdAt: Long,
    val updatedAt: Long,
    val updatedByServer: String,
    val data: @Contextual BsonDocument,
)
