package plutoproject.feature.paper.back

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import plutoproject.framework.common.util.data.serializers.UuidSerializer
import plutoproject.framework.paper.util.data.models.LocationModel
import java.util.*

@Serializable
data class BackModel(
    @SerialName("_id") @Contextual val objectId: ObjectId,
    @Serializable(UuidSerializer::class) val owner: UUID,
    var recordedAt: Long,
    var location: LocationModel,
)
