package plutoproject.framework.common.api.connection

import com.mongodb.ClientSessionOptions
import com.mongodb.TransactionOptions
import com.mongodb.kotlin.client.coroutine.ClientSession
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import plutoproject.framework.common.util.inject.Koin
import kotlin.reflect.KClass

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

    /**
     * 获取指定名称的集合。
     *
     * @param T 集合文档类型
     * @param collectionName 集合名称
     * @param type 集合文档类
     * @return 获取到的 [MongoCollection] 对象
     */
    fun <T : Any> getCollection(collectionName: String, type: KClass<T>): MongoCollection<T>

    /**
     * 开启事务。
     *
     * @param clientSessionOptions [ClientSession] 设置
     * @param transactionOptions 事务设置
     * @param block 事务逻辑函数
     * @return 事务结果，若失败可获取异常
     */
    suspend fun withTransaction(
        clientSessionOptions: ClientSessionOptions = ClientSessionOptions.builder().build(),
        transactionOptions: TransactionOptions = TransactionOptions.builder().build(),
        block: suspend (ClientSession) -> Unit
    ): Result<Unit>
}

/**
 * 获取指定名称的集合。
 *
 * @param T 集合文档类型
 * @param collectionName 集合名称
 * @return 获取到的 [MongoCollection] 对象
 */
inline fun <reified T : Any> MongoConnection.getCollection(collectionName: String): MongoCollection<T> {
    return getCollection(collectionName, T::class)
}
