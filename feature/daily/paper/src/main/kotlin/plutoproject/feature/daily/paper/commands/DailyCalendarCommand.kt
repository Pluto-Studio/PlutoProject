package plutoproject.feature.daily.paper.commands

import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.daily.paper.screens.DailyCalenderScreen
import plutoproject.feature.daily.paper.startScreen
import plutoproject.foundation.paper.command.ensurePlayer

@Suppress("UNUSED")
object DailyCalendarCommand {
    @Command("dailycalender")
    @Permission("plutoproject.daily.command.dailycalender")
    fun CommandSender.dailyCalender() = ensurePlayer {
        startScreen(DailyCalenderScreen())
    }
}
