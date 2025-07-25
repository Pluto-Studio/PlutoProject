package plutoproject.framework.common.api.connection

import com.mongodb.ClientSessionOptions
import com.mongodb.TransactionOptions
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import plutoproject.framework.common.util.database.withTransaction
import plutoproject.framework.common.util.inject.Koin

/**
 * MongoDB 数据库外部链接。
 */
interface MongoConnection {
    companion object : MongoConnection by Koin.get()

    /**
     * 链接的 [MongoClient]。
     */
    val client: MongoClient

    /**
     * 链接的 [MongoDatabase]。
     */
    val database: MongoDatabase
}

/**
 * 获取指定名称的集合。
 *
 * @param T 集合文档类型
 * @param collectionName 集合名称
 * @return 获取到的 [MongoCollection] 对象
 */
inline fun <reified T : Any> MongoConnection.getCollection(collectionName: String): MongoCollection<T> {
    return database.getCollection(collectionName)
}

/**
 * 开启事务上下文。
 *
 * @param clientSessionOptions 此 ClientSession 的设置
 * @param transactionOptions 此事务的设置
 * @param block 事务逻辑函数
 */
suspend inline fun MongoConnection.withTransaction(
    clientSessionOptions: ClientSessionOptions = ClientSessionOptions.builder().build(),
    transactionOptions: TransactionOptions = TransactionOptions.builder().build(),
    block: () -> Unit
) {
    client.startSession(clientSessionOptions).withTransaction(transactionOptions, block)
}
