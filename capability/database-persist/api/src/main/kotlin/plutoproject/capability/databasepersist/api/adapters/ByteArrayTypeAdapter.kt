package plutoproject.capability.databasepersist.api.adapters

import org.bson.BsonBinary
import org.bson.BsonValue
import plutoproject.capability.databasepersist.api.DataTypeAdapter
import kotlin.reflect.KClass

object ByteArrayTypeAdapter : DataTypeAdapter<ByteArray> {
    override val type: KClass<ByteArray> = ByteArray::class

    override fun fromBson(bson: BsonValue): ByteArray {
        require(bson is BsonBinary) { "Bson value is not BsonBinary." }
        return bson.data
    }

    override fun toBson(value: ByteArray): BsonValue = BsonBinary(value)
}
