package plutoproject.feature.paper.warp.commands

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotation.specifier.Quoted
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.paper.api.warp.WarpManager
import plutoproject.feature.paper.warp.COMMAND_SETWARP_FAILED_EXISTED
import plutoproject.feature.paper.warp.COMMAND_SETWARP_SUCCEED
import plutoproject.feature.paper.warp.COMMAND_SETWARP_SUCCEED_ALIAS
import plutoproject.feature.paper.warp.commandSetwarpFailedLengthLimit
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.common.util.coroutine.Loom
import plutoproject.framework.common.util.coroutine.PluginScope
import plutoproject.framework.paper.util.command.ensurePlayer

@Suppress("UNUSED")
@Permission("essentials.setwarp")
object SetWarpCommand {
    @Command("setwarp <name> [alias]")
    @Permission("essentials.setwarp")
    suspend fun CommandSender.setWarp(
        @Argument("name") @Quoted name: String,
        @Argument("alias") @Quoted alias: String?
    ) = ensurePlayer {
        if (WarpManager.has(name)) {
            sendMessage(COMMAND_SETWARP_FAILED_EXISTED.replace("<name>", name))
            return
        }
        if (name.length > WarpManager.nameLengthLimit) {
            sendMessage(commandSetwarpFailedLengthLimit)
            return
        }
        PluginScope.launch(Dispatchers.Loom) {
            WarpManager.create(name, location, alias)
        }
        if (alias == null) {
            sendMessage(COMMAND_SETWARP_SUCCEED.replace("<name>", name))
        } else {
            sendMessage(
                COMMAND_SETWARP_SUCCEED_ALIAS
                    .replace("<name>", name)
                    .replace("<alias>", alias)
            )
        }
    }
}
