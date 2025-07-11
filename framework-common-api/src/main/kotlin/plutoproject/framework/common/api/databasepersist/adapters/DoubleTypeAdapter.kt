package plutoproject.framework.common.api.databasepersist.adapters

import org.bson.BsonDouble
import org.bson.BsonValue
import plutoproject.framework.common.api.databasepersist.DataTypeAdapter

object DoubleTypeAdapter : DataTypeAdapter<Double> {
    override val type: Class<Double> = Double::class.java

    override fun fromBson(bson: BsonValue): Double {
        require(bson is BsonDouble) { "Bson value is not BsonDouble." }
        return bson.value
    }

    override fun toBson(value: Double): BsonValue {
        return BsonDouble(value)
    }
}
