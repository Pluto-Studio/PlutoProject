package plutoproject.feature.paper.gm

import kotlinx.coroutines.withContext
import org.bukkit.GameMode
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.framework.common.util.chat.PLAYER_ONLY_COMMAND
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.paper.util.command.ensurePlayer
import plutoproject.framework.paper.util.command.selectPlayer
import plutoproject.framework.paper.util.coroutine.coroutineContext

private suspend fun CommandSender.gm(gameMode: GameMode, player: Player?) = withContext(server.coroutineContext) {
    val actualPlayer = selectPlayer(this@gm, player) ?: run {
        sendMessage(PLAYER_ONLY_COMMAND)
        return@withContext
    }
    val mode = when (gameMode) {
        GameMode.SURVIVAL -> SURVIVAL
        GameMode.CREATIVE -> CREATIVE
        GameMode.ADVENTURE -> ADVENTURE
        GameMode.SPECTATOR -> SPECTATOR
    }
    if (this != actualPlayer && actualPlayer.gameMode == gameMode) {
        sendMessage(COMMAND_GM_OTHER_FAILED_SAME_GAMEMODE)
        return@withContext
    }
    if (this != actualPlayer) {
        actualPlayer.gameMode = gameMode
        sendMessage(
            COMMAND_GM_OTHER
                .replace("<player>", actualPlayer.name)
                .replace("<gamemode>", mode)
        )
        return@withContext
    }
    ensurePlayer {
        if (this.gameMode == gameMode) {
            sendMessage(COMMAND_GM_FAILED_SAME_GAMEMODE)
            return@withContext
        }
        this.gameMode = gameMode
        sendMessage(COMMAND_GM.replace("<gamemode>", mode))
    }
}

@Suppress("UNUSED")
object GmCommand {
    @Command("gm survival|s|0 [player]")
    @Permission("essentials.gm.survival")
    suspend fun CommandSender.survival(@Argument("player") player: Player?) = gm(GameMode.SURVIVAL, player)

    @Command("gms [player]")
    @Permission("essentials.gm.survival")
    suspend fun CommandSender.gms(@Argument("player") player: Player?) = gm(GameMode.SURVIVAL, player)

    @Command("gm creative|c|1 [player]")
    @Permission("essentials.gm.creative")
    suspend fun CommandSender.creative(@Argument("player") player: Player?) = gm(GameMode.CREATIVE, player)

    @Command("gmc [player]")
    @Permission("essentials.gm.creative")
    suspend fun CommandSender.gmc(@Argument("player") player: Player?) = gm(GameMode.CREATIVE, player)

    @Command("gm adventure|a|2 [player]")
    @Permission("essentials.gm.adventure")
    suspend fun CommandSender.adventure(@Argument("player") player: Player?) = gm(GameMode.ADVENTURE, player)

    @Command("gma [player]")
    @Permission("essentials.gm.adventure")
    suspend fun CommandSender.gma(@Argument("player") player: Player?) = gm(GameMode.ADVENTURE, player)

    @Command("gm spectator|sp|3 [player]")
    @Permission("essentials.gm.spectator")
    suspend fun CommandSender.spectator(@Argument("player") player: Player?) = gm(GameMode.SPECTATOR, player)

    @Command("gmsp [player]")
    @Permission("essentials.gm.spectator")
    suspend fun CommandSender.gmsp(@Argument("player") player: Player?) = gm(GameMode.SPECTATOR, player)
}
