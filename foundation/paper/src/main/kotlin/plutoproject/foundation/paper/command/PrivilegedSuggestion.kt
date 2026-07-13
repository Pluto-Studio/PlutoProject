package plutoproject.foundation.paper.command

import org.bukkit.command.CommandSender
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.suggestion.Suggestion
import org.incendo.cloud.suggestion.SuggestionProvider
import java.util.concurrent.CompletableFuture

fun <T : CommandSender> SuggestionProvider<T>.withPermission(permission: String): SuggestionProvider<T> {
    val delegate = this
    return object : SuggestionProvider<T> {
        override fun suggestionsFuture(
            context: CommandContext<T>,
            input: CommandInput,
        ): CompletableFuture<out MutableIterable<Suggestion>> =
            if (context.sender().hasPermission(permission)) delegate.suggestionsFuture(context, input)
            else CompletableFuture.completedFuture(mutableListOf())
    }
}
