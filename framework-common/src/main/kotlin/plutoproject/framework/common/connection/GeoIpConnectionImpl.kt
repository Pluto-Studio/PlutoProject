package plutoproject.framework.common.connection

import com.maxmind.geoip2.DatabaseReader
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import plutoproject.framework.common.api.connection.GeoIpConnection
import plutoproject.framework.common.util.getFrameworkModuleDataFolder

class GeoIpConnectionImpl : GeoIpConnection, ExternalConnection, KoinComponent {
    private val config by lazy { get<ExternalConnectionConfig>().geoIp }

    init {
        check(config.enabled) { "GeoIP database external connection is disabled in configuration." }
    }

    override val database: DatabaseReader = connectGeoIp()

    private fun connectGeoIp(): DatabaseReader {
        val databaseFile = getFrameworkModuleDataFolder("connection").resolve(config.database)
        check(databaseFile.exists()) { "GeoIP database file not found. Expected at: ${databaseFile.canonicalFile}" }
        return DatabaseReader.Builder(databaseFile).build()
    }

    override fun close() {
        database.close()
    }
}
