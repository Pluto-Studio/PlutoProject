package plutoproject.capability.databasepersist.api.adapters

import org.bson.BsonInt64
import org.bson.BsonValue
import plutoproject.capability.databasepersist.api.DataTypeAdapter
import kotlin.reflect.KClass

object LongTypeAdapter : DataTypeAdapter<Long> {
    override val type: KClass<Long> = Long::class

    override fun fromBson(bson: BsonValue): Long {
        require(bson is BsonInt64) { "Bson value is not Int64." }
        return bson.value
    }

    override fun toBson(value: Long): BsonValue = BsonInt64(value)
}
