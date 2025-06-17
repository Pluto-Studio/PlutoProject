package plutoproject.framework.paper.util.chat.component

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.craftbukkit.util.CraftChatMessage

fun Component.toNms(): NmsComponent {
    val json = GsonComponentSerializer.gson().serialize(this)
    return CraftChatMessage.fromJSON(json)
}
