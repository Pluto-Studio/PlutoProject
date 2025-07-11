package plutoproject.framework.common.api.databasepersist.adapters

import org.bson.BsonInt64
import org.bson.BsonValue
import plutoproject.framework.common.api.databasepersist.DataTypeAdapter

object LongTypeAdapter : DataTypeAdapter<Long> {
    override val type: Class<Long> = Long::class.java

    override fun fromBson(bson: BsonValue): Long {
        require(bson is BsonInt64) { "Bson value is not Int64." }
        return bson.value
    }

    override fun toBson(value: Long): BsonValue {
        return BsonInt64(value)
    }
}
