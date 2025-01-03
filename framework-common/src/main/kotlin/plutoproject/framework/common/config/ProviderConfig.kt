package plutoproject.framework.common.config

data class ProviderConfig(
    val mongo: MongoDB = MongoDB(),
    val geoIp: GeoIP = GeoIP(),
)

data class MongoDB(
    val host: String = "localhost",
    val port: Int = 27017,
    val database: String = "pluto_production",
    val username: String = "pluto",
    val password: String = "pluto_passworld_###",
)

data class GeoIP(
    val database: String = "GeoLite2-City.mmdb",
)
