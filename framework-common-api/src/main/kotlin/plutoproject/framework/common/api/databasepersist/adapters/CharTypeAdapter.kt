package plutoproject.framework.common.api.databasepersist.adapters

import org.bson.BsonValue
import plutoproject.framework.common.api.databasepersist.DataTypeAdapter
import kotlin.reflect.KClass

object CharTypeAdapter : DataTypeAdapter<Char> {
    override val type: KClass<Char> = Char::class

    override fun fromBson(bson: BsonValue): Char {
        return StringTypeAdapter.fromBson(bson).first()
    }

    override fun toBson(value: Char): BsonValue {
        return StringTypeAdapter.toBson(value.toString())
    }
}
