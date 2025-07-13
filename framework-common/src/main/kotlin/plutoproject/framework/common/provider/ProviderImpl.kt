package plutoproject.framework.common.provider

import com.maxmind.geoip2.DatabaseReader
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.bson.UuidRepresentation
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.framework.common.api.provider.Provider
import plutoproject.framework.common.config.ProviderConfig
import plutoproject.framework.common.util.getFrameworkModuleDataFolder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class ProviderImpl : Provider, KoinComponent {
    private val config by inject<ProviderConfig>()
    override val mongoClient: MongoClient
    override val defaultMongoDatabase: MongoDatabase
    override val geoIpDatabase: DatabaseReader

    init {
        val mongoConfig = config.mongo
        val encodedUsername = URLEncoder.encode(mongoConfig.username, StandardCharsets.UTF_8.toString())
        val encodedPassword = URLEncoder.encode(mongoConfig.password, StandardCharsets.UTF_8.toString())
        val mongoSettings = MongoClientSettings.builder()
            .applyConnectionString(ConnectionString("mongodb://${encodedUsername}:${encodedPassword}@${mongoConfig.host}:${mongoConfig.port}/${mongoConfig.database}"))
            .uuidRepresentation(UuidRepresentation.STANDARD)
            .build()
        mongoClient = MongoClient.create(mongoSettings)
        defaultMongoDatabase = mongoClient.getDatabase(mongoConfig.database)

        val geoIpConfig = config.geoIp
        val dbFile = getFrameworkModuleDataFolder("provider").resolve(geoIpConfig.database)
        check(dbFile.exists()) { "GeoIP database file not found" }
        geoIpDatabase = DatabaseReader.Builder(dbFile).build()
    }

    override fun close() {
        mongoClient.close()
        geoIpDatabase.close()
    }
}
