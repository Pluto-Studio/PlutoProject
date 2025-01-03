package plutoproject.framework.common.api.options

interface OptionDescriptor<T> {
    val key: String
    val type: EntryValueType
    val defaultValue: T?
    val objectClass: Class<*>?
    val limitation: Limitation<T>?
}
