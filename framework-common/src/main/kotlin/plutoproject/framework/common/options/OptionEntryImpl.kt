package plutoproject.framework.common.options

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializerOrNull
import plutoproject.framework.common.api.options.EntryValueType.*
import plutoproject.framework.common.api.options.OptionDescriptor
import plutoproject.framework.common.api.options.OptionEntry
import plutoproject.framework.common.api.options.OptionsManager
import plutoproject.framework.common.options.models.OptionEntryModel
import plutoproject.framework.common.util.data.json.convertJsonToObject

@OptIn(InternalSerializationApi::class)
@Suppress("UNCHECKED_CAST")
internal fun createEntryFromModel(model: OptionEntryModel): OptionEntry<*> {
    val descriptor = OptionsManager.getOptionDescriptor(model.key)
        ?: return OptionEntryImpl(UnknownDescriptor(model.type), model.value)
    return when (descriptor.type) {
        INT -> OptionEntryImpl(descriptor as OptionDescriptor<Int>, Json.decodeFromString(model.value))
        LONG -> OptionEntryImpl(descriptor as OptionDescriptor<Long>, Json.decodeFromString(model.value))
        SHORT -> OptionEntryImpl(descriptor as OptionDescriptor<Short>, Json.decodeFromString(model.value))
        BYTE -> OptionEntryImpl(descriptor as OptionDescriptor<Byte>, Json.decodeFromString(model.value))
        DOUBLE -> OptionEntryImpl(descriptor as OptionDescriptor<Double>, Json.decodeFromString(model.value))
        FLOAT -> OptionEntryImpl(descriptor as OptionDescriptor<Float>, Json.decodeFromString(model.value))
        BOOLEAN -> OptionEntryImpl(descriptor as OptionDescriptor<Boolean>, Json.decodeFromString(model.value))
        STRING -> OptionEntryImpl(descriptor as OptionDescriptor<String>, Json.decodeFromString(model.value))
        OBJECT -> {
            val objClass =
                checkNotNull(descriptor.objectClass) { "Object class cannot be null: ${descriptor.key}" }
            val kSerializer = objClass.kotlin.serializerOrNull()
            if (kSerializer != null) {
                OptionEntryImpl(
                    descriptor as OptionDescriptor<Any>,
                    Json.decodeFromString(kSerializer, model.value)
                )
            } else {
                OptionEntryImpl(descriptor as OptionDescriptor<Any>, model.value.convertJsonToObject(objClass))
            }
        }

        UNKNOWN -> error("Unreachable")
    }
}

class OptionEntryImpl<T : Any>(
    override val descriptor: OptionDescriptor<T>,
    override val value: T
) : OptionEntry<T> {
    override fun intValue(): Int {
        check(descriptor.type == INT) { "${typeCheckMsg(descriptor.key)} Int" }
        return value as Int
    }

    override fun longValue(): Long {
        check(descriptor.type == LONG) { "${typeCheckMsg(descriptor.key)} Long" }
        return value as Long
    }

    override fun shortValue(): Short {
        check(descriptor.type == SHORT) { "${typeCheckMsg(descriptor.key)} Short" }
        return value as Short
    }

    override fun byteValue(): Byte {
        check(descriptor.type == BYTE) { "${typeCheckMsg(descriptor.key)} Byte" }
        return value as Byte
    }

    override fun doubleValue(): Double {
        check(descriptor.type == DOUBLE) { "${typeCheckMsg(descriptor.key)} Double" }
        return value as Double
    }

    override fun floatValue(): Float {
        check(descriptor.type == FLOAT) { "${typeCheckMsg(descriptor.key)} Float" }
        return value as Float
    }

    override fun booleanValue(): Boolean {
        check(descriptor.type == BOOLEAN) { "${typeCheckMsg(descriptor.key)} Boolean" }
        return value as Boolean
    }

    override fun stringValue(): String {
        check(descriptor.type == STRING) { "${typeCheckMsg(descriptor.key)} String" }
        return value as String
    }

    override fun objectValue(): T {
        val objClass = descriptor.objectClass
        checkNotNull(objClass) { "Object class cannot be null: ${descriptor.key}" }
        check(descriptor.type == OBJECT && objClass.isInstance(value)) { "${typeCheckMsg(descriptor.key)} ${objClass.name}" }
        return value
    }

    private fun typeCheckMsg(key: String): String {
        return "Entry $key type not match:"
    }
}
