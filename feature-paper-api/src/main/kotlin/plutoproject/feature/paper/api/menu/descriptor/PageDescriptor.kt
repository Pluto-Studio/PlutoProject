package plutoproject.feature.paper.api.menu.descriptor

import net.kyori.adventure.text.Component
import org.bukkit.Material

interface PageDescriptor {
    val id: String
    val icon: Material
    val name: Component
    val description: List<Component>
    val customPagingButtonId: String?
}
