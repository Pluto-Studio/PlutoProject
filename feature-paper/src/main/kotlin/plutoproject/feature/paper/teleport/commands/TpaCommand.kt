package plutoproject.feature.paper.teleport.commands

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.paper.api.afk.AfkManager
import plutoproject.feature.paper.api.teleport.TeleportDirection
import plutoproject.feature.paper.api.teleport.TeleportManager
import plutoproject.feature.paper.teleport.*
import plutoproject.feature.paper.teleport.screens.TeleportRequestScreen
import plutoproject.framework.common.api.feature.FeatureManager
import plutoproject.framework.common.util.chat.MESSAGE_SOUND
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.common.util.time.toFormattedComponent
import plutoproject.framework.paper.api.interactive.startScreen
import plutoproject.framework.paper.util.command.ensurePlayer

@Suppress("UNUSED")
object TpaCommand {
    @Command("tpa [player]")
    @Permission("plutoproject.teleport.command.tpa")
    fun tpa(sender: CommandSender, @Argument("player") player: Player? = null) = sender.ensurePlayer {
        handleTpa(this, player, TeleportDirection.GO)
    }

    @Command("tpahere [player]")
    @Permission("plutoproject.teleport.command.tpahere")
    fun tpahere(sender: CommandSender, @Argument("player") player: Player? = null) = sender.ensurePlayer {
        handleTpa(this, player, TeleportDirection.COME)
    }
}

private fun handleTpa(source: Player, destination: Player?, direction: TeleportDirection) {
    if (destination == null) {
        source.startScreen(TeleportRequestScreen())
        return
    }

    if (destination == source) {
        source.sendMessage(COMMAND_TPA_FAILED_SELF)
        return
    }

    if (!source.hasPermission("plutoproject.teleport.command.tpa.ignore_destination_permission")
        && !destination.hasPermission("plutoproject.teleport.as_destination")
    ) {
        source.sendMessage(COMMAND_TPA_FAILED_DESTINATION_NOT_PERMITTED)
        return
    }

    if (TeleportManager.hasPendingRequest(destination)) {
        source.sendMessage(COMMAND_TPA_FAILED_TARGET_BUSY)
        return
    }

    if (direction == TeleportDirection.GO
        && TeleportManager.isBlacklisted(destination.world)
        && !source.hasPermission(TELEPORT_BYPASS_WORLD_LIMIT_PERMISSION)
    ) {
        source.sendMessage(COMMAND_TPA_FAILED_NOT_ALLOWED_GO.replace("<player>", source.name))
        return
    }

    if (direction == TeleportDirection.COME
        && TeleportManager.isBlacklisted(source.world)
        && !source.hasPermission(TELEPORT_BYPASS_WORLD_LIMIT_PERMISSION)
    ) {
        source.sendMessage(COMMAND_TPA_FAILED_NOT_ALLOWED_COME)
        return
    }

    val oldRequest = TeleportManager.getUnfinishedRequest(source)

    oldRequest?.cancel()
    TeleportManager.createRequest(source, destination, direction)

    val message = when (direction) {
        TeleportDirection.GO -> COMMAND_TPA_SUCCEED
        TeleportDirection.COME -> COMMAND_TPAHERE_SUCCEED
    }

    source.sendMessage(
        message
            .replace("<player>", destination.name)
            .replace("<expire>", TeleportManager.defaultRequestOptions.expireAfter.toFormattedComponent())
    )
    if (FeatureManager.isEnabled("afk") && AfkManager.isAfk(destination)) {
        source.sendMessage(COMMAND_TPA_AFK)
    }
    oldRequest?.let {
        source.sendMessage(TELEPORT_REQUEST_AUTO_CANCEL.replace("<player>", oldRequest.destination.name))
    }
    source.playSound(MESSAGE_SOUND)
}
