package plutoproject.framework.common.api.databasepersist.adapters

import org.bson.BsonValue
import plutoproject.framework.common.api.databasepersist.DataTypeAdapter
import kotlin.reflect.KClass

object ByteTypeAdapter : DataTypeAdapter<Byte> {
    override val type: KClass<Byte> = Byte::class

    override fun fromBson(bson: BsonValue): Byte {
        return IntTypeAdapter.fromBson(bson).toByte()
    }

    override fun toBson(value: Byte): BsonValue {
        return IntTypeAdapter.toBson(value.toInt())
    }
}
