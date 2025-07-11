package plutoproject.framework.common.api.databasepersist.adapters

import org.bson.BsonValue
import plutoproject.framework.common.api.databasepersist.DataTypeAdapter

object FloatTypeAdapter : DataTypeAdapter<Float> {
    override val type: Class<Float> = Float::class.java

    override fun fromBson(bson: BsonValue): Float {
        return DoubleTypeAdapter.fromBson(bson).toFloat()
    }

    override fun toBson(value: Float): BsonValue {
        return DoubleTypeAdapter.toBson(value.toDouble())
    }
}
