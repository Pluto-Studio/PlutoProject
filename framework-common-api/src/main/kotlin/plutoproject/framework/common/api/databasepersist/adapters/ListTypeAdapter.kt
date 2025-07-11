package plutoproject.framework.common.api.databasepersist.adapters

import org.bson.BsonArray
import org.bson.BsonValue
import plutoproject.framework.common.api.databasepersist.DataTypeAdapter

@Suppress("UNCHECKED_CAST")
sealed class ListTypeAdapter<T : Any> : DataTypeAdapter<List<T>> {
    abstract val innerType: DataTypeAdapter<T>

    override val type: Class<List<T>> = List::class.java as Class<List<T>>

    override fun fromBson(bson: BsonValue): List<T> {
        require(bson is BsonArray) { "Bson value is not BsonArray." }
        return bson.values.map { innerType.fromBson(it) }
    }

    override fun toBson(value: List<T>): BsonValue {
        return BsonArray(value.map { innerType.toBson(it) })
    }

    data object String : ListTypeAdapter<kotlin.String>() {
        override val innerType: DataTypeAdapter<kotlin.String> = StringTypeAdapter
    }

    data object Boolean : ListTypeAdapter<kotlin.Boolean>() {
        override val innerType: DataTypeAdapter<kotlin.Boolean> = BooleanTypeAdapter
    }

    data object Int : ListTypeAdapter<kotlin.Int>() {
        override val innerType: DataTypeAdapter<kotlin.Int> = IntTypeAdapter
    }

    data object Long : ListTypeAdapter<kotlin.Long>() {
        override val innerType: DataTypeAdapter<kotlin.Long> = LongTypeAdapter
    }

    data object Double : ListTypeAdapter<kotlin.Double>() {
        override val innerType: DataTypeAdapter<kotlin.Double> = DoubleTypeAdapter
    }

    data object Float : ListTypeAdapter<kotlin.Float>() {
        override val innerType: DataTypeAdapter<kotlin.Float> = FloatTypeAdapter
    }

    data object Short : ListTypeAdapter<kotlin.Short>() {
        override val innerType: DataTypeAdapter<kotlin.Short> = ShortTypeAdapter
    }

    data object Byte : ListTypeAdapter<kotlin.Byte>() {
        override val innerType: DataTypeAdapter<kotlin.Byte> = ByteTypeAdapter
    }

    data object Char : ListTypeAdapter<kotlin.Char>() {
        override val innerType: DataTypeAdapter<kotlin.Char> = CharTypeAdapter
    }

    data class Custom<T : Any>(override val innerType: DataTypeAdapter<T>) : ListTypeAdapter<T>()
}
