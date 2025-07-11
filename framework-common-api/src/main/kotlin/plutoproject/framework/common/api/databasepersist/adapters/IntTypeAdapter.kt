package plutoproject.framework.common.api.databasepersist.adapters

import org.bson.BsonInt32
import org.bson.BsonValue
import plutoproject.framework.common.api.databasepersist.DataTypeAdapter

object IntTypeAdapter : DataTypeAdapter<Int> {
    override val type: Class<Int> = Int::class.java

    override fun fromBson(bson: BsonValue): Int {
        require(bson is BsonInt32) { "Bson value is not Int32." }
        return bson.value
    }

    override fun toBson(value: Int): BsonValue {
        return BsonInt32(value)
    }
}
