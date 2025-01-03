package plutoproject.framework.common.options.models

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import plutoproject.framework.common.api.options.EntryValueType
import plutoproject.framework.common.api.options.EntryValueType.*
import plutoproject.framework.common.api.options.OptionEntry
import plutoproject.framework.common.options.UnknownDescriptor
import plutoproject.framework.common.util.data.json.Gson

@OptIn(InternalSerializationApi::class)
@Suppress("UNCHECKED_CAST")
internal fun OptionEntry<*>.toModel(): OptionEntryModel {
    val type = when (descriptor.type) {
        UNKNOWN -> (descriptor as UnknownDescriptor).originalType
        else -> descriptor.type
    }
    val stringValue = when (descriptor.type) {
        INT -> Json.encodeToString(value as Int)
        LONG -> Json.encodeToString(value as Long)
        SHORT -> Json.encodeToString(value as Short)
        BYTE -> Json.encodeToString(value as Byte)
        DOUBLE -> Json.encodeToString(value as Double)
        FLOAT -> Json.encodeToString(value as Float)
        BOOLEAN -> Json.encodeToString(value as Boolean)
        STRING -> Json.encodeToString(value as String)
        OBJECT -> {
            val objClass = checkNotNull(descriptor.objectClass) { "Object class cannot be null: ${descriptor.key}" }
            val kSerializer = objClass.kotlin.serializerOrNull()
            if (kSerializer != null) {
                Json.encodeToString(kSerializer as KSerializer<Any>, value)
            } else {
                Gson.toJson(value, objClass)
            }
        }

        UNKNOWN -> value as String
    }
    return OptionEntryModel(descriptor.key, stringValue, type)
}

@Serializable
data class OptionEntryModel(
    val key: String,
    val value: String,
    val type: EntryValueType
)
