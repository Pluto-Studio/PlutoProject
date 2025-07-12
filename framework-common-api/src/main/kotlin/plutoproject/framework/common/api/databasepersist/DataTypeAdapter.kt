package plutoproject.framework.common.api.databasepersist

import org.bson.BsonValue
import kotlin.reflect.KClass

/**
 * 用于适配不同对象类型的存储
 *
 * @param T 需要存储的对象类型
 */
interface DataTypeAdapter<T : Any> {
    /**
     * 需要存储的对象类型
     */
    val type: KClass<T>

    /**
     * 将要存储的对象类型转换为 [BsonValue]
     *
     * @param value 要存储的对象实例
     * @return 需要存入数据库的 [BsonValue] 实例
     */
    fun toBson(value: T): BsonValue

    /**
     * 将存入数据库的 [BsonValue] 转换为对象实例
     *
     * @param bson 存入数据库的 [BsonValue] 实例
     * @return 需要的对象实例
     */
    fun fromBson(bson: BsonValue): T
}
