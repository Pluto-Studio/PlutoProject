package plutoproject.feature.sit.paper.block

import kotlinx.coroutines.withContext
import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import org.bukkit.block.Block
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.sit.api.paper.block.BlockSit
import plutoproject.feature.sit.api.paper.block.BlockSitFinalResult.*
import plutoproject.feature.sit.api.paper.block.SitOnBlockCause
import plutoproject.feature.sit.paper.*
import plutoproject.foundation.paper.command.ensurePlayer
import plutoproject.kernel.api.currentModuleContext
import plutoproject.kernel.api.koinInject
import plutoproject.kernel.api.paper.PaperModuleContext

object SitCommand {
    private val blockSit by koinInject<BlockSit>()

    @Command("sit")
    @Permission("plutoproject.sit.command.sit")
    suspend fun CommandSender.sit() = ensurePlayer {
        val target = getBlockStandingOn()
        val result = withContext((currentModuleContext() as PaperModuleContext).plugin.minecraftDispatcher) {
            blockSit.sit(this@ensurePlayer, target, cause = SitOnBlockCause.COMMAND)
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
