package plutoproject.capability.databasepersist.api.adapters

import org.bson.BsonString
import org.bson.BsonValue
import plutoproject.capability.databasepersist.api.DataTypeAdapter
import kotlin.reflect.KClass

object StringTypeAdapter : DataTypeAdapter<String> {
    override val type: KClass<String> = String::class

    override fun fromBson(bson: BsonValue): String {
        require(bson is BsonString) { "Bson value is not String." }
        return bson.value
    }

    override fun toBson(value: String): BsonValue = BsonString(value)
}
