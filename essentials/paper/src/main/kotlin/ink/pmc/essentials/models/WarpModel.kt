package ink.pmc.essentials.models

import ink.pmc.essentials.api.warp.WarpCategory
import ink.pmc.essentials.api.warp.WarpType
import ink.pmc.framework.utils.data.UUIDSerializer
import ink.pmc.framework.utils.storage.LocationModel
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import org.bukkit.Material
import java.util.*

@Serializable
data class WarpModel(
    @SerialName("_id") @Contextual val objectId: ObjectId,
    val id: @Serializable(UUIDSerializer::class) UUID,
    val name: String,
    val alias: String?,
    val icon: Material?,
    val category: WarpCategory?,
    val type: WarpType,
    val createdAt: Long,
    val location: LocationModel,
)