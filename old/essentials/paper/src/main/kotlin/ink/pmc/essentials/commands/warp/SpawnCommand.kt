package ink.pmc.essentials.commands.warp

import ink.pmc.essentials.COMMAND_SPAWN_FAILED_NOT_SET
import ink.pmc.essentials.COMMAND_WARP_SUCCEED
import ink.pmc.essentials.COMMAND_WARP_SUCCEED_ALIAS
import ink.pmc.essentials.api.warp.WarpManager
import ink.pmc.framework.chat.replace
import ink.pmc.framework.command.ensurePlayer
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission

@Suppress("UNUSED")
object SpawnCommand {
    @Command("spawn")
    @Permission("essentials.spawn")
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