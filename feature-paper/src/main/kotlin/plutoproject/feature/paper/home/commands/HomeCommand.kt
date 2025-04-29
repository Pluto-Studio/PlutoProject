package plutoproject.feature.paper.home.commands

import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.paper.api.home.Home
import plutoproject.feature.paper.api.home.HomeManager
import plutoproject.feature.paper.home.COMMAND_HOME_SUCCEED
import plutoproject.feature.paper.home.screens.HomeListScreen
import plutoproject.framework.common.util.chat.SoundConstants
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.paper.api.interactive.startScreen
import plutoproject.framework.paper.util.command.ensurePlayer

@Suppress("UNUSED")
object HomeCommand {
    @Command("home [home]")
    @Permission("essentials.home")
    suspend fun CommandSender.home(@Argument("home", parserName = "home") home: Home?) = ensurePlayer {
        if (home == null) {
            val homes = HomeManager.list(this)
            val preferred = HomeManager.getPreferredHome(this)
            val picked = if (homes.size == 1) homes.first() else preferred
            if (picked == null) {
                startScreen(HomeListScreen(this))
                playSound(SoundConstants.UI.paging)
                return
            }
            picked.teleportSuspend(this)
            sendMessage(COMMAND_HOME_SUCCEED.replace("<name>", picked.name))
            return
        }
        home.teleportSuspend(this)
        sendMessage(COMMAND_HOME_SUCCEED.replace("<name>", home.name))
    }
}
