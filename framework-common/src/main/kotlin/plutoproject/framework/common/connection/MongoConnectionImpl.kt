package plutoproject.framework.common.connection

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.bson.UuidRepresentation
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import plutoproject.framework.common.api.connection.MongoConnection
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MongoConnectionImpl : MongoConnection, ExternalConnection, KoinComponent {
    private val config by lazy { get<ExternalConnectionConfig>().mongo }

    init {
        check(config.enabled) { "MongoDB external connection is disabled in configuration." }
    }

    override val client: MongoClient = connectMongo()
    override val database: MongoDatabase = client.getDatabase(config.database)

    private fun connectMongo(): MongoClient {
        val username = URLEncoder.encode(config.username, StandardCharsets.UTF_8.toString())
        val password = URLEncoder.encode(config.password, StandardCharsets.UTF_8.toString())
        val settings = MongoClientSettings.builder()
            .applyConnectionString(ConnectionString("mongodb://${username}:${password}@${config.host}:${config.port}/${config.database}"))
            .uuidRepresentation(UuidRepresentation.STANDARD)
            .build()
        return MongoClient.create(settings)
    }

    override fun close() {
        client.close()
    }
}
