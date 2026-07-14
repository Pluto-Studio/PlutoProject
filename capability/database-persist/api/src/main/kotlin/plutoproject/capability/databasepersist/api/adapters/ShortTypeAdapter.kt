package plutoproject.capability.databasepersist.api.adapters

import org.bson.BsonValue
import plutoproject.capability.databasepersist.api.DataTypeAdapter
import kotlin.reflect.KClass

object ShortTypeAdapter : DataTypeAdapter<Short> {
    override val type: KClass<Short> = Short::class

    override fun fromBson(bson: BsonValue): Short = IntTypeAdapter.fromBson(bson).toShort()

    override fun toBson(value: Short): BsonValue = IntTypeAdapter.toBson(value.toInt())
}
