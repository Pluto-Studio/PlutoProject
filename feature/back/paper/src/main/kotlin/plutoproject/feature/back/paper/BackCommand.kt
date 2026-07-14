package plutoproject.feature.back.paper

import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.back.api.paper.BackManager
import plutoproject.foundation.paper.command.ensurePlayer

@Suppress("UNUSED")
object BackCommand {
    @Command("back")
    @Permission("plutoproject.back.command.back")
    suspend fun CommandSender.back() = ensurePlayer {
        if (!backManager.has(this)) {
            sendMessage(COMMAND_BACK_FAILED_NO_LOCATION)
            return@ensurePlayer
        }
        backManager.backSuspend(this)
        sendMessage(COMMAND_BACK)
    }
}
