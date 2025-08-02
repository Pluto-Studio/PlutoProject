package plutoproject.feature.paper.menu.models

import kotlinx.serialization.Serializable
import plutoproject.framework.common.util.data.serializers.bson.UuidAsBsonBinarySerializer
import java.util.*

@Serializable
data class UserModel(
    @Serializable(UuidAsBsonBinarySerializer::class) val uuid: UUID,
    val wasOpenedBefore: Boolean,
    val itemGivenServers: List<String>
)
