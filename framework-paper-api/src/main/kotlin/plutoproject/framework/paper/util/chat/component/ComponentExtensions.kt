package plutoproject.framework.paper.util.chat.component

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.minecraft.core.RegistryAccess

fun Component.toNms(): NmsComponent {
    val json = GsonComponentSerializer.gson().serialize(this)
    return NmsComponentSerializer.fromJson(json, RegistryAccess.EMPTY) as NmsComponent
}
