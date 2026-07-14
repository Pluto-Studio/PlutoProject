package plutoproject.feature.warp.paper.commands

import plutoproject.feature.warp.paper.warpManager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.warp.api.paper.Warp
import plutoproject.feature.warp.api.paper.WarpManager
import plutoproject.feature.warp.paper.COMMAND_DELWARP_SUCCEED
import plutoproject.feature.warp.paper.COMMAND_DELWARP_SUCCEED_ALIAS
import plutoproject.foundation.common.text.replace
import plutoproject.feature.warp.paper.moduleScope

@Suppress("UNUSED")
object DelWarpCommand {
    @Command("delwarp <warp>")
    @Permission("essentials.delwarp")
    fun CommandSender.delwarp(@Argument("warp", parserName = "warp") warp: Warp) {
        moduleScope.launch(Dispatchers.IO) {
            warpManager.remove(warp.id)
        }
        if (warp.alias == null) {
            sendMessage(COMMAND_DELWARP_SUCCEED.replace("<name>", warp.name))
        } else {
            sendMessage(
                COMMAND_DELWARP_SUCCEED_ALIAS
                    .replace("<name>", warp.name)
                    .replace("<alias>", warp.alias!!)
            )
        }
    }
}
