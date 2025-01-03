package ink.pmc.essentials.commands.teleport

import ink.pmc.essentials.*
import ink.pmc.essentials.api.teleport.TeleportManager
import ink.pmc.framework.chat.NO_PERMISSON
import ink.pmc.framework.chat.replace
import ink.pmc.framework.command.ensurePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission

@Suppress("UNUSED")
object TpcancelCommand {
    @Command("tpcancel [player]")
    @Permission("essentials.tpcancel")
    fun CommandSender.tpcancel(@Argument("player") player: Player?) = ensurePlayer {
        val argRequest = player?.let { TeleportManager.getUnfinishedRequest(it) }
        if (player != null) {
            if (!hasPermission("essentials.tpcancel.other")) {
                sendMessage(NO_PERMISSON)
                return
            }
            if (argRequest == null) {
                sendMessage(
                    COMMAND_TPCANCEL_NO_REQUEST_OTHER
                        .replace("<player>", player.name)
                )
                return
            }
            argRequest.cancel()
            sendMessage(
                COMMAND_TPCANCEL_SUCCEED_OTHER
                    .replace("<player>", player.name)
                    .replace("<dest>", argRequest.destination.name)
            )
            sendMessage(
                COMMAND_TPCANCEL_OTHER_NOTIFY
                    .replace("<player>", argRequest.destination.name)
            )
            return
        }
        val request = TeleportManager.getUnfinishedRequest(this) ?: return run {
            sendMessage(COMMAND_TPCANCEL_NO_REQUEST)
        }
        request.cancel()
        sendMessage(
            COMMAND_TPCANCEL_SUCCEED
                .replace("<player>", request.destination.name)
        )
        playSound(TELEPORT_REQUEST_CANCELLED_SOUND)
    }
}