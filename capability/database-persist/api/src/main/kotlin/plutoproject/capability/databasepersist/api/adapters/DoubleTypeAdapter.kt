package plutoproject.capability.databasepersist.api.adapters

import org.bson.BsonDouble
import org.bson.BsonValue
import plutoproject.capability.databasepersist.api.DataTypeAdapter
import kotlin.reflect.KClass

object DoubleTypeAdapter : DataTypeAdapter<Double> {
    override val type: KClass<Double> = Double::class

    override fun fromBson(bson: BsonValue): Double {
        require(bson is BsonDouble) { "Bson value is not BsonDouble." }
        return bson.value
    }

    override fun toBson(value: Double): BsonValue = BsonDouble(value)
}
