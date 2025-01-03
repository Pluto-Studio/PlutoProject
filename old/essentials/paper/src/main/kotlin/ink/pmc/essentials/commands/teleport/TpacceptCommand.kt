package ink.pmc.essentials.commands.teleport

import ink.pmc.essentials.*
import ink.pmc.essentials.api.teleport.TeleportManager
import ink.pmc.essentials.api.teleport.TeleportRequest
import ink.pmc.framework.chat.replace
import ink.pmc.framework.command.ensurePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission

private enum class Operation {
    ACCEPT, DENY
}

@Suppress("UNUSED")
object TpacceptCommand {
    @Command("tpaccept|tpyes [request]")
    @Permission("essentials.tpaccept")
    fun CommandSender.tpaccept(
        @Argument("request", parserName = "tp-request") request: TeleportRequest?
    ) = ensurePlayer {
        handleOperation(request, Operation.ACCEPT)
    }

    @Command("tpdeny|tpno|tpdecline [request]")
    @Permission("essentials.tpdeny")
    fun CommandSender.tpdeny(
        @Argument("request", parserName = "tp-request") request: TeleportRequest?
    ) = ensurePlayer {
        handleOperation(request, Operation.DENY)
    }
}

private fun CommandSender.handleOperation(request: TeleportRequest?, type: Operation) {
    val actualRequest = request ?: TeleportManager.getPendingRequest(this as Player) ?: run {
        this.sendMessage(COMMAND_TPACCEPT_FAILED_NO_PENDING)
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