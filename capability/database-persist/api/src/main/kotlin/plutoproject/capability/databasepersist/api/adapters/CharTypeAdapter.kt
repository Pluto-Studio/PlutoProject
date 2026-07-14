package plutoproject.capability.databasepersist.api.adapters

import org.bson.BsonValue
import plutoproject.capability.databasepersist.api.DataTypeAdapter
import kotlin.reflect.KClass

object CharTypeAdapter : DataTypeAdapter<Char> {
    override val type: KClass<Char> = Char::class

    override fun fromBson(bson: BsonValue): Char =
        StringTypeAdapter.fromBson(bson).first()

    override fun toBson(value: Char): BsonValue =
        StringTypeAdapter.toBson(value.toString())
}
