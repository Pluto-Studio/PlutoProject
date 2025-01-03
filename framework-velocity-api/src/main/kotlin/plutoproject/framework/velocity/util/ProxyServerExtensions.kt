package plutoproject.framework.velocity.util

import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.Component

fun ProxyServer.broadcast(message: Component) {
    consoleCommandSource.sendMessage(message)
    allPlayers.forEach { it.sendMessage(message) }
}