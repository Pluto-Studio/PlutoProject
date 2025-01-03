package ink.pmc.essentials.commands.warp

import ink.pmc.essentials.COMMAND_DELWARP_SUCCEED
import ink.pmc.essentials.COMMAND_DELWARP_SUCCEED_ALIAS
import ink.pmc.essentials.api.warp.Warp
import ink.pmc.essentials.api.warp.WarpManager
import ink.pmc.framework.chat.replace
import ink.pmc.framework.concurrent.submitAsync
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission

@Suppress("UNUSED")
object DelWarpCommand {
    @Command("delwarp <warp>")
    @Permission("essentials.delwarp")
    fun CommandSender.delwarp(@Argument("warp", parserName = "warp") warp: Warp) {
        submitAsync {
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