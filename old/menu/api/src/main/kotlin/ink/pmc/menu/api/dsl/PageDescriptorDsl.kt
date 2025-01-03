package ink.pmc.menu.api.dsl

import ink.pmc.framework.structure.Builder
import ink.pmc.menu.api.descriptor.PageDescriptor
import ink.pmc.menu.api.factory.PageDescriptorFactory
import net.kyori.adventure.text.Component
import org.bukkit.Material

class PageDescriptorDsl : Builder<PageDescriptor> {
    private var _description = mutableListOf<Component>()
    var id: String? = null
    var icon: Material? = null
    var name: Component? = null
    var description: List<Component>
        get() = _description
        set(value) {
            _description.clear()
            _description.addAll(value)
        }
    var customPagingButtonId: String? = null

    fun description(line: Component) {
        _description.add(line)
    }

    fun description(vararg lines: Component) {
        _description.addAll(lines)
    }

    fun description(lines: Iterable<Component>) {
        _description.addAll(lines)
    }

    override fun build(): PageDescriptor {
        return PageDescriptorFactory.create(
            id = id ?: error("Id not set"),
            icon = icon ?: error("Icon not set"),
            name = name ?: error("Name not set"),
            description = _description,
            customPagingButtonId = customPagingButtonId
        )
    }
}

inline fun pageDescriptor(block: PageDescriptorDsl.() -> Unit): PageDescriptor {
    return PageDescriptorDsl().apply(block).build()
}