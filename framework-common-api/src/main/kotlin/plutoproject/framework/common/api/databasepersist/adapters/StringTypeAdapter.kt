package plutoproject.framework.common.api.databasepersist.adapters

import com.google.common.reflect.TypeToken
import org.bson.BsonString
import org.bson.BsonValue
import plutoproject.framework.common.api.databasepersist.DataTypeAdapter

object StringTypeAdapter : DataTypeAdapter<String> {
    override val type: TypeToken<String> = TypeToken.of(String::class.java)

    override fun fromBson(bson: BsonValue): String {
        require(bson is BsonString) { "Bson value is not String." }
        return bson.value
    }

    override fun toBson(value: String): BsonValue {
        return BsonString(value)
    }
}
