package ink.pmc.essentials.commands.back

import ink.pmc.essentials.COMMAND_BACK_FAILED_NO_LOC
import ink.pmc.essentials.COMMAND_BACK_SUCCEED
import ink.pmc.essentials.TELEPORT_FAILED_SOUND
import ink.pmc.essentials.api.back.BackManager
import ink.pmc.framework.utils.command.ensurePlayer
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission

@Suppress("UNUSED")
object BackCommand {
    @Command("back")
    @Permission("essentials.back")
    suspend fun CommandSender.back() = ensurePlayer {
        if (!BackManager.has(this)) {
            sendMessage(COMMAND_BACK_FAILED_NO_LOC)
            playSound(TELEPORT_FAILED_SOUND)
            return@ensurePlayer
        }
        BackManager.backSuspend(this)
        sendMessage(COMMAND_BACK_SUCCEED)
    }
}