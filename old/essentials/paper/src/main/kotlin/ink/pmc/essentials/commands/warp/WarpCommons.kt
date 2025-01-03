package ink.pmc.essentials.commands.warp

import ink.pmc.advkt.component.replace
import ink.pmc.essentials.COMMAND_WARP_NOT_EXISTED
import ink.pmc.essentials.api.warp.Warp
import ink.pmc.essentials.api.warp.WarpManager
import ink.pmc.framework.concurrent.submitAsync
import kotlinx.coroutines.future.asCompletableFuture
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.exception.ExceptionHandler
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.parser.ArgumentParseResult
import org.incendo.cloud.parser.ArgumentParser.FutureArgumentParser
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.suggestion.Suggestion
import org.incendo.cloud.suggestion.SuggestionProvider
import java.util.concurrent.CompletableFuture
import kotlin.jvm.optionals.getOrNull

@Suppress("UNUSED", "UNUSED_PARAMETER")
object WarpCommons {
    @ExceptionHandler(WarpNotExistedException::class)
    fun CommandSender.warpNotExisted(exception: WarpNotExistedException) {
        sendMessage(COMMAND_WARP_NOT_EXISTED.replace("<name>", exception.name))
    }
}

class WarpParser(val withoutAlias: Boolean) : FutureArgumentParser<CommandSender, Warp>,
    SuggestionProvider<CommandSender> {
    private val stringParser =
        if (!withoutAlias) StringParser.greedyStringParser<CommandSender>() else StringParser.quotedStringParser()

    override fun parseFuture(
        commandContext: CommandContext<CommandSender>,
        commandInput: CommandInput
    ): CompletableFuture<ArgumentParseResult<Warp>> = submitAsync<ArgumentParseResult<Warp>> {
        val string = stringParser.parser().parse(commandContext, commandInput).parsedValue().getOrNull()
            ?: error("Unable to parse warp name")
        val name = parseWarpName(string)
        WarpManager.get(name)?.let { ArgumentParseResult.success(it) }
            ?: throw WarpNotExistedException(name)
    }.asCompletableFuture()

    override fun suggestionsFuture(
        context: CommandContext<CommandSender>,
        input: CommandInput
    ): CompletableFuture<List<Suggestion>> = submitAsync<List<Suggestion>> {
        if (!withoutAlias) {
            WarpManager.list().map {
                val name = it.name
                val alias = it.alias
                Suggestion.suggestion(if (alias == null) name else "$name-$alias")
            }
        } else {
            WarpManager.list().map { Suggestion.suggestion(it.name) }
        }
    }.asCompletableFuture()

    override fun suggestionProvider(): SuggestionProvider<CommandSender> {
        return this
    }
}

fun parseWarpName(input: String): String {
    return input.substringBefore('-')
}

class WarpNotExistedException(val name: String) : Exception()