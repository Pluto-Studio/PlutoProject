package plutoproject.framework.velocity.api.bridge.command.handlers

import com.velocitypowered.api.command.CommandSource
import org.incendo.cloud.exception.handling.ExceptionContext
import org.incendo.cloud.exception.handling.ExceptionHandler
import plutoproject.framework.common.api.bridge.PLAYER_NOT_FOUND
import plutoproject.framework.common.api.bridge.command.parsers.BridgeServerNotFoundException
import plutoproject.framework.common.util.chat.component.replace

object BridgeServerNotFoundHandler : ExceptionHandler<CommandSource, BridgeServerNotFoundException> {
    override fun handle(context: ExceptionContext<CommandSource, BridgeServerNotFoundException>) {
        context.context().sender().sendMessage(
            PLAYER_NOT_FOUND.replace("<server>", context.exception().id)
        )
    }
}
