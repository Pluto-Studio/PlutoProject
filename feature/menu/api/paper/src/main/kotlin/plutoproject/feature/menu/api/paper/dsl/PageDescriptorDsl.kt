package plutoproject.feature.menu.api.paper.dsl

import net.kyori.adventure.text.Component
import org.bukkit.Material
import plutoproject.feature.menu.api.paper.descriptor.PageDescriptor
import plutoproject.feature.menu.api.paper.factory.PageDescriptorFactory
import plutoproject.kernel.api.currentModuleContext
import plutoproject.kernel.api.getService

class PageDescriptorDsl {
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

    fun build(): PageDescriptor = currentModuleContext().services.getService<PageDescriptorFactory>().create(
        id = id ?: error("Id not set"),
        icon = icon ?: error("Icon not set"),
        name = name ?: error("Name not set"),
        description = _description,
        customPagingButtonId = customPagingButtonId
    )
}

inline fun PageDescriptor(block: PageDescriptorDsl.() -> Unit): PageDescriptor =
    PageDescriptorDsl().apply(block).build()
