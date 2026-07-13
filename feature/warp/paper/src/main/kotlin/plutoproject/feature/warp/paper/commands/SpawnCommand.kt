package plutoproject.feature.warp.paper.commands

import plutoproject.feature.warp.paper.warpManager

import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.warp.api.paper.WarpManager
import plutoproject.feature.warp.paper.COMMAND_SPAWN_FAILED_NOT_SET
import plutoproject.feature.warp.paper.COMMAND_WARP_SUCCEED
import plutoproject.feature.warp.paper.COMMAND_WARP_SUCCEED_ALIAS
import plutoproject.foundation.common.text.replace
import plutoproject.foundation.paper.command.ensurePlayer

@Suppress("UNUSED")
object SpawnCommand {
    @Command("spawn")
    @Permission("plutoproject.warp.command.spawn")
    suspend fun CommandSender.spawn() = ensurePlayer {
        val spawn = warpManager.getPreferredSpawn(this)
        if (spawn == null) {
            sendMessage(COMMAND_SPAWN_FAILED_NOT_SET)
            return
        }
        spawn.teleport(this)
        if (spawn.alias == null) {
            sendMessage(COMMAND_WARP_SUCCEED.replace("<name>", spawn.name))
        } else {
            sendMessage(
                COMMAND_WARP_SUCCEED_ALIAS
                    .replace("<name>", spawn.name)
                    .replace("<alias>", spawn.alias)
            )
        }
    }
}
