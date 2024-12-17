package ink.pmc.menu.model

import ink.pmc.framework.utils.data.serializers.bson.BsonUUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class UserModel(
    @Serializable(BsonUUIDSerializer::class) val uuid: UUID,
    val wasOpenedBefore: Boolean,
    val itemGivenServers: List<String>
)