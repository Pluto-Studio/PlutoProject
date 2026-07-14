package plutoproject.feature.menu.paper.descriptor

import net.kyori.adventure.text.Component
import org.bukkit.Material
import plutoproject.feature.menu.api.paper.descriptor.PageDescriptor

data class PageDescriptorImpl(
    override val id: String,
    override val icon: Material,
    override val name: Component,
    override val description: List<Component>,
    override val customPagingButtonId: String?
) : PageDescriptor
