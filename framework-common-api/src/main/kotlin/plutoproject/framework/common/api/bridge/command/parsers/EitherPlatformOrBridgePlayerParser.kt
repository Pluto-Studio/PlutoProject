package plutoproject.framework.common.api.bridge.command.parsers

import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.parser.ArgumentParseResult
import org.incendo.cloud.parser.ArgumentParser
import org.incendo.cloud.parser.ArgumentParser.FutureArgumentParser
import org.incendo.cloud.parser.ParserDescriptor
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.suggestion.Suggestion
import org.incendo.cloud.suggestion.SuggestionProvider
import org.incendo.cloud.type.Either
import plutoproject.framework.common.api.bridge.player.BridgePlayer
import plutoproject.framework.common.util.coroutine.runAsync
import java.util.concurrent.CompletableFuture
import kotlin.jvm.optionals.getOrNull

fun <C : Any, P : Any> eitherPlatformOrBridgePlayerParser(platformParser: ArgumentParser<C, P>) {
    EitherPlatformOrBridgePlayerParser(platformParser)
}

fun <C : Any, P : Any> eitherPlatformOrBridgePlayerParser(platformParser: ParserDescriptor<C, P>) {
    eitherPlatformOrBridgePlayerParser(platformParser.parser())
}

class EitherPlatformOrBridgePlayerParser<C : Any, P : Any>(
    private val platformParser: ArgumentParser<C, P>
) : FutureArgumentParser<C, Either<P, BridgePlayer>>,
    SuggestionProvider<C> {
    private val stringParser = StringParser.quotedStringParser<C>().parser()
    private val bridgePlayerParser = bridgePlayerParser<C>().parser()

    override fun parseFuture(
        commandContext: CommandContext<C>,
        commandInput: CommandInput
    ): CompletableFuture<ArgumentParseResult<Either<P, BridgePlayer>>> = runAsync {
        val name = stringParser.parseFuture(commandContext, commandInput).await().parsedValue().get()
        val platform = platformParser.parseFuture(commandContext, commandInput).await().parsedValue().getOrNull()
        val bridge = bridgePlayerParser.parseFuture(commandContext, commandInput).await().parsedValue().getOrNull()
        val either = if (platform != null) {
            Either.ofPrimary(platform)
        } else if (bridge != null) {
            Either.ofFallback(bridge)
        } else {
            throw BridgePlayerNotFoundException(name)
        }
        ArgumentParseResult.success(either)
    }.asCompletableFuture()

    override fun suggestionsFuture(
        context: CommandContext<C>,
        input: CommandInput
    ): CompletableFuture<List<Suggestion>> = runAsync {
        val platform = platformParser.suggestionProvider().suggestionsFuture(context, input).await()
        val bridge = bridgePlayerParser.suggestionProvider().suggestionsFuture(context, input).await()
        val result = (platform + bridge).distinctBy { it.suggestion() }
        result.toList()
    }.asCompletableFuture()

    override fun suggestionProvider(): SuggestionProvider<C> {
        return this
    }
}
