package plutoproject.framework.common.util.data

import org.bson.*
import org.jetbrains.annotations.ApiStatus

@Deprecated("")
@ApiStatus.ScheduledForRemoval
fun BsonValue?.toPrimitive(): Any? = when (this) {
    is BsonInt32 -> value
    is BsonInt64 -> value
    is BsonDouble -> value
    is BsonString -> value
    is BsonBoolean -> value
    is BsonNull -> null
    null -> null
    else -> error("Not a primitive")
}
