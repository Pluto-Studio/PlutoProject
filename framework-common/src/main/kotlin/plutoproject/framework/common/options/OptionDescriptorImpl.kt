package plutoproject.framework.common.options

import plutoproject.framework.common.api.options.EntryValueType
import plutoproject.framework.common.api.options.Limitation
import plutoproject.framework.common.api.options.OptionDescriptor

data class OptionDescriptorImpl<T>(
    override val key: String,
    override val type: EntryValueType,
    override val defaultValue: T?,
    override val objectClass: Class<*>? = null,
    override val limitation: Limitation<T>? = null
) : OptionDescriptor<T>
