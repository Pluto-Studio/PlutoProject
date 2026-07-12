package plutoproject.capability.databasepersist.api.adapters

import org.bson.BsonValue
import plutoproject.capability.databasepersist.api.DataTypeAdapter
import kotlin.reflect.KClass

object ByteTypeAdapter : DataTypeAdapter<Byte> {
    override val type: KClass<Byte> = Byte::class

    override fun fromBson(bson: BsonValue): Byte =
        IntTypeAdapter.fromBson(bson).toByte()

    override fun toBson(value: Byte): BsonValue =
        IntTypeAdapter.toBson(value.toInt())
}
