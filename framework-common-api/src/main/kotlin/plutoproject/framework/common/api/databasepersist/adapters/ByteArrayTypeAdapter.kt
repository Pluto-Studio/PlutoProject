package plutoproject.framework.common.api.databasepersist.adapters

import org.bson.BsonBinary
import org.bson.BsonValue
import plutoproject.framework.common.api.databasepersist.DataTypeAdapter
import kotlin.reflect.KClass

object ByteArrayTypeAdapter : DataTypeAdapter<ByteArray> {
    override val type: KClass<ByteArray> = ByteArray::class

    override fun fromBson(bson: BsonValue): ByteArray {
        require(bson is BsonBinary) { "Bson value is not BsonBinary." }
        return bson.data
    }

    override fun toBson(value: ByteArray): BsonValue {
        return BsonBinary(value)
    }
}
