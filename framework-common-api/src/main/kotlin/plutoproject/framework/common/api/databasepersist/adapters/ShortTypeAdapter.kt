package plutoproject.framework.common.api.databasepersist.adapters

import org.bson.BsonValue
import plutoproject.framework.common.api.databasepersist.DataTypeAdapter
import kotlin.reflect.KClass

object ShortTypeAdapter : DataTypeAdapter<Short> {
    override val type: KClass<Short> = Short::class

    override fun fromBson(bson: BsonValue): Short {
        return IntTypeAdapter.fromBson(bson).toShort()
    }

    override fun toBson(value: Short): BsonValue {
        return IntTypeAdapter.toBson(value.toInt())
    }
}
