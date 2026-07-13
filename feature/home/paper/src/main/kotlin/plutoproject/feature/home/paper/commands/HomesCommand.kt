package plutoproject.feature.home.paper.commands

import plutoproject.feature.home.paper.homeManager

import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.home.api.paper.HomeManager
import plutoproject.feature.home.paper.COMMAND_HOMES_FAILED_PLAYER_HAS_NO_HOME
import plutoproject.feature.home.paper.COMMAND_HOMES_FAILED_PLAYER_NOT_FOUND
import plutoproject.feature.home.paper.HOME_LOOKUP_OTHER_PERMISSION
import plutoproject.feature.home.paper.screens.HomeListScreen
import plutoproject.foundation.common.text.PERMISSION_DENIED
import plutoproject.foundation.common.text.replace
import plutoproject.feature.home.paper.startScreen
import plutoproject.feature.home.paper.lookupProfile
import plutoproject.foundation.paper.command.ensurePlayer
import plutoproject.foundation.paper.command.selectPlayer

@Suppress("UNUSED")
object HomesCommand {
    @Command("homes [player]")
    @Permission("plutoproject.home.command.homes")
    suspend fun CommandSender.homes(
        @Argument("player", suggestions = "homes-offlineplayer") player: OfflinePlayer?
    ) = ensurePlayer {
        val actualPlayer = selectPlayer(this, player)!!
        if (this != actualPlayer) {
            if (!hasPermission(HOME_LOOKUP_OTHER_PERMISSION)) {
                sendMessage(PERMISSION_DENIED)
                return
            }
            val profile = actualPlayer.lookupProfile()
            if (profile == null) {
                val nameOrUuid = actualPlayer.name ?: actualPlayer.uniqueId.toString()
                sendMessage(COMMAND_HOMES_FAILED_PLAYER_NOT_FOUND.replace("<name>", nameOrUuid))
                return
            }
            val other = Bukkit.getOfflinePlayer(profile.uuid)
            if (!homeManager.hasHome(other)) {
                sendMessage(COMMAND_HOMES_FAILED_PLAYER_HAS_NO_HOME.replace("<player>", profile.name))
                return
            }
            startScreen(HomeListScreen(other))
            return
        }
        startScreen(HomeListScreen(actualPlayer))
    }
}
