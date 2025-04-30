package plutoproject.framework.velocity.util.command

import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import plutoproject.framework.common.util.chat.PLAYER_ONLY_COMMAND

inline fun ensurePlayer(sender: CommandSource, action: Player.() -> Unit) {
    if (sender !is Player) {
        sender.sendMessage(PLAYER_ONLY_COMMAND)
        return
    }
    sender.action()
}

@JvmName("ensurePlayerReceiver")
inline fun CommandSource.ensurePlayer(action: Player.() -> Unit) {
    ensurePlayer(this, action)
}

inline fun <reified T : Player> selectPlayer(self: CommandSource, other: T?): T? {
    return other ?: self as? T
}
