package plutoproject.feature.menu.api.paper.factory

import net.kyori.adventure.text.Component
import org.bukkit.Material
import plutoproject.feature.menu.api.paper.descriptor.PageDescriptor

interface PageDescriptorFactory {
    fun create(
        id: String,
        icon: Material,
        name: Component,
        description: List<Component>,
        customPagingButtonId: String? = null
    ): PageDescriptor
}
