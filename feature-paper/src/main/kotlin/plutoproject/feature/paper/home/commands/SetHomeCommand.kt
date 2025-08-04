package plutoproject.feature.paper.home.commands

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotation.specifier.Greedy
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.paper.api.home.HomeManager
import plutoproject.feature.paper.home.*
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.common.util.coroutine.Loom
import plutoproject.framework.common.util.coroutine.PluginScope
import plutoproject.framework.paper.util.command.ensurePlayer

@Suppress("UNUSED")
object SetHomeCommand {
    @Command("sethome [name]")
    @Permission("essentials.sethome")
    suspend fun CommandSender.sethome(@Greedy name: String?) = ensurePlayer {
        val list = HomeManager.list(this)
        val preferredHome = HomeManager.getPreferredHome(this)
        val isDefaultHome = name == null && preferredHome == null
        val actualName = name ?: "home"
        if (list.size >= HomeManager.maxHomes && !hasPermission(HOME_LIMIT_BYPASS_PERMISSION)) {
            sendMessage(COMMAND_SETHOME_FAILED_AMOUNT_LIMIT)
            return
        }
        if (HomeManager.has(this, actualName)) {
            sendMessage(COMMAND_SETHOME_FAILED_EXISTED.replace("<name>", actualName))
            return
        }
        if (actualName.length > HomeManager.nameLengthLimit) {
            sendMessage(COMMAND_SETHOME_FAILED_NAME_LENGTH_LIMIT)
            return
        }
        PluginScope.launch(Dispatchers.Loom) {
            val home = HomeManager.create(this@ensurePlayer, actualName, location)
            if (isDefaultHome) {
                home.setPreferred(true)
                home.update()
            }
        }
        sendMessage(COMMAND_SETHOME.replace("<name>", actualName))
    }
}
