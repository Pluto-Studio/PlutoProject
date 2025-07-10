package plutoproject.framework.common.api.databasepersist.adapters

import com.google.common.reflect.TypeToken
import org.bson.BsonBinary
import org.bson.BsonValue
import plutoproject.framework.common.api.databasepersist.DataTypeAdapter
import java.util.*

object JavaUuidTypeAdapter : DataTypeAdapter<UUID> {
    override val type: TypeToken<UUID> = TypeToken.of(UUID::class.java)

    override fun fromBson(bson: BsonValue): UUID {
        require(bson is BsonBinary) { "Bson value is not BsonBinary." }
        return bson.asUuid()
    }

    override fun toBson(value: UUID): BsonValue {
        return BsonBinary(value)
    }
}
