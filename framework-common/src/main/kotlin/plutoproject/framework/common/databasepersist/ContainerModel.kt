package plutoproject.framework.common.databasepersist

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.BsonDocument
import org.bson.types.ObjectId
import plutoproject.framework.common.util.data.serializers.bson.UuidAsBsonBinarySerializer
import java.util.*

@Serializable
data class ContainerModel(
    @SerialName("_id") val objectId: @Contextual ObjectId = ObjectId(),
    val playerId: @Serializable(UuidAsBsonBinarySerializer::class) UUID,
    val createdAt: Long,
    val updateInfo: UpdateInfo,
    val data: @Contextual BsonDocument,
)
