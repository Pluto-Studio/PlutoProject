package plutoproject.feature.paper.warp.commands

import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.paper.api.warp.WarpManager
import plutoproject.feature.paper.warp.COMMAND_SPAWN_FAILED_NOT_SET
import plutoproject.feature.paper.warp.COMMAND_WARP_SUCCEED
import plutoproject.feature.paper.warp.COMMAND_WARP_SUCCEED_ALIAS
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.paper.util.command.ensurePlayer

@Suppress("UNUSED")
object SpawnCommand {
    @Command("spawn")
    @Permission("plutoproject.warp.command.spawn")
    suspend fun CommandSender.spawn() = ensurePlayer {
        val spawn = WarpManager.getPreferredSpawn(this)
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
