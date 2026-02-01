package plutoproject.feature.velocity.serverSelector.commands

import com.velocitypowered.api.command.CommandSource
import ink.pmc.advkt.component.italic
import ink.pmc.advkt.component.text
import ink.pmc.advkt.send
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.velocity.serverSelector.VelocityServerSelectorConfig
import plutoproject.framework.common.util.chat.palettes.mochaMaroon
import plutoproject.framework.velocity.util.command.ensurePlayer
import plutoproject.framework.velocity.util.switchServer
import kotlin.jvm.optionals.getOrNull

object LobbyCommand : KoinComponent {
    private val config by inject<VelocityServerSelectorConfig>()

    @Command("lobby|hub")
    @Permission("plutoproject.server_selector.command.lobby")
    suspend fun CommandSource.lobby() = ensurePlayer {
        if (currentServer.getOrNull()?.serverInfo?.name == config.transferServer) {
            send {
                text("无法在此处使用该命令") with mochaMaroon
            }
            return@ensurePlayer
        }
        switchServer(config.transferServer)
    }
}
