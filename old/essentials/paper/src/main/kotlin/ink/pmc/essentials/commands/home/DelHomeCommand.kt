package ink.pmc.essentials.commands.home

import ink.pmc.essentials.COMMAND_DELHOME_SUCCEED
import ink.pmc.essentials.api.home.Home
import ink.pmc.essentials.api.home.HomeManager
import ink.pmc.framework.chat.replace
import ink.pmc.framework.command.ensurePlayer
import ink.pmc.framework.concurrent.submitAsync
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission

@Suppress("UNUSED")
object DelHomeCommand {
    @Command("delhome <home>")
    @Permission("essentials.delhome")
    fun CommandSender.delhome(@Argument("home", parserName = "home") home: Home) = ensurePlayer {
        submitAsync {
            HomeManager.remove(home.id)
        }
        sendMessage(COMMAND_DELHOME_SUCCEED.replace("<name>", home.name))
    }
}