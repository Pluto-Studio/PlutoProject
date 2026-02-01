package plutoproject.feature.paper.daily.commands

import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.paper.daily.screens.DailyCalenderScreen
import plutoproject.framework.paper.api.interactive.startScreen
import plutoproject.framework.paper.util.command.ensurePlayer

@Suppress("UNUSED")
object DailyCalendarCommand {
    @Command("dailycalender")
    @Permission("plutoproject.daily.command.dailycalender")
    fun CommandSender.dailyCalender() = ensurePlayer {
        startScreen(DailyCalenderScreen())
    }
}
