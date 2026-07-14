package plutoproject.capability.databasepersist.api.adapters

import org.bson.BsonDateTime
import org.bson.BsonValue
import plutoproject.capability.databasepersist.api.DataTypeAdapter
import java.time.Instant
import kotlin.reflect.KClass

object InstantTypeAdapter : DataTypeAdapter<Instant> {
    override val type: KClass<Instant> = Instant::class

    override fun fromBson(bson: BsonValue): Instant {
        require(bson is BsonDateTime) { "Bson value is not BsonDateTime." }
        return Instant.ofEpochMilli(bson.value)
    }

    override fun toBson(value: Instant): BsonValue = BsonDateTime(value.toEpochMilli())
}
