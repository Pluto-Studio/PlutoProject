package ink.pmc.menu.descriptor

import ink.pmc.menu.api.descriptor.PageDescriptor
import net.kyori.adventure.text.Component
import org.bukkit.Material

data class PageDescriptorImpl(
    override val id: String,
    override val icon: Material,
    override val name: Component,
    override val description: List<Component>,
    override val customPagingButtonId: String?
) : PageDescriptor