package plutoproject.framework.common.api.databasepersist.adapters

import org.bson.BsonValue
import plutoproject.framework.common.api.databasepersist.DataTypeAdapter

object CharTypeAdapter : DataTypeAdapter<Char> {
    override val type: Class<Char> = Char::class.java

    override fun fromBson(bson: BsonValue): Char {
        return StringTypeAdapter.fromBson(bson).first()
    }

    override fun toBson(value: Char): BsonValue {
        return StringTypeAdapter.toBson(value.toString())
    }
}
