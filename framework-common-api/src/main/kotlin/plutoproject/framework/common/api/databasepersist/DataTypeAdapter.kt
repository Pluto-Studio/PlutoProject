package plutoproject.framework.common.api.databasepersist

import org.bson.BsonValue

interface DataTypeAdapter<T> {
    fun toBson(value: T): BsonValue
    fun fromBson(bson: BsonValue): T
}
