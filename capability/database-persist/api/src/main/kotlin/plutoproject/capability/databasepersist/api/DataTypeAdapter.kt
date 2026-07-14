package plutoproject.capability.databasepersist.api

import org.bson.BsonValue
import kotlin.reflect.KClass

interface DataTypeAdapter<T : Any> {
    val type: KClass<T>

    fun toBson(value: T): BsonValue

    fun fromBson(bson: BsonValue): T
}
