package plutoproject.framework.common.options

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.framework.common.api.options.*
import plutoproject.framework.common.options.models.toModel
import plutoproject.framework.common.options.repositories.OptionsContainerRepository
import java.util.*

class PlayerOptionsImpl(
    override val player: UUID,
    private val entriesMap: MutableMap<String, OptionEntry<*>>
) : PlayerOptions, KoinComponent {
    private val repo by inject<OptionsContainerRepository>()
    private val notifier by inject<OptionsUpdateNotifier>()
    override val entries: List<OptionEntry<*>>
        get() = entriesMap.values.toList()

    override fun <T : Any> contains(descriptor: OptionDescriptor<T>): Boolean {
        require(descriptor.key.isNotEmpty()) { "Illegal key" }
        return entriesMap.containsKey(descriptor.key)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> checkDescriptor(descriptor: OptionDescriptor<T>): OptionDescriptor<T> {
        require(descriptor.key.isNotEmpty()) { "Illegal key" }
        require(descriptor.type != EntryValueType.UNKNOWN) { "UNKNOWN is for internal use only" }
        val registeredDescriptor = requireNotNull(OptionsManager.getOptionDescriptor(descriptor.key)) {
            "Descriptor for ${descriptor.key} not registered"
        } as OptionDescriptor<T>
        require(registeredDescriptor == descriptor) {
            "Descriptor for ${descriptor.key} was registered, but not equal to given one"
        }
        return registeredDescriptor
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getEntry(descriptor: OptionDescriptor<T>): OptionEntry<T>? {
        val registeredDescriptor = checkDescriptor(descriptor)
        return when {
            contains(registeredDescriptor) -> entriesMap[registeredDescriptor.key] as? OptionEntry<T>
            registeredDescriptor.defaultValue != null ->
                OptionEntryImpl(registeredDescriptor, registeredDescriptor.defaultValue!!)

            else -> null
        }
    }

    override fun <T : Any> setEntry(descriptor: OptionDescriptor<T>, value: T) {
        checkDescriptor(descriptor)
        if (descriptor.type == EntryValueType.OBJECT) {
            val objClass = checkNotNull(descriptor.objectClass) { "Object class cannot be null: ${descriptor.key}" }
            require(objClass.isInstance(value)) { "Value must be a instance of ${objClass.name}" }
        }
        descriptor.limitation?.let {
            require(it.match(value)) { "Value doesn't match limitation: $value" }
        }
        entriesMap[descriptor.key] = OptionEntryImpl(descriptor, value)
    }

    override fun removeEntry(descriptor: OptionDescriptor<*>) {
        entriesMap.remove(descriptor.key)
    }

    override suspend fun reload() {
        val model = checkNotNull(repo.findById(player)) { "Options reloading failed: Cannot fetch model" }
        entriesMap.clear()
        model.entries.forEach {
            val entry = createEntryFromModel(it)
            entriesMap[it.key] = entry
        }
    }

    override suspend fun save() {
        repo.saveOrUpdate(this.toModel())
        notifier.notify(player)
    }
}
