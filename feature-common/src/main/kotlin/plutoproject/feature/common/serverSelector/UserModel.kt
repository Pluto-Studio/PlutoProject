package plutoproject.feature.common.serverSelector

import kotlinx.serialization.Serializable
import plutoproject.framework.common.util.data.serializers.bson.JavaUuidBinarySerializer
import java.util.*

@Serializable
data class UserModel(
    @Serializable(JavaUuidBinarySerializer::class) val uuid: UUID,
    val hasJoinedBefore: Boolean,
    val previouslyJoinedServer: String?
)
