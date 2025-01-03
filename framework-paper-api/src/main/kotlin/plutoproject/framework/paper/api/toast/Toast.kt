package plutoproject.framework.paper.api.toast

import net.kyori.adventure.text.Component
import org.bukkit.Material

interface Toast {
    val icon: Material
    val message: Component
    val type: ToastType
    val frame: ToastFrame
}
