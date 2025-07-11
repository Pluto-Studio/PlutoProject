package plutoproject.framework.paper.databasepersist

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import plutoproject.framework.common.util.data.serializers.bson.UuidBinarySerializer
import java.util.*

@Serializable
data class TestModel(
    val objectId: @Contextual ObjectId = ObjectId(),
    val uniqueId: @Serializable(UuidBinarySerializer::class) UUID = UUID.randomUUID()
)
