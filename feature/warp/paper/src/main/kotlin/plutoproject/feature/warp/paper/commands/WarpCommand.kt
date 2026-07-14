package plutoproject.feature.warp.paper.commands

import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.warp.api.paper.Warp
import plutoproject.feature.warp.paper.COMMAND_WARP_SUCCEED
import plutoproject.feature.warp.paper.COMMAND_WARP_SUCCEED_ALIAS
import plutoproject.feature.warp.paper.screens.WarpListScreen
import plutoproject.foundation.common.text.replace
import plutoproject.feature.warp.paper.startScreen
import plutoproject.foundation.paper.command.ensurePlayer

@Suppress("UNUSED")
object WarpCommand {
    @Command("warp [warp]")
    @Permission("plutoproject.warp.command.warp")
    suspend fun CommandSender.warp(@Argument("warp", parserName = "warp") warp: Warp?) = ensurePlayer {
        if (warp == null) {
            startScreen(WarpListScreen())
            return
        }
        warp.teleportSuspend(this)
        if (warp.alias == null) {
            sendMessage(COMMAND_WARP_SUCCEED.replace("<name>", warp.name))
        } else {
            sendMessage(
                COMMAND_WARP_SUCCEED_ALIAS
                    .replace("<name>", warp.name)
                    .replace("<alias>", warp.alias!!)
            )
        }
    }
}
