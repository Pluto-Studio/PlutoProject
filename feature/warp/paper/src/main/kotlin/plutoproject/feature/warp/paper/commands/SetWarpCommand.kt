package plutoproject.feature.warp.paper.commands

import plutoproject.feature.warp.paper.warpManager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotation.specifier.Quoted
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.warp.api.paper.WarpManager
import plutoproject.feature.warp.paper.COMMAND_SETWARP_FAILED_EXISTED
import plutoproject.feature.warp.paper.COMMAND_SETWARP_SUCCEED
import plutoproject.feature.warp.paper.COMMAND_SETWARP_SUCCEED_ALIAS
import plutoproject.feature.warp.paper.commandSetwarpFailedLengthLimit
import plutoproject.foundation.common.text.replace
import plutoproject.feature.warp.paper.moduleScope
import plutoproject.foundation.paper.command.ensurePlayer

@Suppress("UNUSED")
@Permission("essentials.setwarp")
object SetWarpCommand {
    @Command("setwarp <name> [alias]")
    @Permission("essentials.setwarp")
    suspend fun CommandSender.setWarp(
        @Argument("name") @Quoted name: String,
        @Argument("alias") @Quoted alias: String?
    ) = ensurePlayer {
        if (warpManager.has(name)) {
            sendMessage(COMMAND_SETWARP_FAILED_EXISTED.replace("<name>", name))
            return
        }
        if (name.length > warpManager.nameLengthLimit) {
            sendMessage(commandSetwarpFailedLengthLimit)
            return
        }
        moduleScope.launch(Dispatchers.IO) {
            warpManager.create(name, location, alias)
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
