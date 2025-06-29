package plutoproject.feature.paper.home.commands

import org.bukkit.command.CommandSender
import org.incendo.cloud.annotation.specifier.Greedy
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.paper.api.home.Home
import plutoproject.feature.paper.api.home.HomeManager
import plutoproject.feature.paper.home.*
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.common.util.coroutine.runAsync
import plutoproject.framework.paper.util.command.ensurePlayer

@Suppress("UNUSED")
object EditHomeCommand {
    @Command("edithome <home> prefer")
    @Permission("essentials.edithome")
    fun CommandSender.prefer(@Argument("home", parserName = "home") home: Home) = ensurePlayer {
        if (home.isPreferred) {
            sendMessage(COMMAND_EDITHOME_FAILED_ALREADY_PREFERRED.replace("<name>", home.name))
            return
        }
        runAsync {
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
        runAsync {
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
        if (name.length > HomeManager.nameLengthLimit) {
            sendMessage(COMMAND_SETHOME_FAILED_NAME_LENGTH_LIMIT)
            return
        }
        runAsync {
            home.name = name
            home.update()
        }
        sendMessage(COMMAND_EDITHOME_RENAME.replace("<new_name>", name))
    }

    @Command("edithome <home> move")
    @Permission("essentials.edithome")
    fun CommandSender.move(@Argument("home", parserName = "home") home: Home) = ensurePlayer {
        runAsync {
            home.location = location
            home.update()
        }
        sendMessage(COMMAND_EDITHOME_MOVE.replace("<name>", home.name))
    }
}
