package plutoproject.feature.daily.paper.commands

import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.daily.api.paper.Daily
import plutoproject.feature.daily.paper.COMMAND_CHECKIN_ALREADY_CHECKIN
import plutoproject.foundation.common.text.UI_SUCCEED_SOUND
import plutoproject.foundation.paper.command.ensurePlayer

@Suppress("UNUSED")
object CheckInCommand {
    @Command("checkin")
    @Permission("plutoproject.daily.command.checkin")
    suspend fun CommandSender.checkIn() = ensurePlayer {
        val user = plutoproject.kernel.api.koinGet<Daily>().getUserOrCreate(uniqueId)
        if (user.isCheckedInToday()) {
            sendMessage(COMMAND_CHECKIN_ALREADY_CHECKIN)
            return@ensurePlayer
        }
        user.checkIn()
        playSound(UI_SUCCEED_SOUND)
    }
}
