package plutoproject.feature.paper.sit.block

import kotlinx.coroutines.withContext
import org.bukkit.block.Block
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.paper.api.sit.block.BlockSit
import plutoproject.feature.paper.api.sit.block.BlockSitFinalResult.*
import plutoproject.feature.paper.api.sit.block.SitOnBlockCause
import plutoproject.feature.paper.sit.*
import plutoproject.framework.paper.util.command.ensurePlayer
import plutoproject.framework.paper.util.coroutine.coroutineContext

object SitCommand {
    @Command("sit")
    @Permission("plutoproject.sit.command.sit")
    suspend fun CommandSender.sit() = ensurePlayer {
        val target = getBlockStandingOn()
        val result = withContext(coroutineContext) {
            BlockSit.sit(this@ensurePlayer, target, cause = SitOnBlockCause.COMMAND)
        }

        val message = when (result) {
            SUCCEED -> COMMAND_SIT
            ALREADY_SITTING -> COMMAND_SIT_FAILED_ALREADY_SITTING
            SEAT_OCCUPIED -> COMMAND_SIT_FAILED_TARGET_OCCUPIED
            INVALID_SEAT -> COMMAND_SIT_FAILED_INVALID_TARGET
            BLOCKED_BY_BLOCKS -> COMMAND_SIT_FAILED_TARGET_BLOCKED_BY_BLOCKS
            CANCELLED_BY_PLUGIN -> null
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
