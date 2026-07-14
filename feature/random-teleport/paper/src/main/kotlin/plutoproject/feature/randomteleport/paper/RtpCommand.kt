package plutoproject.feature.randomteleport.paper

import org.bukkit.World
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.randomteleport.api.paper.RandomTeleportManager
import plutoproject.foundation.common.text.PERMISSION_DENIED
import plutoproject.foundation.common.text.replace
import plutoproject.foundation.common.time.toFormattedString
import plutoproject.foundation.paper.command.ensurePlayer
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Suppress("UNUSED")
object RtpCommand {
    @Command("rtp|tpr|randomteleport [world]")
    @Permission("plutoproject.rtp.command.rtp")
    fun CommandSender.rtp(world: World?) = ensurePlayer {
        val actualWorld = world ?: this.world
        if (actualWorld == world && !hasPermission(RANDOM_TELEPORT_SPECIFIC_PERMISSION)) {
            sendMessage(PERMISSION_DENIED)
            return
        }
        if (!randomTeleportManager.isEnabled(actualWorld)) {
            sendMessage(COMMAND_RTP_FAILED_WORLD_NOT_ENABLED)
            return
        }
        randomTeleportManager.getCooldown(this)?.also {
            sendMessage(
                COMMAND_RTP_COOLDOWN.replace(
                    "<time>",
                    it.remainingSeconds.toDuration(DurationUnit.SECONDS).toFormattedString()
                )
            )
            return
        }
        randomTeleportManager.launch(this, actualWorld)
    }
}
