package plutoproject.feature.paper.home

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import org.bukkit.Material
import plutoproject.framework.common.util.data.serializers.UuidSerializer
import plutoproject.framework.paper.util.data.models.LocationModel
import java.util.*

@Serializable
data class HomeModel(
    @SerialName("_id") @Contextual val objectId: ObjectId,
    @Serializable(UuidSerializer::class) val id: UUID,
    val name: String,
    val icon: Material? = null,
    val createdAt: Long,
    val location: LocationModel,
    @Serializable(UuidSerializer::class) val owner: UUID,
    val isStarred: Boolean = false,
    val isPreferred: Boolean = false,
)
