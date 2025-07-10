package plutoproject.framework.common.api.databasepersist.adapters

import com.google.common.reflect.TypeToken
import org.bson.BsonValue
import plutoproject.framework.common.api.databasepersist.DataTypeAdapter

object ShortTypeAdapter : DataTypeAdapter<Short> {
    override val type: TypeToken<Short> = TypeToken.of(Short::class.java)

    override fun fromBson(bson: BsonValue): Short {
        return IntTypeAdapter.fromBson(bson).toShort()
    }

    override fun toBson(value: Short): BsonValue {
        return IntTypeAdapter.toBson(value.toInt())
    }
}
