package ink.pmc.essentials.commands.home

import ink.pmc.essentials.COMMAND_HOME_NOT_EXISTED
import ink.pmc.essentials.api.home.Home
import ink.pmc.essentials.api.home.HomeManager
import ink.pmc.framework.chat.replace
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
object HomeCommons {
    @Suggestions("homes")
    suspend fun homes(context: CommandContext<CommandSender>, input: CommandInput): List<String> {
        val sender = context.sender()
        if (sender !is Player) return emptyList()
        return HomeManager.list(sender).map { it.name }
    }

    @Parser(name = "home", suggestions = "homes")
    suspend fun home(context: CommandContext<CommandSender>, input: CommandInput): Home {
        val parser = StringParser.greedyStringParser<CommandSender>().parser()
        val name = parser.parse(context, input).parsedValue().getOrNull()
            ?: error("Unable to parse home name")
        return HomeManager.get(context.sender() as Player, name) ?: throw HomeNotExistedException(name)
    }

    @ExceptionHandler(HomeNotExistedException::class)
    fun CommandSender.homeNotFound(exception: HomeNotExistedException) {
        sendMessage(COMMAND_HOME_NOT_EXISTED.replace("<name>", exception.name))
    }
}

class HomeNotExistedException(val name: String) : Exception()