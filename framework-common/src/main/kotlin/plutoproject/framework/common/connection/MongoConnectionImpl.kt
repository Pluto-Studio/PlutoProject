package plutoproject.framework.common.connection

import com.mongodb.ClientSessionOptions
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.TransactionOptions
import com.mongodb.kotlin.client.coroutine.ClientSession
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.bson.UuidRepresentation
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import plutoproject.framework.common.api.connection.MongoConnection
import plutoproject.framework.common.util.database.withTransaction
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.reflect.KClass

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

    override fun <T : Any> getCollection(collectionName: String, type: KClass<T>): MongoCollection<T> {
        return database.getCollection(collectionName, type.java)
    }

    override suspend fun withTransaction(
        clientSessionOptions: ClientSessionOptions,
        transactionOptions: TransactionOptions,
        block: suspend (ClientSession) -> Unit
    ): Result<Unit> {
        return client.startSession(clientSessionOptions).use { session ->
            session.withTransaction(transactionOptions, block)
        }
    }

    override fun close() {
        client.close()
    }
}
