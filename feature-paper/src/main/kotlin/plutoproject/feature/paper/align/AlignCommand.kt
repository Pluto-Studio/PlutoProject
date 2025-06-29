package plutoproject.feature.paper.align

import org.bukkit.block.BlockFace.*
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.framework.paper.util.command.ensurePlayer
import plutoproject.framework.paper.util.entity.teleportSuspend

@Suppress("UNUSED")
object AlignCommand {
    @Command("align")
    @Permission("essentials.align")
    suspend fun CommandSender.align() = ensurePlayer {
        alignPos()
        alignView()
        sendMessage(COMMAND_ALIGN)
    }

    @Command("align pos")
    @Permission("essentials.align.pos")
    suspend fun CommandSender.pos() = ensurePlayer {
        alignPos()
        sendMessage(COMMAND_ALIGN_POS)
    }

    @Command("align view")
    @Permission("essentials.align.view")
    suspend fun CommandSender.view() = ensurePlayer {
        alignView()
        sendMessage(COMMAND_ALIGN_VIEW)
    }

    private suspend fun Player.alignPos() {
        teleportSuspend(location.toCenterLocation())
    }

    private suspend fun Player.alignView() {
        val alignYaw = when (facing) {
            NORTH -> -180.0F
            EAST -> -90.0F
            SOUTH -> 0.0F
            WEST -> 90.0F
            NORTH_EAST -> -180.0F
            NORTH_WEST -> -180.0F
            SOUTH_EAST -> 0.0F
            SOUTH_WEST -> 0.0F
            WEST_NORTH_WEST -> 90.0F
            NORTH_NORTH_WEST -> -180.0F
            NORTH_NORTH_EAST -> -180.0F
            EAST_NORTH_EAST -> -90.0F
            EAST_SOUTH_EAST -> -90.0F
            SOUTH_SOUTH_EAST -> 0.0F
            SOUTH_SOUTH_WEST -> 0.0F
            WEST_SOUTH_WEST -> 90.0F
            else -> 0.0F
        }
        val loc = location.clone().apply {
            yaw = alignYaw
            pitch = 0.0F
        }
        teleportSuspend(loc)
    }
}
