package plutoproject.capability.mongo.common

internal data class MongoConfig(
    val host: String,
    val port: Int = 27017,
    val database: String,
    val username: String,
    val password: String,
)
