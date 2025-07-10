package plutoproject.framework.common.databasepersist

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import plutoproject.framework.common.util.data.serializers.JavaUuidSerializer
import java.util.*

data class ContainerModel(
    @SerialName("_id") @Contextual val objectId: ObjectId,
    val containerId: @Serializable(JavaUuidSerializer::class) UUID,
    val playerId: @Serializable(JavaUuidSerializer::class) UUID,
)
