package plutoproject.feature.teleport.paper.commands

import plutoproject.feature.teleport.paper.teleportManager

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.afk.api.paper.AfkManager
import plutoproject.feature.teleport.api.paper.TeleportDirection
import plutoproject.feature.teleport.api.paper.TeleportManager
import plutoproject.feature.teleport.paper.*
import plutoproject.feature.teleport.paper.screens.TeleportRequestScreen
import plutoproject.kernel.api.currentModuleContext
import plutoproject.kernel.api.getServiceOrNull
import plutoproject.foundation.common.text.MESSAGE_SOUND
import plutoproject.foundation.common.text.replace
import plutoproject.foundation.common.time.toFormattedComponent
import plutoproject.capability.interactive.api.startScreen
import plutoproject.foundation.paper.command.ensurePlayer

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

    if (teleportManager.hasPendingRequest(destination)) {
        source.sendMessage(COMMAND_TPA_FAILED_TARGET_BUSY)
        return
    }

    if (direction == TeleportDirection.GO
        && teleportManager.isBlacklisted(destination.world)
        && !source.hasPermission(TELEPORT_BYPASS_WORLD_LIMIT_PERMISSION)
    ) {
        source.sendMessage(COMMAND_TPA_FAILED_NOT_ALLOWED_GO.replace("<player>", source.name))
        return
    }

    if (direction == TeleportDirection.COME
        && teleportManager.isBlacklisted(source.world)
        && !source.hasPermission(TELEPORT_BYPASS_WORLD_LIMIT_PERMISSION)
    ) {
        source.sendMessage(COMMAND_TPA_FAILED_NOT_ALLOWED_COME)
        return
    }

    val oldRequest = teleportManager.getUnfinishedRequest(source)

    oldRequest?.cancel()
    teleportManager.createRequest(source, destination, direction)

    val message = when (direction) {
        TeleportDirection.GO -> COMMAND_TPA_SUCCEED
        TeleportDirection.COME -> COMMAND_TPAHERE_SUCCEED
    }

    source.sendMessage(
        message
            .replace("<player>", destination.name)
            .replace("<expire>", teleportManager.defaultRequestOptions.expireAfter.toFormattedComponent())
    )
    if (currentModuleContext().services.getServiceOrNull<AfkManager>()?.isAfk(destination) == true) {
        source.sendMessage(COMMAND_TPA_AFK)
    }
    oldRequest?.let {
        source.sendMessage(TELEPORT_REQUEST_AUTO_CANCEL.replace("<player>", oldRequest.destination.name))
    }
    source.playSound(MESSAGE_SOUND)
}
