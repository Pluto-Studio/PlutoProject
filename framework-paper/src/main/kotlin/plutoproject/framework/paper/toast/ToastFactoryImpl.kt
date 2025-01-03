package plutoproject.framework.paper.toast

import net.kyori.adventure.text.Component
import org.bukkit.Material
import plutoproject.framework.paper.api.toast.Toast
import plutoproject.framework.paper.api.toast.ToastFactory
import plutoproject.framework.paper.api.toast.ToastFrame
import plutoproject.framework.paper.api.toast.ToastType

class ToastFactoryImpl : ToastFactory {
    override fun of(
        icon: Material,
        message: Component,
        type: ToastType,
        frame: ToastFrame
    ): Toast = ToastImpl(icon, message, type, frame)
}
