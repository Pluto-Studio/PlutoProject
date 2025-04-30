package plutoproject.framework.paper.util.command

import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import plutoproject.framework.common.util.chat.PLAYER_ONLY_COMMAND

inline fun ensurePlayer(sender: CommandSender, action: Player.() -> Unit) {
    if (sender !is Player) {
        sender.sendMessage(PLAYER_ONLY_COMMAND)
        return
    }
    sender.action()
}

@JvmName("ensurePlayerReceiver")
inline fun CommandSender.ensurePlayer(action: Player.() -> Unit) {
    ensurePlayer(this, action)
}

inline fun <reified T : OfflinePlayer> selectPlayer(self: CommandSender, other: T?): T? {
    return other ?: self as? T
}
