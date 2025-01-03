package plutoproject.framework.common.api.bridge.command.parsers

import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.parser.ArgumentParseResult
import org.incendo.cloud.parser.ArgumentParser.FutureArgumentParser
import org.incendo.cloud.parser.ParserDescriptor
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.suggestion.Suggestion
import org.incendo.cloud.suggestion.SuggestionProvider
import plutoproject.framework.common.api.bridge.Bridge
import plutoproject.framework.common.api.bridge.player.BridgePlayer
import plutoproject.framework.common.api.bridge.server.BridgeServer
import plutoproject.framework.common.util.coroutine.runAsync
import java.util.concurrent.CompletableFuture

fun <C : Any> bridgePlayerParser(server: BridgeServer? = null): ParserDescriptor<C, BridgePlayer> {
    return ParserDescriptor.of(BridgePlayerParser<C>(server), BridgePlayer::class.java)
}

class BridgePlayerParser<C : Any>(private val server: BridgeServer?) : FutureArgumentParser<C, BridgePlayer>,
    SuggestionProvider<C> {
    private val stringParser = StringParser.quotedStringParser<C>().parser()

    override fun parseFuture(
        commandContext: CommandContext<C>,
        commandInput: CommandInput
    ): CompletableFuture<ArgumentParseResult<BridgePlayer>> = runAsync {
        val name = stringParser.parseFuture(commandContext, commandInput).await().parsedValue().get()
        val player = if (server != null) server.getPlayer(name) else Bridge.getPlayer(name)
        player?.let { ArgumentParseResult.success(it) } ?: throw BridgePlayerNotFoundException(name)
    }.asCompletableFuture()

    override fun suggestionsFuture(
        context: CommandContext<C>,
        input: CommandInput
    ): CompletableFuture<List<Suggestion>> = runAsync {
        val players = server?.players ?: Bridge.players
        players.map { Suggestion.suggestion(it.name) }
    }.asCompletableFuture()

    override fun suggestionProvider(): SuggestionProvider<C> {
        return this
    }
}

class BridgePlayerNotFoundException(val name: String) : Exception()
