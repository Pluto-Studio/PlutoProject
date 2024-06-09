package ink.pmc.utils.chat

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer

typealias NmsComponent = net.minecraft.network.chat.Component
typealias NmsComponentSerializer = net.minecraft.network.chat.Component.Serializer

val Component.nms: NmsComponent
    get() {
        val json = GsonComponentSerializer.gson().serialize(this)
        return NmsComponentSerializer.fromJson(json) as NmsComponent
    }