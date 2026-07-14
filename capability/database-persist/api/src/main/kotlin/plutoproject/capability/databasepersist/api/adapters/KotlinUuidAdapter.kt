package plutoproject.capability.databasepersist.api.adapters

import org.bson.BsonValue
import plutoproject.capability.databasepersist.api.DataTypeAdapter
import kotlin.reflect.KClass
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
object KotlinUuidAdapter : DataTypeAdapter<Uuid> {
    override val type: KClass<Uuid> = Uuid::class

    override fun fromBson(bson: BsonValue): Uuid =
        JavaUuidTypeAdapter.fromBson(bson).toKotlinUuid()

    override fun toBson(value: Uuid): BsonValue =
        JavaUuidTypeAdapter.toBson(value.toJavaUuid())
}
