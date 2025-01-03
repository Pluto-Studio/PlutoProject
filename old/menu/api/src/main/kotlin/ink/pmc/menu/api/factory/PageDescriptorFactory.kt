package ink.pmc.menu.api.factory

import ink.pmc.framework.inject.inlinedGet
import ink.pmc.menu.api.descriptor.PageDescriptor
import net.kyori.adventure.text.Component
import org.bukkit.Material

interface PageDescriptorFactory {
    companion object : PageDescriptorFactory by inlinedGet()

    fun create(
        id: String,
        icon: Material,
        name: Component,
        description: List<Component>,
        customPagingButtonId: String? = null
    ): PageDescriptor
}