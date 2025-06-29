package plutoproject.feature.paper.daily.commands

import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import plutoproject.feature.paper.daily.screens.DailyCalenderScreen
import plutoproject.framework.paper.api.interactive.startScreen
import plutoproject.framework.paper.util.command.ensurePlayer

@Suppress("UNUSED")
object DailyCalendarCommand {
    @Command("dailycalender")
    fun CommandSender.dailyCalender() = ensurePlayer {
        startScreen(DailyCalenderScreen())
    }
}
