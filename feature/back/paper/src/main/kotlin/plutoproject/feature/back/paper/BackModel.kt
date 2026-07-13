package plutoproject.feature.back.paper

import plutoproject.foundation.common.serialization.UuidSerializer
import plutoproject.foundation.paper.world.LocationModel

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import java.util.*

@Serializable
data class BackModel(
    @SerialName("_id") @Contextual val objectId: ObjectId,
    @Serializable(UuidSerializer::class) val owner: UUID,
    var recordedAt: Long,
    var location: LocationModel,
)
