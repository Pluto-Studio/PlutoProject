package plutoproject.capability.mongo.api

import com.mongodb.ClientSessionOptions
import com.mongodb.TransactionOptions
import com.mongodb.kotlin.client.coroutine.ClientSession
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlin.reflect.KClass

interface MongoConnection {
    val client: MongoClient
    val database: MongoDatabase

    fun <T : Any> getCollection(name: String, type: KClass<T>): MongoCollection<T>

    suspend fun withTransaction(
        clientSessionOptions: ClientSessionOptions = ClientSessionOptions.builder().build(),
        transactionOptions: TransactionOptions = TransactionOptions.builder().build(),
        block: suspend (ClientSession) -> Unit,
    ): Result<Unit>
}

inline fun <reified T : Any> MongoConnection.getCollection(name: String): MongoCollection<T> =
    getCollection(name, T::class)
