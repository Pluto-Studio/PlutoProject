package plutoproject.framework.paper.util.entity

import com.google.common.io.ByteStreams
import net.minecraft.network.protocol.Packet
import net.minecraft.server.level.ServerPlayer
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player
import plutoproject.framework.common.util.coroutine.runAsyncIO
import plutoproject.framework.common.util.coroutine.withIO
import plutoproject.framework.paper.util.plugin

fun Player.toNmsPlayer(): ServerPlayer = (this as CraftPlayer).handle

fun Player.sendPacket(packet: Packet<*>) {
    toNmsPlayer().connection.send(packet)
}

suspend fun Player.switchServer(name: String) {
    withIO {
        val out = ByteStreams.newDataOutput()
        out.writeUTF("Connect")
        out.writeUTF(name)
        player?.sendPluginMessage(plugin, "BungeeCord", out.toByteArray())
    }
}

fun Player.switchServerAsync(name: String) = runAsyncIO {
    switchServer(name)
}
