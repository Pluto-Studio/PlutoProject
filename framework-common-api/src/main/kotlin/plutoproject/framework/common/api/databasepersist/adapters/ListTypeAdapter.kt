package plutoproject.framework.common.api.databasepersist.adapters

import org.bson.BsonArray
import org.bson.BsonValue
import plutoproject.framework.common.api.databasepersist.DataTypeAdapter
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
sealed class ListTypeAdapter<T : Any>(private val innerType: DataTypeAdapter<T>) : DataTypeAdapter<List<T>> {
    override val type: KClass<List<T>> = List::class as KClass<List<T>>

    override fun fromBson(bson: BsonValue): List<T> {
        require(bson is BsonArray) { "Bson value is not BsonArray." }
        return bson.values.map { innerType.fromBson(it) }
    }

    override fun toBson(value: List<T>): BsonValue {
        return BsonArray(value.map { innerType.toBson(it) })
    }

    data object String : ListTypeAdapter<kotlin.String>(StringTypeAdapter)
    data object Boolean : ListTypeAdapter<kotlin.Boolean>(BooleanTypeAdapter)
    data object Int : ListTypeAdapter<kotlin.Int>(IntTypeAdapter)
    data object Long : ListTypeAdapter<kotlin.Long>(LongTypeAdapter)
    data object Double : ListTypeAdapter<kotlin.Double>(DoubleTypeAdapter)
    data object Float : ListTypeAdapter<kotlin.Float>(FloatTypeAdapter)
    data object Short : ListTypeAdapter<kotlin.Short>(ShortTypeAdapter)
    data object Byte : ListTypeAdapter<kotlin.Byte>(ByteTypeAdapter)
    data object Char : ListTypeAdapter<kotlin.Char>(CharTypeAdapter)
    class Custom<T : Any>(innerType: DataTypeAdapter<T>) : ListTypeAdapter<T>(innerType)
}
