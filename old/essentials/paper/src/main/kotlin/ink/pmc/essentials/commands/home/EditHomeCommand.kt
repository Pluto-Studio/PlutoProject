package ink.pmc.essentials.commands.home

import ink.pmc.essentials.*
import ink.pmc.essentials.api.home.Home
import ink.pmc.essentials.api.home.HomeManager
import ink.pmc.framework.chat.replace
import ink.pmc.framework.command.ensurePlayer
import ink.pmc.framework.concurrent.submitAsync
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotation.specifier.Greedy
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission

@Suppress("UNUSED")
object EditHomeCommand {
    @Command("edithome <home> prefer")
    @Permission("essentials.edithome")
    fun CommandSender.prefer(@Argument("home", parserName = "home") home: Home) = ensurePlayer {
        if (home.isPreferred) {
            sendMessage(COMMAND_EDITHOME_ALREADY_PREFERRED.replace("<name>", home.name))
            return
        }
        submitAsync {
            home.setPreferred(true)
        }
        sendMessage(COMMAND_EDITHOME_PREFER_SUCCEED.replace("<name>", home.name))
    }

    @Command("edithome <home> star")
    @Permission("essentials.edithome")
    fun CommandSender.star(@Argument("home", parserName = "home") home: Home) = ensurePlayer {
        if (home.isStarred) {
            sendMessage(COMMAND_EDITHOME_ALREADY_STARRED.replace("<name>", home.name))
            return
        }
        submitAsync {
            home.isStarred = true
            home.update()
        }
        sendMessage(COMMAND_EDITHOME_STAR_SUCCEED.replace("<name>", home.name))
    }

    @Command("edithome <home> rename <name>")
    @Permission("essentials.edithome")
    fun CommandSender.rename(
        @Argument("home", parserName = "home") home: Home,
        @Argument("name") @Greedy name: String
    ) = ensurePlayer {
        if (name.length > HomeManager.nameLengthLimit) {
            sendMessage(COMMAND_SETHOME_FAILED_LENGTN_LIMIT)
            return
        }
        submitAsync {
            home.name = name
            home.update()
        }
        sendMessage(COMMAND_EDITHOME_RENAME_SUCCEED.replace("<new_name>", name))
    }

    @Command("edithome <home> move")
    @Permission("essentials.edithome")
    fun CommandSender.move(@Argument("home", parserName = "home") home: Home) = ensurePlayer {
        submitAsync {
            home.location = location
            home.update()
        }
        sendMessage(COMMAND_EDITHOME_MOVE_SUCCEED.replace("<name>", home.name))
    }
}