package plutoproject.framework.common.api.options.factory

import plutoproject.framework.common.api.options.EntryValueType
import plutoproject.framework.common.api.options.Limitation
import plutoproject.framework.common.api.options.OptionDescriptor
import plutoproject.framework.common.util.inject.inlinedGet

interface OptionDescriptorFactory {
    companion object : OptionDescriptorFactory by inlinedGet()

    fun <T> create(
        key: String,
        type: EntryValueType,
        defaultValue: T? = null,
        objectClass: Class<*>? = null,
        limitation: Limitation<T>? = null
    ): OptionDescriptor<T>
}
