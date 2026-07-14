package plutoproject.capability.databasepersist.api.adapters

import org.bson.BsonBinary
import org.bson.BsonValue
import plutoproject.capability.databasepersist.api.DataTypeAdapter
import java.util.*
import kotlin.reflect.KClass

object JavaUuidTypeAdapter : DataTypeAdapter<UUID> {
    override val type: KClass<UUID> = UUID::class

    override fun fromBson(bson: BsonValue): UUID {
        require(bson is BsonBinary) { "Bson value is not BsonBinary." }
        return bson.asUuid()
    }

    override fun toBson(value: UUID): BsonValue = BsonBinary(value)
}
