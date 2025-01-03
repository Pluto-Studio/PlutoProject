package plutoproject.framework.common.options

import plutoproject.framework.common.api.options.EntryValueType
import plutoproject.framework.common.api.options.Limitation
import plutoproject.framework.common.api.options.OptionDescriptor
import plutoproject.framework.common.api.options.factory.OptionDescriptorFactory

class OptionDescriptorFactoryImpl : OptionDescriptorFactory {
    override fun <T> create(
        key: String,
        type: EntryValueType,
        defaultValue: T?,
        objectClass: Class<*>?,
        limitation: Limitation<T>?
    ): OptionDescriptor<T> = OptionDescriptorImpl(key, type, defaultValue, objectClass, limitation)
}
