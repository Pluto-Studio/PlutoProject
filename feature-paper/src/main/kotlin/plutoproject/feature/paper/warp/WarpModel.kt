package plutoproject.feature.paper.warp

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import org.bukkit.Material
import plutoproject.feature.paper.api.warp.WarpCategory
import plutoproject.feature.paper.api.warp.WarpType
import plutoproject.framework.common.util.data.serializers.UuidSerializer
import plutoproject.framework.paper.util.data.models.LocationModel
import java.util.*

@Serializable
data class WarpModel(
    @SerialName("_id") @Contextual val objectId: ObjectId,
    val id: @Serializable(UuidSerializer::class) UUID,
    val name: String,
    val alias: String?,
    val founder: String?,
    val icon: Material?,
    val category: WarpCategory?,
    val description: String?,
    val type: WarpType,
    val createdAt: Long,
    val location: LocationModel,
)
