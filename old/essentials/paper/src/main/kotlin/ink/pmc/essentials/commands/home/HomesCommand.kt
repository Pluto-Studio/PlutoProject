package ink.pmc.essentials.commands.home

import ink.pmc.essentials.HOMES_OTHER
import ink.pmc.essentials.api.home.HomeManager
import ink.pmc.essentials.screens.home.HomeListScreen
import ink.pmc.framework.startScreen
import ink.pmc.framework.chat.NO_PERMISSON
import ink.pmc.framework.chat.PLAYER_HAS_NO_HOME
import ink.pmc.framework.chat.replace
import ink.pmc.framework.command.ensurePlayer
import ink.pmc.framework.command.selectPlayer
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission

@Suppress("UNUSED")
object HomesCommand {
    @Command("homes [player]")
    @Permission("essentials.homes")
    suspend fun CommandSender.homes(
        @Argument("player", suggestions = "homes-offlineplayer") player: OfflinePlayer?
    ) = ensurePlayer {
        val actualPlayer = selectPlayer(this, player)!!
        if (this != actualPlayer) {
            if (!hasPermission(HOMES_OTHER)) {
                sendMessage(NO_PERMISSON)
                return
            }
            if (!HomeManager.hasHome(actualPlayer)) {
                sendMessage(
                    PLAYER_HAS_NO_HOME.replace(
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