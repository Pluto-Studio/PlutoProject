package plutoproject.framework.common.api.options

import java.util.*

interface PlayerOptions {
    val player: UUID
    val entries: List<OptionEntry<*>>

    fun <T : Any> contains(descriptor: OptionDescriptor<T>): Boolean

    fun <T : Any> getEntry(descriptor: OptionDescriptor<T>): OptionEntry<T>?

    fun <T : Any> setEntry(descriptor: OptionDescriptor<T>, value: T)

    fun removeEntry(descriptor: OptionDescriptor<*>)

    suspend fun reload()

    suspend fun save()
}
