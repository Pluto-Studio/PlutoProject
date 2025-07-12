package plutoproject.framework.common.api.databasepersist.adapters

import org.bson.BsonDocument
import org.bson.BsonValue
import plutoproject.framework.common.api.databasepersist.DataTypeAdapter
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
sealed class MapTypeAdapter<T : Any>(private val innerType: DataTypeAdapter<T>) : DataTypeAdapter<Map<String, T>> {
    override val type: KClass<Map<kotlin.String, T>> = Map::class as KClass<Map<kotlin.String, T>>

    override fun fromBson(bson: BsonValue): Map<kotlin.String, T> {
        require(bson is BsonDocument) { "Bson value is not BsonDocument." }
        return bson.entries.associate { (key, value) -> key to innerType.fromBson(value) }
    }

    override fun toBson(value: Map<kotlin.String, T>): BsonValue {
        return BsonDocument().apply {
            putAll(value.entries.associate { (key, value) -> key to innerType.toBson(value) })
        }
    }

    data object String : MapTypeAdapter<kotlin.String>(StringTypeAdapter)
    data object Boolean : MapTypeAdapter<kotlin.Boolean>(BooleanTypeAdapter)
    data object Int : MapTypeAdapter<kotlin.Int>(IntTypeAdapter)
    data object Long : MapTypeAdapter<kotlin.Long>(LongTypeAdapter)
    data object Double : MapTypeAdapter<kotlin.Double>(DoubleTypeAdapter)
    data object Float : MapTypeAdapter<kotlin.Float>(FloatTypeAdapter)
    data object Short : MapTypeAdapter<kotlin.Short>(ShortTypeAdapter)
    data object Byte : MapTypeAdapter<kotlin.Byte>(ByteTypeAdapter)
    data object Char : MapTypeAdapter<kotlin.Char>(CharTypeAdapter)
    class Custom<T : Any>(innerType: DataTypeAdapter<T>) : MapTypeAdapter<T>(innerType)
}
