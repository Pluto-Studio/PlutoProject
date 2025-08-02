package plutoproject.feature.paper.randomTeleport

import org.bukkit.World
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.paper.api.randomTeleport.RandomTeleportManager
import plutoproject.framework.common.util.chat.PERMISSION_DENIED
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.common.util.time.toFormattedString
import plutoproject.framework.paper.util.command.ensurePlayer
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Suppress("UNUSED")
object RtpCommand {
    @Command("rtp|tpr|randomteleport [world]")
    @Permission("essentials.rtp")
    fun CommandSender.rtp(world: World?) = ensurePlayer {
        val actualWorld = world ?: this.world
        if (actualWorld == world && !hasPermission(RANDOM_TELEPORT_SPECIFIC_PERMISSION)) {
            sendMessage(PERMISSION_DENIED)
            return
        }
        if (!RandomTeleportManager.isEnabled(actualWorld)) {
            sendMessage(COMMAND_RTP_FAILED_WORLD_NOT_ENABLED)
            return
        }
        RandomTeleportManager.getCooldown(this)?.also {
            sendMessage(
                COMMAND_RTP_COOLDOWN.replace(
                    "<time>",
                    it.remainingSeconds.toDuration(DurationUnit.SECONDS).toFormattedString()
                )
            )
            return
        }
        RandomTeleportManager.launch(this, actualWorld)
    }
}
