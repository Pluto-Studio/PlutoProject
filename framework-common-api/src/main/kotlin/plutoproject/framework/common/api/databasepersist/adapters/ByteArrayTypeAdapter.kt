package plutoproject.framework.common.api.databasepersist.adapters

import org.bson.BsonBinary
import org.bson.BsonValue
import plutoproject.framework.common.api.databasepersist.DataTypeAdapter

object ByteArrayTypeAdapter : DataTypeAdapter<ByteArray> {
    override val type: Class<ByteArray> = ByteArray::class.java

    override fun fromBson(bson: BsonValue): ByteArray {
        require(bson is BsonBinary) { "Bson value is not BsonBinary." }
        return bson.data
    }

    override fun toBson(value: ByteArray): BsonValue {
        return BsonBinary(value)
    }
}
