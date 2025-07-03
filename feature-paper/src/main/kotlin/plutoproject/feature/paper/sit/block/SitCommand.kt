package plutoproject.feature.paper.sit.block

import org.bukkit.block.Block
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Command
import plutoproject.feature.paper.api.sit.block.BlockSit
import plutoproject.feature.paper.api.sit.SitFinalResult.*
import plutoproject.feature.paper.sit.*
import plutoproject.framework.paper.util.command.ensurePlayer
import plutoproject.framework.paper.util.coroutine.withSync

object SitCommand {
    @Command("sit")
    suspend fun CommandSender.sit() = ensurePlayer {
        val target = getBlockStandingOn()
        val result = withSync {
            BlockSit.sit(this@ensurePlayer, target)
        }

        val message = when (result) {
            SUCCEED -> COMMAND_SIT
            FAILED_ALREADY_SITTING -> COMMAND_SIT_FAILED_ALREADY_SITTING
            FAILED_TARGET_OCCUPIED -> COMMAND_SIT_FAILED_TARGET_OCCUPIED
            FAILED_INVALID_TARGET -> COMMAND_SIT_FAILED_INVALID_TARGET
            FAILED_TARGET_BLOCKED_BY_BLOCKS -> COMMAND_SIT_FAILED_TARGET_BLOCKED_BY_BLOCKS
            FAILED_CANCELLED_BY_PLUGIN -> null
        }

        message?.let { sendMessage(it) }
    }

    private fun Player.getBlockStandingOn(): Block {
        if (location.y % 1 != 0.0) {
            val startY = location.blockY
            val endY = startY - 2
            for (y in startY downTo endY) {
                val block = world.getBlockAt(location.blockX, y, location.blockZ)
                if (block.type.isAir) continue
                return block
            }
        }
        return location.clone().subtract(0.0, 1.0, 0.0).block
    }
}
