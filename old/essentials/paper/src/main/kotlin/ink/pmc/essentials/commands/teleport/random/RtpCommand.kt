package ink.pmc.essentials.commands.teleport.random

import ink.pmc.essentials.COMMAND_RTP_COOLDOWN
import ink.pmc.essentials.COMMAND_RTP_NOT_ENABLED
import ink.pmc.essentials.RANDOM_TELEPORT_SPECIFIC
import ink.pmc.essentials.api.teleport.random.RandomTeleportManager
import ink.pmc.framework.chat.NO_PERMISSON
import ink.pmc.framework.chat.replace
import ink.pmc.framework.command.ensurePlayer
import ink.pmc.framework.time.formatDuration
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Suppress("UNUSED")
object RtpCommand {
    @Command("rtp|tpr|randomteleport [world]")
    @Permission("essentials.rtp")
    fun CommandSender.rtp(world: World?) = ensurePlayer {
        val actualWorld = world ?: this.world
        if (actualWorld == world && !hasPermission(RANDOM_TELEPORT_SPECIFIC)) {
            sendMessage(NO_PERMISSON)
            return
        }
        if (!RandomTeleportManager.isEnabled(actualWorld)) {
            sendMessage(COMMAND_RTP_NOT_ENABLED)
            return
        }
        RandomTeleportManager.getCooldown(this)?.also {
            sendMessage(
                COMMAND_RTP_COOLDOWN
                    .replace(
                        "<time>",
                        it.remainingSeconds.toDuration(DurationUnit.SECONDS).formatDuration()
                    )
            )
            return
        }
        RandomTeleportManager.launch(this, actualWorld)
    }
}