package plutoproject.feature.paper.menu.models

import kotlinx.serialization.Serializable
import plutoproject.framework.common.util.data.serializers.bson.JavaUuidBsonSerializer
import java.util.*

@Serializable
data class UserModel(
    @Serializable(JavaUuidBsonSerializer::class) val uuid: UUID,
    val wasOpenedBefore: Boolean,
    val itemGivenServers: List<String>
)
