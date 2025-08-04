package plutoproject.framework.paper.util.entity

import com.google.common.io.ByteStreams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ClientboundClearDialogPacket
import net.minecraft.server.level.ServerPlayer
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player
import plutoproject.framework.common.util.coroutine.Loom
import plutoproject.framework.common.util.coroutine.PluginScope
import plutoproject.framework.paper.util.plugin

fun Player.toNmsPlayer(): ServerPlayer = (this as CraftPlayer).handle

fun Player.sendPacket(packet: Packet<*>) {
    toNmsPlayer().connection.send(packet)
}

suspend fun Player.switchServer(name: String) {
    withContext(Dispatchers.Loom) {
        val out = ByteStreams.newDataOutput()
        out.writeUTF("Connect")
        out.writeUTF(name)
        player?.sendPluginMessage(plugin, "BungeeCord", out.toByteArray())
    }
}

fun Player.switchServerAsync(name: String) = PluginScope.launch(Dispatchers.Loom) {
    switchServer(name)
}

fun Player.clearDialog() {
    sendPacket(ClientboundClearDialogPacket.INSTANCE)
}
