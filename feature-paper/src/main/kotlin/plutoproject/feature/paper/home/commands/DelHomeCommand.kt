package plutoproject.feature.paper.home.commands

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.paper.api.home.Home
import plutoproject.feature.paper.api.home.HomeManager
import plutoproject.feature.paper.home.COMMAND_DELHOME
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.common.util.coroutine.Loom
import plutoproject.framework.common.util.coroutine.PluginScope
import plutoproject.framework.paper.util.command.ensurePlayer

@Suppress("UNUSED")
object DelHomeCommand {
    @Command("delhome <home>")
    @Permission("plutoproject.home.command.delhome")
    fun CommandSender.delhome(@Argument("home", parserName = "home") home: Home) = ensurePlayer {
        PluginScope.launch(Dispatchers.Loom) {
            HomeManager.remove(home.id)
        }
        sendMessage(COMMAND_DELHOME.replace("<name>", home.name))
    }
}
