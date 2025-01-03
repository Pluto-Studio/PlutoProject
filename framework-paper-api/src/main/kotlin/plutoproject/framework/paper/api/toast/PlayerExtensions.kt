package plutoproject.framework.paper.api.toast

import org.bukkit.entity.Player

fun Player.showToast(
    toast: Toast,
    render: ToastRenderer<Player> = DefaultToastRenderer
) = render.render(this, toast)

fun Player.showToast(
    render: ToastRenderer<Player> = DefaultToastRenderer,
    block: ToastDsl.() -> Unit
) {
    val toast = ToastDsl().apply(block).create()
    showToast(toast, render)
}
