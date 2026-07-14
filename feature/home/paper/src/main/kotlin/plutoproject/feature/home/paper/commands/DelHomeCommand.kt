package plutoproject.feature.home.paper.commands

import plutoproject.feature.home.paper.homeManager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.home.api.paper.Home
import plutoproject.feature.home.api.paper.HomeManager
import plutoproject.feature.home.paper.COMMAND_DELHOME
import plutoproject.foundation.common.text.replace
import plutoproject.feature.home.paper.moduleScope
import plutoproject.foundation.paper.command.ensurePlayer

@Suppress("UNUSED")
object DelHomeCommand {
    @Command("delhome <home>")
    @Permission("plutoproject.home.command.delhome")
    fun CommandSender.delhome(@Argument("home", parserName = "home") home: Home) = ensurePlayer {
        moduleScope.launch(Dispatchers.IO) {
            homeManager.remove(home.id)
        }
        sendMessage(COMMAND_DELHOME.replace("<name>", home.name))
    }
}
