package plutoproject.framework.paper.toast

import net.kyori.adventure.text.Component
import org.bukkit.Material
import plutoproject.framework.paper.api.toast.Toast
import plutoproject.framework.paper.api.toast.ToastFrame
import plutoproject.framework.paper.api.toast.ToastType

data class ToastImpl(
    override val icon: Material,
    override val message: Component,
    override val type: ToastType,
    override val frame: ToastFrame
) : Toast
