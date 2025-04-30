package plutoproject.feature.paper.home.commands

import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.paper.api.home.HomeManager
import plutoproject.feature.paper.home.COMMAND_HOMES_PLAYER_HAS_NO_HOME
import plutoproject.feature.paper.home.HOME_LOOKUP_OTHER_PERMISSION
import plutoproject.feature.paper.home.screens.HomeListScreen
import plutoproject.framework.common.util.chat.PERMISSION_DENIED
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.paper.api.interactive.startScreen
import plutoproject.framework.paper.util.command.ensurePlayer
import plutoproject.framework.paper.util.command.selectPlayer

@Suppress("UNUSED")
object HomesCommand {
    @Command("homes [player]")
    @Permission("essentials.homes")
    suspend fun CommandSender.homes(
        @Argument("player", suggestions = "homes-offlineplayer") player: OfflinePlayer?
    ) = ensurePlayer {
        val actualPlayer = selectPlayer(this, player)!!
        if (this != actualPlayer) {
            if (!hasPermission(HOME_LOOKUP_OTHER_PERMISSION)) {
                sendMessage(PERMISSION_DENIED)
                return
            }
            if (!HomeManager.hasHome(actualPlayer)) {
                sendMessage(
                    COMMAND_HOMES_PLAYER_HAS_NO_HOME.replace(
                        "<player>",
                        actualPlayer.name ?: actualPlayer.uniqueId
                    )
                )
                return
            }
        }
        startScreen(HomeListScreen(actualPlayer))
    }
}
