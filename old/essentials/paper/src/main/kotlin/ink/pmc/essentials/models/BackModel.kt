package ink.pmc.essentials.models

import ink.pmc.framework.serialize.UUIDSerializer
import ink.pmc.framework.storage.LocationModel
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import java.util.*

@Serializable
data class BackModel(
    @SerialName("_id") @Contextual val objectId: ObjectId,
    @Serializable(UUIDSerializer::class) val owner: UUID,
    var recordedAt: Long,
    var location: LocationModel
)