package plutoproject.framework.velocity.rpc

import com.velocitypowered.api.command.CommandSource
import ink.pmc.advkt.component.text
import ink.pmc.advkt.send
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.framework.common.api.rpc.RpcServer
import plutoproject.framework.common.util.chat.palettes.mochaFlamingo
import plutoproject.framework.common.util.chat.palettes.mochaMaroon
import plutoproject.framework.common.util.chat.palettes.mochaSubtext0
import plutoproject.framework.common.util.chat.palettes.mochaText

@Suppress("UNUSED")
object RpcCommand {
    @Command("rpc")
    @Permission("rpc.command")
    fun rpcCommand(sender: CommandSource) {
        if (RpcServer.server.services.isEmpty()) {
            sender.send {
                text("暂无注册的服务") with mochaMaroon
            }
            return
        }
        sender.send {
            text("已在 gRPC 服务端注册的服务: ") with mochaFlamingo
        }
        RpcServer.server.services.forEach { service ->
            val name = service.serviceDescriptor.name
            sender.send {
                text("  - ") with mochaSubtext0
                text(name) with mochaText
            }
        }
    }
}
