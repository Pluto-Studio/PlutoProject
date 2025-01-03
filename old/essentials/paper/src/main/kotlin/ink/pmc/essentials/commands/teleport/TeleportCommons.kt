package ink.pmc.essentials.commands.teleport

import ink.pmc.essentials.COMMAND_TPACCEPT_FAILED_NO_PENDING
import ink.pmc.essentials.COMMAND_TPACCEPT_FAILED_NO_REQUEST
import ink.pmc.essentials.COMMAND_TPACCEPT_FAILED_NO_REQUEST_ID
import ink.pmc.essentials.api.teleport.TeleportManager
import ink.pmc.essentials.api.teleport.TeleportRequest
import ink.pmc.framework.chat.replace
import ink.pmc.framework.platform.paper
import ink.pmc.framework.player.uuidOrNull
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.exception.ExceptionHandler
import org.incendo.cloud.annotations.parser.Parser
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.parser.standard.StringParser
import kotlin.jvm.optionals.getOrNull

@Suppress("UNUSED", "UNUSED_PARAMETER")
object TeleportCommons {
    @Suggestions("tp-request-players")
    fun tpRequests(context: CommandContext<CommandSender>, input: CommandInput): List<String> {
        return paper.onlinePlayers.map { it.name }
    }

    @Parser(name = "tp-request", suggestions = "tp-request-players")
    fun tpRequest(context: CommandContext<CommandSender>, input: CommandInput): TeleportRequest {
        val player = context.sender() as Player
        val stringParser = StringParser.quotedStringParser<CommandSender>().parser()
        val string = stringParser.parse(context, input).parsedValue().getOrNull() ?: error("Unable to parse request")
        val uuid = string.uuidOrNull
        val source = paper.getPlayer(string)
        val request = when {
            uuid != null -> TeleportManager.getRequest(uuid) ?: throw TeleportRequestNotFound()
            source != null -> TeleportManager.getUnfinishedRequest(source)
                ?: throw NoRequestFromPlayerException(source.name)

            else -> throw NoRequestException()
        }
        if (request.destination != player) throw NoRequestFromPlayerException(request.source.name)
        return request
    }

    @ExceptionHandler(NoRequestException::class)
    fun CommandSender.noRequest() {
        sendMessage(COMMAND_TPACCEPT_FAILED_NO_PENDING)
    }

    @ExceptionHandler(TeleportRequestNotFound::class)
    fun CommandSender.teleportRequestNotFound() {
        sendMessage(COMMAND_TPACCEPT_FAILED_NO_REQUEST_ID)
    }

    @ExceptionHandler(NoRequestFromPlayerException::class)
    fun CommandSender.noRequestFormPlayer(exception: NoRequestFromPlayerException) {
        sendMessage(COMMAND_TPACCEPT_FAILED_NO_REQUEST.replace("<player>", exception.player))
    }
}

class NoRequestException : Exception()

class TeleportRequestNotFound : Exception()

class NoRequestFromPlayerException(val player: String) : Exception()