package plutoproject.feature.menu.paper.factory

import net.kyori.adventure.text.Component
import org.bukkit.Material
import plutoproject.feature.menu.api.paper.descriptor.PageDescriptor
import plutoproject.feature.menu.api.paper.factory.PageDescriptorFactory
import plutoproject.feature.menu.paper.descriptor.PageDescriptorImpl

class PageDescriptorFactoryImpl : PageDescriptorFactory {
    override fun create(
        id: String,
        icon: Material,
        name: Component,
        description: List<Component>,
        customPagingButtonId: String?
    ): PageDescriptor = PageDescriptorImpl(id, icon, name, description, customPagingButtonId)
}
