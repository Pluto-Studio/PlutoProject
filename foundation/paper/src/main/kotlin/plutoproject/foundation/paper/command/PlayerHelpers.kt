package plutoproject.foundation.paper.command

import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import plutoproject.foundation.common.text.PLAYER_ONLY_COMMAND

inline fun CommandSender.ensurePlayer(action: Player.() -> Unit) {
    if (this !is Player) {
        sendMessage(PLAYER_ONLY_COMMAND)
        return
    }
    action()
}

inline fun <reified T : OfflinePlayer> selectPlayer(self: CommandSender, other: T?): T? =
    other ?: self as? T
