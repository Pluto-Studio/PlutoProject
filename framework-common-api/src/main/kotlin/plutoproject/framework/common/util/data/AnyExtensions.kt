package plutoproject.framework.common.util.data

import org.bson.*
import org.jetbrains.annotations.ApiStatus

@Deprecated("")
@ApiStatus.ScheduledForRemoval
fun Any?.convertPrimitiveToBson(): BsonValue = when (this) {
    is Byte -> BsonInt32(toInt())
    is Short -> BsonInt32(toInt())
    is Int -> BsonInt32(this)
    is Long -> BsonInt64(this)
    is Float -> BsonDouble(toDouble())
    is Double -> BsonDouble(this)
    is Char -> BsonString(toString())
    is Boolean -> BsonBoolean(this)
    is String -> BsonString(this)
    null -> BsonNull()
    else -> error("Not a primitive")
}

