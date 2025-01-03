package ink.pmc.essentials.commands.home

import ink.pmc.essentials.*
import ink.pmc.essentials.api.home.HomeManager
import ink.pmc.framework.chat.replace
import ink.pmc.framework.command.ensurePlayer
import ink.pmc.framework.concurrent.submitAsync
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotation.specifier.Greedy
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission

@Suppress("UNUSED")
object SetHomeCommand {
    @Command("sethome [name]")
    @Permission("essentials.sethome")
    suspend fun CommandSender.sethome(@Greedy name: String?) = ensurePlayer {
        val list = HomeManager.list(this)
        val actualName = name ?: "home"
        if (list.size >= HomeManager.maxHomes && !hasPermission(BYPASS_HOME_LIMIT)) {
            sendMessage(COMMAND_SETHOME_FAILED_REACH_LIMIT)
            return
        }
        if (HomeManager.has(this, actualName)) {
            sendMessage(COMMAND_SETHOME_FAILED_EXISTED.replace("<name>", actualName))
            return
        }
        if (actualName.length > HomeManager.nameLengthLimit) {
            sendMessage(COMMAND_SETHOME_FAILED_LENGTN_LIMIT)
            return
        }
        submitAsync {
            HomeManager.create(this@ensurePlayer, actualName, location)
        }
        sendMessage(COMMAND_SETHOME_SUCCEED.replace("<name>", actualName))
    }
}