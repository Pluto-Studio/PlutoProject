package plutoproject.framework.common.connection

data class ExternalConnectionConfig(
    val mongo: MongoConnectionConfig,
    val geoIp: GeoIpDatabaseConnectionConfig = GeoIpDatabaseConnectionConfig(),
    val charonflow: CharonFlowConfig = CharonFlowConfig(),
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

data class CharonFlowConfig(
    val enabled: Boolean = true,
    val redis: String = "redis://localhost:6379",
)
