package plutoproject.capability.mongo.common

import com.mongodb.ClientSessionOptions
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.TransactionOptions
import com.mongodb.kotlin.client.coroutine.ClientSession
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.hocon.HoconParser
import org.bson.UuidRepresentation
import org.koin.dsl.module
import org.koin.dsl.onClose
import plutoproject.capability.mongo.api.MongoConnection
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.RuntimeModule
import plutoproject.kernel.api.exportServiceFromKoin
import plutoproject.kernel.api.loadKoinModuleDefinitions
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.reflect.KClass

@OptIn(ExperimentalHoplite::class)
class MongoCapability : RuntimeModule {
    override suspend fun onLoad(context: ModuleContext) {
        val configFile = context.saveResource("config.conf")
        val config = ConfigLoaderBuilder.empty()
            .withClassLoader(MongoCapability::class.java.classLoader)
            .withExplicitSealedTypes()
            .addDefaults()
            .addParser("conf", HoconParser())
            .addPropertySource(PropertySource.file(configFile.toFile()))
            .build()
            .loadConfigOrThrow<MongoConfig>()
        context.loadKoinModuleDefinitions(module {
            single { DefaultMongoConnection(config) }.onClose { it?.close() }
            single<MongoConnection> { get<DefaultMongoConnection>() }
        })
        context.services.exportServiceFromKoin<MongoConnection>()
    }
}

internal class DefaultMongoConnection(config: MongoConfig) : MongoConnection, AutoCloseable {
    override val client: MongoClient = MongoClient.create(settings(config))
    override val database: MongoDatabase = client.getDatabase(config.database)

    override fun <T : Any> getCollection(name: String, type: KClass<T>): MongoCollection<T> =
        database.getCollection(name, type.java)

    override suspend fun withTransaction(
        clientSessionOptions: ClientSessionOptions,
        transactionOptions: TransactionOptions,
        block: suspend (ClientSession) -> Unit,
    ): Result<Unit> = runCatching {
        client.startSession(clientSessionOptions).use { session ->
            session.startTransaction(transactionOptions)
            try {
                block(session)
                session.commitTransaction()
            } catch (cause: Throwable) {
                session.abortTransaction()
                throw cause
            }
        }
    }

    override fun close() = client.close()
}

internal fun settings(config: MongoConfig): MongoClientSettings {
    val username = URLEncoder.encode(config.username, StandardCharsets.UTF_8)
    val password = URLEncoder.encode(config.password, StandardCharsets.UTF_8)
    return MongoClientSettings.builder()
        .applyConnectionString(ConnectionString("mongodb://$username:$password@${config.host}:${config.port}/${config.database}"))
        .uuidRepresentation(UuidRepresentation.STANDARD)
        .build()
}
