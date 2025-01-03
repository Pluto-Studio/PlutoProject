package ink.pmc.essentials.commands

import ink.pmc.essentials.*
import ink.pmc.framework.chat.NON_PLAYER
import ink.pmc.framework.chat.replace
import ink.pmc.framework.command.ensurePlayer
import ink.pmc.framework.command.selectPlayer
import ink.pmc.framework.concurrent.sync
import org.bukkit.GameMode
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission

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

private suspend fun CommandSender.gm(gameMode: GameMode, player: Player?) = sync {
    val actualPlayer = selectPlayer(this@gm, player) ?: run {
        sendMessage(NON_PLAYER)
        return@sync
    }
    val mode = when (gameMode) {
        GameMode.SURVIVAL -> GM_SURVIVAL
        GameMode.CREATIVE -> GM_CREATIVE
        GameMode.ADVENTURE -> GM_ADVENTURE
        GameMode.SPECTATOR -> GM_SPECTATOR
    }
    if (this != actualPlayer && actualPlayer.gameMode == gameMode) {
        sendMessage(COMMAND_GM_FAILED_OTHER)
        return@sync
    }
    if (this != actualPlayer) {
        actualPlayer.gameMode = gameMode
        sendMessage(
            COMMAND_GM_OTHER_SUCCCEED
                .replace("<player>", actualPlayer.name)
                .replace("<gamemode>", mode)
        )
        return@sync
    }
    ensurePlayer {
        if (this.gameMode == gameMode) {
            sendMessage(COMMAND_GM_FAILED)
            return@sync
        }
        this.gameMode = gameMode
        sendMessage(
            COMMAND_GM_SUCCCEED
                .replace("<gamemode>", mode)
        )
    }
}