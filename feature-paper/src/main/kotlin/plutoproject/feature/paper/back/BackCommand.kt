package plutoproject.feature.paper.back

import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.paper.api.back.BackManager
import plutoproject.framework.paper.util.command.ensurePlayer

@Suppress("UNUSED")
object BackCommand {
    @Command("back")
    @Permission("essentials.back")
    suspend fun CommandSender.back() = ensurePlayer {
        if (!BackManager.has(this)) {
            sendMessage(COMMAND_BACK_FAILED_NO_LOCATION)
            return@ensurePlayer
        }
        BackManager.backSuspend(this)
        sendMessage(COMMAND_BACK)
    }
}
