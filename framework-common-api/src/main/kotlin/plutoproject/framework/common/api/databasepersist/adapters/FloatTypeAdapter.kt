package plutoproject.framework.common.api.databasepersist.adapters

import org.bson.BsonValue
import plutoproject.framework.common.api.databasepersist.DataTypeAdapter
import kotlin.reflect.KClass

object FloatTypeAdapter : DataTypeAdapter<Float> {
    override val type: KClass<Float> = Float::class

    override fun fromBson(bson: BsonValue): Float {
        return DoubleTypeAdapter.fromBson(bson).toFloat()
    }

    override fun toBson(value: Float): BsonValue {
        return DoubleTypeAdapter.toBson(value.toDouble())
    }
}
