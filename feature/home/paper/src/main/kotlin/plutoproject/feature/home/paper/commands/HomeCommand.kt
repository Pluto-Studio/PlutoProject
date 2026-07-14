package plutoproject.feature.home.paper.commands

import plutoproject.feature.home.paper.homeManager

import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.home.api.paper.Home
import plutoproject.feature.home.api.paper.HomeManager
import plutoproject.feature.home.paper.COMMAND_HOME
import plutoproject.feature.home.paper.screens.HomeListScreen
import plutoproject.foundation.common.text.replace
import plutoproject.feature.home.paper.startScreen
import plutoproject.foundation.paper.command.ensurePlayer

@Suppress("UNUSED")
object HomeCommand {
    @Command("home [home]")
    @Permission("plutoproject.home.command.home")
    suspend fun CommandSender.home(@Argument("home", parserName = "home") home: Home?) = ensurePlayer {
        if (home == null) {
            val homes = homeManager.list(this)
            val preferred = homeManager.getPreferredHome(this)
            val picked = if (homes.size == 1) homes.first() else preferred
            if (picked == null) {
                startScreen(HomeListScreen(this))
                return
            }
            picked.teleportSuspend(this)
            sendMessage(COMMAND_HOME.replace("<name>", picked.name))
            return
        }
        home.teleportSuspend(this)
        sendMessage(COMMAND_HOME.replace("<name>", home.name))
    }
}
