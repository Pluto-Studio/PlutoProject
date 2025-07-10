package plutoproject.framework.common.api.databasepersist.adapters

import com.google.common.reflect.TypeToken
import org.bson.BsonValue
import plutoproject.framework.common.api.databasepersist.DataTypeAdapter

object ByteTypeAdapter : DataTypeAdapter<Byte> {
    override val type: TypeToken<Byte> = TypeToken.of(Byte::class.java)

    override fun fromBson(bson: BsonValue): Byte {
        return IntTypeAdapter.fromBson(bson).toByte()
    }

    override fun toBson(value: Byte): BsonValue {
        return IntTypeAdapter.toBson(value.toInt())
    }
}
