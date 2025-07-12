package plutoproject.framework.common.api.databasepersist.adapters

import org.bson.BsonBoolean
import org.bson.BsonValue
import plutoproject.framework.common.api.databasepersist.DataTypeAdapter
import kotlin.reflect.KClass

object BooleanTypeAdapter : DataTypeAdapter<Boolean> {
    override val type: KClass<Boolean> = Boolean::class

    override fun fromBson(bson: BsonValue): Boolean {
        require(bson is BsonBoolean) { "Bson value is not Boolean." }
        return bson.value
    }

    override fun toBson(value: Boolean): BsonValue {
        return BsonBoolean(value)
    }
}
