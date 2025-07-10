package plutoproject.framework.common.api.databasepersist

import java.util.*

/**
 * 数据容器接口
 */
interface PersistContainer {
    /**
     * 此数据容器所属玩家的 ID
     */
    val playerId: UUID

    /**
     * 设置一个键值
     *
     * @param key 需要设置的键
     * @param adapter 需要设置的值的类型适配器
     * @param value 需要设置的值
     */
    fun <T : Any> set(key: String, adapter: DataTypeAdapter<T>, value: T)

    /**
     * 删除一个键值
     *
     * @param key 需要删除的键
     */
    fun remove(key: String)


    /**
     * 获取一个键值
     *
     * @param key 需要获取的键
     * @param adapter 需要获取的值的类型适配器
     * @return 若这个键存在则返回值，不存在则为 null
     */
    suspend fun <T : Any> get(key: String, adapter: DataTypeAdapter<T>): T?

    /**
     * 检查一个键是否存在
     *
     * @param key 需要检查的键
     * @return 若存在则为 true，反之为 false
     */
    suspend fun contains(key: String): Boolean

    /**
     * 将更改写入数据库
     */
    suspend fun save()
}
