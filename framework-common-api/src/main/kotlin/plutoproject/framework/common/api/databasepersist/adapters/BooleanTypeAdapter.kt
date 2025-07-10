package plutoproject.framework.common.api.databasepersist.adapters

import com.google.common.reflect.TypeToken
import org.bson.BsonBoolean
import org.bson.BsonValue
import plutoproject.framework.common.api.databasepersist.DataTypeAdapter

object BooleanTypeAdapter : DataTypeAdapter<Boolean> {
    override val type: TypeToken<Boolean> = TypeToken.of(Boolean::class.java)

    override fun fromBson(bson: BsonValue): Boolean {
        require(bson is BsonBoolean) { "Bson value is not Boolean." }
        return bson.value
    }

    override fun toBson(value: Boolean): BsonValue {
        return BsonBoolean(value)
    }
}
