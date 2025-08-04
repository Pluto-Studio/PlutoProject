package plutoproject.feature.paper.warp.commands

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.paper.api.warp.Warp
import plutoproject.feature.paper.api.warp.WarpManager
import plutoproject.feature.paper.warp.COMMAND_DELWARP_SUCCEED
import plutoproject.feature.paper.warp.COMMAND_DELWARP_SUCCEED_ALIAS
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.common.util.coroutine.Loom
import plutoproject.framework.common.util.coroutine.PluginScope

@Suppress("UNUSED")
object DelWarpCommand {
    @Command("delwarp <warp>")
    @Permission("essentials.delwarp")
    fun CommandSender.delwarp(@Argument("warp", parserName = "warp") warp: Warp) {
        PluginScope.launch(Dispatchers.Loom) {
            WarpManager.remove(warp.id)
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
