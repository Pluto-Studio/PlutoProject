package plutoproject.feature.home.paper.commands

import plutoproject.feature.home.paper.homeManager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotation.specifier.Greedy
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.home.api.paper.HomeManager
import plutoproject.feature.home.paper.*
import plutoproject.foundation.common.text.replace
import plutoproject.feature.home.paper.moduleScope
import plutoproject.foundation.paper.command.ensurePlayer

@Suppress("UNUSED")
object SetHomeCommand {
    @Command("sethome [name]")
    @Permission("plutoproject.home.command.sethome")
    suspend fun CommandSender.sethome(@Greedy name: String?) = ensurePlayer {
        val list = homeManager.list(this)
        val preferredHome = homeManager.getPreferredHome(this)
        val isDefaultHome = name == null && preferredHome == null
        val actualName = name ?: "home"
        val maxHomes = homeManager.maxHomes(this)
        if (list.size >= maxHomes && !hasPermission(HOME_LIMIT_BYPASS_PERMISSION)) {
            sendMessage(COMMAND_SETHOME_FAILED_AMOUNT_LIMIT.replace("<max_homes>", maxHomes.toString()))
            return
        }
        if (homeManager.has(this, actualName)) {
            sendMessage(COMMAND_SETHOME_FAILED_EXISTED.replace("<name>", actualName))
            return
        }
        if (actualName.length > homeManager.nameLengthLimit) {
            sendMessage(COMMAND_SETHOME_FAILED_NAME_LENGTH_LIMIT)
            return
        }
        moduleScope.launch(Dispatchers.IO) {
            val home = homeManager.create(this@ensurePlayer, actualName, location)
            if (isDefaultHome) {
                home.setPreferred(true)
                home.update()
            }
        }
        sendMessage(COMMAND_SETHOME.replace("<name>", actualName))
    }
}
