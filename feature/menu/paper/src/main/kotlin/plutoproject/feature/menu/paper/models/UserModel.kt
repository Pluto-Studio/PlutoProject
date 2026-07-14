package plutoproject.feature.menu.paper.models

import kotlinx.serialization.Serializable
import plutoproject.foundation.common.bson.UuidAsBsonBinarySerializer
import java.util.*

@Serializable
data class UserModel(
    @Serializable(UuidAsBsonBinarySerializer::class) val uuid: UUID,
    val wasOpenedBefore: Boolean,
    val itemGivenServers: List<String>
)
