package plutoproject.framework.common.api.options.dsl

import plutoproject.framework.common.api.options.EntryValueType
import plutoproject.framework.common.api.options.Limitation
import plutoproject.framework.common.api.options.OptionDescriptor
import plutoproject.framework.common.api.options.factory.OptionDescriptorFactory

class OptionDescriptorDsl<T> {
    var key: String? = null
    var type: EntryValueType? = null
    var defaultValue: T? = null
    var objectClass: Class<*>? = null
    var limitation: Limitation<T>? = null

    fun build(): OptionDescriptor<T> =
        OptionDescriptorFactory.create(
            requireNotNull(key) { "Key cannot be null" },
            requireNotNull(type) { "Type cannot be null" },
            defaultValue,
            objectClass,
            limitation
        )
}

inline fun <reified T> descriptor(block: OptionDescriptorDsl<T>.() -> Unit): OptionDescriptor<T> =
    OptionDescriptorDsl<T>()
        .apply { objectClass = T::class.java }
        .apply(block)
        .build()
