package plutoproject.framework.common.profile

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import plutoproject.framework.common.util.data.serializers.UuidSerializer
import java.util.*

@Serializable
data class ProfileModel(
    @SerialName("_id") @Contextual val objectId: ObjectId,
    @Serializable(UuidSerializer::class) val uuid: UUID,
    val name: String,
    val lowercasedName: String = name.lowercase(),
)
