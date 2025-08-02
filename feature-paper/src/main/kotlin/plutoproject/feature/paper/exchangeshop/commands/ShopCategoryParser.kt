package plutoproject.feature.paper.exchangeshop.commands

import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.exception.ExceptionHandler
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.parser.ArgumentParseResult
import org.incendo.cloud.parser.ArgumentParser
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.suggestion.BlockingSuggestionProvider
import org.incendo.cloud.suggestion.Suggestion
import plutoproject.feature.paper.api.exchangeshop.ExchangeShop
import plutoproject.feature.paper.api.exchangeshop.ShopCategory
import plutoproject.feature.paper.exchangeshop.COMMAND_EXCHANGE_SHOP_CATEGORY_NOT_FOUND
import plutoproject.framework.common.util.chat.component.replace
import kotlin.jvm.optionals.getOrNull

object ShopCategoryParser : ArgumentParser<CommandSender, ShopCategory>, BlockingSuggestionProvider<CommandSender> {
    private val stringParser = StringParser.stringParser<CommandSender>().parser()

    override fun parse(
        commandContext: CommandContext<CommandSender>,
        commandInput: CommandInput
    ): ArgumentParseResult<ShopCategory> {
        val id = stringParser.parse(commandContext, commandInput).parsedValue().getOrNull()
            ?: error("Unable to parse category id")
        val category = ExchangeShop.getCategory(id)
        return if (category != null) ArgumentParseResult.success(category) else throw ShopCategoryNotFoundException(id)
    }

    override fun suggestions(context: CommandContext<CommandSender>, input: CommandInput): List<Suggestion> {
        return ExchangeShop.categories.map { Suggestion.suggestion(it.id) }
    }
}

@Suppress("UNUSED")
object ShopCategoryNotFoundExceptionHandler {
    @ExceptionHandler(ShopCategoryNotFoundException::class)
    fun shopCategoryNotFound(sender: CommandSender, exception: ShopCategoryNotFoundException) {
        sender.sendMessage(COMMAND_EXCHANGE_SHOP_CATEGORY_NOT_FOUND.replace("<categoryId>", exception.id))
    }
}

class ShopCategoryNotFoundException(val id: String) : Exception()
