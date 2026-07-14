package plutoproject.capability.databasepersist.api.adapters

import org.bson.BsonValue
import plutoproject.capability.databasepersist.api.DataTypeAdapter
import kotlin.reflect.KClass

object FloatTypeAdapter : DataTypeAdapter<Float> {
    override val type: KClass<Float> = Float::class

    override fun fromBson(bson: BsonValue): Float =
        DoubleTypeAdapter.fromBson(bson).toFloat()

    override fun toBson(value: Float): BsonValue =
        DoubleTypeAdapter.toBson(value.toDouble())
}
