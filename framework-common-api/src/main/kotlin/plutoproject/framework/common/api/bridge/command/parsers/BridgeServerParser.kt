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
import plutoproject.framework.common.api.bridge.server.BridgeGroup
import plutoproject.framework.common.api.bridge.server.BridgeServer
import plutoproject.framework.common.util.coroutine.runAsync
import java.util.concurrent.CompletableFuture

fun <C> bridgeServerParser(group: BridgeGroup? = null): ParserDescriptor<C, BridgeServer> {
    return ParserDescriptor.of(BridgeServerParser<C>(group), BridgeServer::class.java)
}

class BridgeServerParser<C>(private val group: BridgeGroup?) : FutureArgumentParser<C, BridgeServer>,
    SuggestionProvider<C> {
    private val stringParser = StringParser.quotedStringParser<C>().parser()

    override fun parseFuture(
        commandContext: CommandContext<C>,
        commandInput: CommandInput
    ): CompletableFuture<ArgumentParseResult<BridgeServer>> = runAsync {
        val id = stringParser.parseFuture(commandContext, commandInput).await().parsedValue().get()
        val server = if (group != null) group.getServer(id) else Bridge.getServer(id)
        server?.let { ArgumentParseResult.success(it) } ?: throw BridgeServerNotFoundException(id)
    }.asCompletableFuture()

    override fun suggestionsFuture(
        context: CommandContext<C>,
        input: CommandInput
    ): CompletableFuture<List<Suggestion>> = runAsync {
        val servers = group?.servers ?: Bridge.servers
        servers.map { Suggestion.suggestion(it.id) }
    }.asCompletableFuture()

    override fun suggestionProvider(): SuggestionProvider<C> {
        return this
    }
}

class BridgeServerNotFoundException(val id: String) : Exception()
