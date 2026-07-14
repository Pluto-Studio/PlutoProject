package plutoproject.feature.warp.paper

import plutoproject.foundation.common.serialization.UuidSerializer
import plutoproject.foundation.paper.world.LocationModel

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import org.bukkit.Material
import plutoproject.feature.warp.api.paper.WarpCategory
import plutoproject.feature.warp.api.paper.WarpType
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
