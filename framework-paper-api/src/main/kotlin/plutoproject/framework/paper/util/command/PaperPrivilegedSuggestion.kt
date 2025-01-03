package plutoproject.framework.paper.util.command

import org.bukkit.command.CommandSender
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.suggestion.SuggestionProvider
import plutoproject.framework.common.util.command.PrivilegedSuggestion

class PaperPrivilegedSuggestion<T : CommandSender>(
    wrap: SuggestionProvider<T>,
    permission: String
) : PrivilegedSuggestion<T>(wrap, permission) {
    companion object {
        fun <T : CommandSender> of(
            wrap: SuggestionProvider<T>,
            permission: String
        ): SuggestionProvider<T> {
            return PaperPrivilegedSuggestion(wrap, permission)
        }
    }

    override fun hasPermission(context: CommandContext<T>, permission: String): Boolean {
        return context.sender().hasPermission(permission)
    }
}

fun <T : CommandSender> SuggestionProvider<T>.toPrivileged(permission: String) =
    PaperPrivilegedSuggestion.of(this, permission)
