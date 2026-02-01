package plutoproject.feature.paper.warp.commands

import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.paper.api.warp.Warp
import plutoproject.feature.paper.warp.COMMAND_WARP_SUCCEED
import plutoproject.feature.paper.warp.COMMAND_WARP_SUCCEED_ALIAS
import plutoproject.feature.paper.warp.screens.WarpListScreen
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.paper.api.interactive.startScreen
import plutoproject.framework.paper.util.command.ensurePlayer

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
