package plutoproject.framework.common.api.databasepersist.adapters

import com.google.common.reflect.TypeToken
import org.bson.BsonValue
import plutoproject.framework.common.api.databasepersist.DataTypeAdapter

object CharTypeAdapter : DataTypeAdapter<Char> {
    override val type: TypeToken<Char> = TypeToken.of(Char::class.java)

    override fun fromBson(bson: BsonValue): Char {
        return StringTypeAdapter.fromBson(bson).first()
    }

    override fun toBson(value: Char): BsonValue {
        return StringTypeAdapter.toBson(value.toString())
    }
}
