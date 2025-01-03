package plutoproject.framework.velocity.api.bridge.command.handlers

import com.velocitypowered.api.command.CommandSource
import org.incendo.cloud.exception.handling.ExceptionContext
import org.incendo.cloud.exception.handling.ExceptionHandler
import plutoproject.framework.common.api.bridge.MessageConstants
import plutoproject.framework.common.api.bridge.command.parsers.BridgePlayerNotFoundException
import plutoproject.framework.common.util.chat.component.replace

object BridgePlayerNotFoundHandler : ExceptionHandler<CommandSource, BridgePlayerNotFoundException> {
    override fun handle(context: ExceptionContext<CommandSource, BridgePlayerNotFoundException>) {
        context.context().sender().sendMessage(
            MessageConstants.playerNotFound.replace("<player>", context.exception().name)
        )
    }
}
