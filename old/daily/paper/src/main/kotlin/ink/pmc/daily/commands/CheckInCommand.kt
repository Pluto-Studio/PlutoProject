package ink.pmc.daily.commands

import ink.pmc.daily.CHECKED_IN
import ink.pmc.daily.api.Daily
import ink.pmc.daily.screens.DailyCalenderScreen
import ink.pmc.framework.startScreen
import ink.pmc.framework.chat.UI_SUCCEED_SOUND
import ink.pmc.framework.command.ensurePlayer
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command

@Suppress("UNUSED")
object CheckInCommand {
    @Command("checkin")
    suspend fun CommandSender.checkIn() = ensurePlayer {
        val user = Daily.getUserOrCreate(uniqueId)
        if (user.isCheckedInToday()) {
            sendMessage(CHECKED_IN)
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