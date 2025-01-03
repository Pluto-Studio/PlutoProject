package plutoproject.framework.common.playerdb

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.BsonDocument
import plutoproject.framework.common.api.playerdb.Database

fun Database.toModel(): DatabaseModel {
    return DatabaseModel(id.toString(), contents)
}

@Serializable
data class DatabaseModel(
    @SerialName("_id") val id: String,
    @Contextual val contents: BsonDocument
)
