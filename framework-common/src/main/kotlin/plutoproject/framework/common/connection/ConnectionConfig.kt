package plutoproject.framework.common.connection

data class ExternalConnectionConfig(
    val mongo: MongoConnectionConfig,
    val geoIp: GeoIpDatabaseConnectionConfig = GeoIpDatabaseConnectionConfig(),
)

data class MongoConnectionConfig(
    val enabled: Boolean = true,
    val host: String,
    val port: Int = 27017,
    val database: String,
    val username: String,
    val password: String,
)

data class GeoIpDatabaseConnectionConfig(
    val enabled: Boolean = false,
    val database: String = "GeoLite2-City.mmdb",
)
