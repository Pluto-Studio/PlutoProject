package plutoproject.feature.paper.exchangeshop.commands

import kotlinx.coroutines.future.asCompletableFuture
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.exception.ExceptionHandler
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.parser.ArgumentParseResult
import org.incendo.cloud.parser.ArgumentParser.FutureArgumentParser
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.suggestion.BlockingSuggestionProvider
import org.incendo.cloud.suggestion.Suggestion
import plutoproject.feature.paper.api.exchangeshop.ExchangeShop
import plutoproject.feature.paper.api.exchangeshop.ShopUser
import plutoproject.feature.paper.exchangeshop.COMMAND_EXCHANGE_SHOP_TRANSACTIONS_SHOP_USER_NOT_FOUND
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.common.util.coroutine.runAsync
import plutoproject.framework.common.util.data.convertToUuidOrNull
import plutoproject.framework.paper.util.server
import java.util.concurrent.CompletableFuture
import kotlin.jvm.optionals.getOrNull

object ShopUserParser : FutureArgumentParser<CommandSender, ShopUser>, BlockingSuggestionProvider<CommandSender> {
    private val stringParser = StringParser.stringParser<CommandSender>().parser()

    override fun parseFuture(
        commandContext: CommandContext<CommandSender>,
        commandInput: CommandInput
    ): CompletableFuture<ArgumentParseResult<ShopUser>> = runAsync {
        val input = stringParser.parse(commandContext, commandInput).parsedValue().getOrNull()
            ?: error("Unable to get input")
        val uniqueId = input.convertToUuidOrNull()
        val player = if (uniqueId != null) server.getOfflinePlayer(uniqueId) else server.getOfflinePlayer(input)
        val shopUser = ExchangeShop.getUser(player)
        if (shopUser != null) ArgumentParseResult.success(shopUser) else throw ShopUserNotFoundException(input)
    }.asCompletableFuture()

    override fun suggestions(context: CommandContext<CommandSender>, input: CommandInput): List<Suggestion> {
        return server.onlinePlayers.map { Suggestion.suggestion(it.name) }
    }
}

@Suppress("UNUSED")
object ShopUserNotFoundExceptionHandler {
    @ExceptionHandler(ShopUserNotFoundException::class)
    fun shopUserNotFound(sender: CommandSender, exception: ShopUserNotFoundException) {
        sender.sendMessage(COMMAND_EXCHANGE_SHOP_TRANSACTIONS_SHOP_USER_NOT_FOUND.replace("<input>", exception.input))
    }
}

class ShopUserNotFoundException(val input: String) : Exception()
