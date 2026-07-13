package plutoproject.capability.geoip.common

import com.maxmind.geoip2.DatabaseReader
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.hocon.HoconParser
import org.koin.dsl.module
import org.koin.dsl.onClose
import plutoproject.capability.geoip.api.GeoIpConnection
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.RuntimeModule
import plutoproject.kernel.api.exportServiceFromKoin
import plutoproject.kernel.api.loadKoinModuleDefinitions

@OptIn(ExperimentalHoplite::class)
class GeoIpCapability : RuntimeModule {
    override suspend fun onLoad(context: ModuleContext) {
        context.dataFolder.toFile().mkdirs()
        val configFile = context.saveResource("config.conf")
        val config = ConfigLoaderBuilder.empty()
            .withClassLoader(GeoIpCapability::class.java.classLoader)
            .withExplicitSealedTypes()
            .addDefaults()
            .addParser("conf", HoconParser())
            .addPropertySource(PropertySource.file(configFile.toFile()))
            .build()
            .loadConfigOrThrow<GeoIpConfig>()
        context.loadKoinModuleDefinitions(module {
            single { DefaultGeoIpConnection(config, context) }.onClose { it?.close() }
            single<GeoIpConnection> { get<DefaultGeoIpConnection>() }
        })
        context.services.exportServiceFromKoin<GeoIpConnection>()
    }
}

internal class DefaultGeoIpConnection(config: GeoIpConfig, context: ModuleContext) : GeoIpConnection, AutoCloseable {
    override val database: DatabaseReader = run {
        val databaseFile = context.dataFolder.resolve(config.database)
        check(databaseFile.toFile().exists()) { "GeoIP database file not found. Expected at: $databaseFile" }
        DatabaseReader.Builder(databaseFile.toFile()).build()
    }

    override fun close() = database.close()
}
