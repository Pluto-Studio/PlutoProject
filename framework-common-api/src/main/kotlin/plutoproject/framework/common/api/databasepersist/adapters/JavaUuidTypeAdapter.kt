package plutoproject.framework.common.api.databasepersist.adapters

import org.bson.BsonBinary
import org.bson.BsonValue
import plutoproject.framework.common.api.databasepersist.DataTypeAdapter
import java.util.*
import kotlin.reflect.KClass

object JavaUuidTypeAdapter : DataTypeAdapter<UUID> {
    override val type: KClass<UUID> = UUID::class

    override fun fromBson(bson: BsonValue): UUID {
        require(bson is BsonBinary) { "Bson value is not BsonBinary." }
        return bson.asUuid()
    }

    override fun toBson(value: UUID): BsonValue {
        return BsonBinary(value)
    }
}
