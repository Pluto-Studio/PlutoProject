package plutoproject.framework.velocity.util.command

import com.velocitypowered.api.command.CommandSource
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.suggestion.SuggestionProvider
import plutoproject.framework.common.util.command.PrivilegedSuggestion

class VelocityPrivilegedSuggestion<T : CommandSource>(
    wrap: SuggestionProvider<T>,
    permission: String
) : PrivilegedSuggestion<T>(wrap, permission) {
    companion object {
        fun <T : CommandSource> of(
            wrap: SuggestionProvider<T>,
            permission: String
        ): SuggestionProvider<T> {
            return VelocityPrivilegedSuggestion(wrap, permission)
        }
    }

    override fun hasPermission(context: CommandContext<T>, perm: String): Boolean {
        return context.sender().hasPermission(perm)
    }
}

fun <T : CommandSource> SuggestionProvider<T>.toPrivileged(permission: String) =
    VelocityPrivilegedSuggestion.of(this, permission)
