package ink.pmc.essentials.models

import ink.pmc.essentials.api.warp.WarpCategory
import ink.pmc.essentials.api.warp.WarpType
import ink.pmc.framework.serialize.UUIDSerializer
import ink.pmc.framework.storage.LocationModel
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
    val founder: String?,
    val icon: Material?,
    val category: WarpCategory?,
    val description: String?,
    val type: WarpType,
    val createdAt: Long,
    val location: LocationModel,
)