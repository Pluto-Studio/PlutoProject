package plutoproject.feature.warp.paper.commands

import plutoproject.feature.warp.paper.warpManager

import ink.pmc.advkt.component.replace
import kotlinx.coroutines.async
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
import plutoproject.feature.warp.api.paper.Warp
import plutoproject.feature.warp.api.paper.WarpManager
import plutoproject.feature.warp.paper.COMMAND_WARP_NOT_EXISTED
import plutoproject.feature.warp.paper.moduleScope
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
    ): CompletableFuture<ArgumentParseResult<Warp>> = moduleScope.async {
        val string = stringParser.parser().parse(commandContext, commandInput).parsedValue().getOrNull()
            ?: error("Unable to parse warp name")
        val name = parseWarpName(string)
        warpManager.get(name)?.let { ArgumentParseResult.success(it) }
            ?: throw WarpNotExistedException(name)
    }.asCompletableFuture()

    override fun suggestionsFuture(
        context: CommandContext<CommandSender>,
        input: CommandInput
    ): CompletableFuture<List<Suggestion>> = moduleScope.async {
        if (!withoutAlias) {
            warpManager.list().map {
                val name = it.name
                val alias = it.alias
                Suggestion.suggestion(if (alias == null) name else "$name-$alias")
            }
        } else {
            warpManager.list().map { Suggestion.suggestion(it.name) }
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
