package plutoproject.framework.common.api.databasepersist.adapters

import org.bson.BsonValue
import plutoproject.framework.common.api.databasepersist.DataTypeAdapter
import kotlin.reflect.KClass
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
object KotlinUuidAdapter : DataTypeAdapter<Uuid> {
    override val type: KClass<Uuid> = Uuid::class

    override fun fromBson(bson: BsonValue): Uuid {
        return JavaUuidTypeAdapter.fromBson(bson).toKotlinUuid()
    }

    override fun toBson(value: Uuid): BsonValue {
        return JavaUuidTypeAdapter.toBson(value.toJavaUuid())
    }
}
