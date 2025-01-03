package plutoproject.framework.paper.api.bridge.command.handlers

import org.bukkit.command.CommandSender
import org.incendo.cloud.exception.handling.ExceptionContext
import org.incendo.cloud.exception.handling.ExceptionHandler
import plutoproject.framework.common.api.bridge.MessageConstants
import plutoproject.framework.common.api.bridge.command.parsers.BridgeServerNotFoundException
import plutoproject.framework.common.util.chat.component.replace

object BridgeServerNotFoundHandler : ExceptionHandler<CommandSender, BridgeServerNotFoundException> {
    override fun handle(context: ExceptionContext<CommandSender, BridgeServerNotFoundException>) {
        context.context().sender().sendMessage(
            MessageConstants.serverNotFound.replace("<server>", context.exception().id)
        )
    }
}
