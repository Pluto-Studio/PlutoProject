package plutoproject.framework.common.api.profile

import kotlinx.serialization.Serializable
import plutoproject.framework.common.util.data.serializers.JavaUuidSerializer
import java.util.*

@Serializable
data class CachedProfile(
    val rawName: String,
    val name: String,
    @Serializable(with = JavaUuidSerializer::class) val uuid: UUID,
)
