package plutoproject.feature.home.paper.commands

import plutoproject.feature.home.paper.homeManager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotation.specifier.Greedy
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.home.api.paper.Home
import plutoproject.feature.home.api.paper.HomeManager
import plutoproject.feature.home.paper.*
import plutoproject.foundation.common.text.replace
import plutoproject.feature.home.paper.moduleScope
import plutoproject.foundation.paper.command.ensurePlayer

@Suppress("UNUSED")
object EditHomeCommand {
    @Command("edithome <home> prefer")
    @Permission("essentials.edithome")
    fun CommandSender.prefer(@Argument("home", parserName = "home") home: Home) = ensurePlayer {
        if (home.isPreferred) {
            sendMessage(COMMAND_EDITHOME_FAILED_ALREADY_PREFERRED.replace("<name>", home.name))
            return
        }
        moduleScope.launch(Dispatchers.IO) {
            home.setPreferred(true)
        }
        sendMessage(COMMAND_EDITHOME_PREFER.replace("<name>", home.name))
    }

    @Command("edithome <home> star")
    @Permission("essentials.edithome")
    fun CommandSender.star(@Argument("home", parserName = "home") home: Home) = ensurePlayer {
        if (home.isStarred) {
            sendMessage(COMMAND_EDITHOME_FAILED_ALREADY_STARRED.replace("<name>", home.name))
            return
        }
        moduleScope.launch(Dispatchers.IO) {
            home.isStarred = true
            home.update()
        }
        sendMessage(COMMAND_EDITHOME_STAR.replace("<name>", home.name))
    }

    @Command("edithome <home> rename <name>")
    @Permission("essentials.edithome")
    fun CommandSender.rename(
        @Argument("home", parserName = "home") home: Home,
        @Argument("name") @Greedy name: String
    ) = ensurePlayer {
        if (name.length > homeManager.nameLengthLimit) {
            sendMessage(COMMAND_SETHOME_FAILED_NAME_LENGTH_LIMIT)
            return
        }
        moduleScope.launch(Dispatchers.IO) {
            home.name = name
            home.update()
        }
        sendMessage(COMMAND_EDITHOME_RENAME.replace("<new_name>", name))
    }

    @Command("edithome <home> move")
    @Permission("essentials.edithome")
    fun CommandSender.move(@Argument("home", parserName = "home") home: Home) = ensurePlayer {
        moduleScope.launch(Dispatchers.IO) {
            home.location = location
            home.update()
        }
        sendMessage(COMMAND_EDITHOME_MOVE.replace("<name>", home.name))
    }
}
