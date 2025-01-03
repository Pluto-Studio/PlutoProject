package plutoproject.framework.common.api.options

interface OptionEntry<T : Any> {
    val descriptor: OptionDescriptor<T>
    val value: T

    fun intValue(): Int

    fun longValue(): Long

    fun shortValue(): Short

    fun byteValue(): Byte

    fun doubleValue(): Double

    fun floatValue(): Float

    fun booleanValue(): Boolean

    fun stringValue(): String

    fun objectValue(): T
}
