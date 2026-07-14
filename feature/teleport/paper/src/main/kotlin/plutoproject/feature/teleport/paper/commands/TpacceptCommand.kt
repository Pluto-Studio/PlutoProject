package plutoproject.feature.teleport.paper.commands

import plutoproject.feature.teleport.paper.teleportManager

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.teleport.api.paper.TeleportManager
import plutoproject.feature.teleport.api.paper.TeleportRequest
import plutoproject.feature.teleport.paper.*
import plutoproject.foundation.common.text.replace
import plutoproject.foundation.paper.command.ensurePlayer

private enum class Operation {
    ACCEPT, DENY
}

@Suppress("UNUSED")
object TpacceptCommand {
    @Command("tpaccept|tpyes [request]")
    @Permission("plutoproject.teleport.command.tpaccept")
    fun CommandSender.tpaccept(
        @Argument("request", parserName = "tp-request") request: TeleportRequest?
    ) = ensurePlayer {
        handleOperation(request, Operation.ACCEPT)
    }

    @Command("tpdeny|tpno|tpdecline [request]")
    @Permission("plutoproject.teleport.command.tpdeny")
    fun CommandSender.tpdeny(
        @Argument("request", parserName = "tp-request") request: TeleportRequest?
    ) = ensurePlayer {
        handleOperation(request, Operation.DENY)
    }
}

private fun CommandSender.handleOperation(request: TeleportRequest?, type: Operation) {
    val actualRequest = request ?: teleportManager.getPendingRequest(this as Player) ?: run {
        this.sendMessage(COMMAND_TPACCEPT_FAILED_NO_PENDING)
        return
    }

    if (actualRequest.isFinished) {
        this.sendMessage(COMMAND_TPACCEPT_FAILED_NO_REQUEST_ID.replace("<player>", actualRequest.source.name))
        return
    }

    val choice = when (type) {
        Operation.ACCEPT -> {
            actualRequest.accept()
            playSound(TELEPORT_SUCCEED_SOUND)
            COMMAND_TPACCEPT_SUCCEED
        }

        Operation.DENY -> {
            actualRequest.deny()
            playSound(TELEPORT_REQUEST_DENIED_SOUND)
            COMMAND_TPDENY_SUCCEED
        }
    }
    sendMessage(choice.replace("<player>", actualRequest.source.name))
}
