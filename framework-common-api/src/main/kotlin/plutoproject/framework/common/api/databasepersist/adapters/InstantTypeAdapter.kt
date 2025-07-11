package plutoproject.framework.common.api.databasepersist.adapters

import org.bson.BsonDateTime
import org.bson.BsonValue
import plutoproject.framework.common.api.databasepersist.DataTypeAdapter
import plutoproject.framework.common.util.time.toInstant
import java.time.Instant

object InstantTypeAdapter : DataTypeAdapter<Instant> {
    override val type: Class<Instant> = Instant::class.java

    override fun fromBson(bson: BsonValue): Instant {
        require(bson is BsonDateTime) { "Bson value is not BsonDateTime." }
        return bson.value.toInstant()
    }

    override fun toBson(value: Instant): BsonValue {
        return BsonDateTime(value.toEpochMilli())
    }
}
