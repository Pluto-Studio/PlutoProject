package plutoproject.feature.home.paper.commands

import plutoproject.feature.home.paper.homeManager

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.exception.ExceptionHandler
import org.incendo.cloud.annotations.parser.Parser
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.parser.standard.StringParser
import plutoproject.feature.home.api.paper.Home
import plutoproject.feature.home.api.paper.HomeManager
import plutoproject.feature.home.paper.COMMAND_HOME_FAILED_NOT_EXISTED
import plutoproject.foundation.common.text.replace
import kotlin.jvm.optionals.getOrNull

@Suppress("UNUSED", "UNUSED_PARAMETER")
object HomeCommons {
    @Suggestions("homes")
    suspend fun homes(context: CommandContext<CommandSender>, input: CommandInput): List<String> {
        val sender = context.sender()
        if (sender !is Player) return emptyList()
        return homeManager.list(sender).map { it.name }
    }

    @Parser(name = "home", suggestions = "homes")
    suspend fun home(context: CommandContext<CommandSender>, input: CommandInput): Home {
        val parser = StringParser.greedyStringParser<CommandSender>().parser()
        val name = parser.parse(context, input).parsedValue().getOrNull()
            ?: error("Unable to parse home name")
        return homeManager.get(context.sender() as Player, name) ?: throw HomeNotExistedException(name)
    }

    @ExceptionHandler(HomeNotExistedException::class)
    fun CommandSender.homeNotFound(exception: HomeNotExistedException) {
        sendMessage(COMMAND_HOME_FAILED_NOT_EXISTED.replace("<name>", exception.name))
    }
}

class HomeNotExistedException(val name: String) : Exception()
