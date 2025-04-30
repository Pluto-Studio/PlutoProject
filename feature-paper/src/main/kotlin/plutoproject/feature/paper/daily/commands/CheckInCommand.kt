package plutoproject.feature.paper.daily.commands

import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import plutoproject.feature.paper.api.daily.Daily
import plutoproject.feature.paper.daily.ALREADY_CHECK_IN
import plutoproject.feature.paper.daily.screens.DailyCalenderScreen
import plutoproject.framework.common.util.chat.UI_SUCCEED_SOUND
import plutoproject.framework.paper.api.interactive.startScreen
import plutoproject.framework.paper.util.command.ensurePlayer

@Suppress("UNUSED")
object CheckInCommand {
    @Command("checkin")
    suspend fun CommandSender.checkIn() = ensurePlayer {
        val user = Daily.getUserOrCreate(uniqueId)
        if (user.isCheckedInToday()) {
            sendMessage(ALREADY_CHECK_IN)
            return@ensurePlayer
        }
        user.checkIn()
        playSound(UI_SUCCEED_SOUND)
    }

    @Command("checkin gui")
    fun CommandSender.gui() = ensurePlayer {
        startScreen(DailyCalenderScreen())
    }
}
