package plutoproject.framework.common.util.command

import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.suggestion.Suggestion
import org.incendo.cloud.suggestion.SuggestionProvider
import java.util.concurrent.CompletableFuture

abstract class PrivilegedSuggestion<C>(
    private val wrap: SuggestionProvider<C>,
    private val permission: String
) : SuggestionProvider<C> {
    override fun suggestionsFuture(
        context: CommandContext<C>,
        input: CommandInput
    ): CompletableFuture<out MutableIterable<Suggestion>> {
        if (hasPermission(context, permission)) {
            return wrap.suggestionsFuture(context, input)
        }
        return CompletableFuture.completedFuture(mutableListOf())
    }

    abstract fun hasPermission(context: CommandContext<C>, permission: String): Boolean
}
