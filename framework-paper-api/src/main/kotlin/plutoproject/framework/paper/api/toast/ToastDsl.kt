package plutoproject.framework.paper.api.toast

import ink.pmc.advkt.component.RootComponentKt
import net.kyori.adventure.text.Component
import org.bukkit.Material

class ToastDsl {
    private var icon: Material = Material.PAPER
    private var message: Component = Component.empty()
    private var type: ToastType = ToastType.TASK
    private var frame: ToastFrame = ToastFrame.ADVENTURE

    fun icon(icon: Material) {
        this.icon = icon
    }

    fun message(component: Component) {
        this.message = component
    }

    fun message(content: RootComponentKt.() -> Unit) {
        this.message = RootComponentKt().apply(content).build()
    }

    fun type(type: ToastType) {
        this.type = type
    }

    fun frame(frame: ToastFrame) {
        this.frame = frame
    }

    fun create(): Toast = ToastFactory.of(icon, message, type, frame)
}

fun Toast(content: ToastDsl.() -> Unit): Toast {
    return ToastDsl().apply(content).create()
}
