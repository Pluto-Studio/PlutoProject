package plutoproject.framework.common.api.options

interface Limitation<T> {
    fun match(value: T): Boolean
}
