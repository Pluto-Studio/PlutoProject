package plutoproject.capability.databasepersist.api.adapters

import org.bson.BsonInt32
import org.bson.BsonValue
import plutoproject.capability.databasepersist.api.DataTypeAdapter
import kotlin.reflect.KClass

object IntTypeAdapter : DataTypeAdapter<Int> {
    override val type: KClass<Int> = Int::class

    override fun fromBson(bson: BsonValue): Int {
        require(bson is BsonInt32) { "Bson value is not Int32." }
        return bson.value
    }

    override fun toBson(value: Int): BsonValue = BsonInt32(value)
}
