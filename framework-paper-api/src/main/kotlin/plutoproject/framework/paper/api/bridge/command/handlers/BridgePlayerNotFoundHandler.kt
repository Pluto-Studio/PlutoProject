package plutoproject.framework.paper.api.bridge.command.handlers

import org.bukkit.command.CommandSender
import org.incendo.cloud.exception.handling.ExceptionContext
import org.incendo.cloud.exception.handling.ExceptionHandler
import plutoproject.framework.common.api.bridge.MessageConstants
import plutoproject.framework.common.api.bridge.command.parsers.BridgePlayerNotFoundException
import plutoproject.framework.common.util.chat.component.replace

object BridgePlayerNotFoundHandler : ExceptionHandler<CommandSender, BridgePlayerNotFoundException> {
    override fun handle(context: ExceptionContext<CommandSender, BridgePlayerNotFoundException>) {
        context.context().sender().sendMessage(
            MessageConstants.playerNotFound.replace("<player>", context.exception().name)
        )
    }
}
