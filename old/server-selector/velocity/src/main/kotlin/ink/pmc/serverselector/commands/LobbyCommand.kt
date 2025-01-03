package ink.pmc.serverselector.commands

import com.velocitypowered.api.command.CommandSource
import ink.pmc.advkt.component.italic
import ink.pmc.advkt.component.text
import ink.pmc.advkt.send
import ink.pmc.advkt.title.*
import ink.pmc.essentials.TELEPORT_FAILED_SOUND
import ink.pmc.essentials.TELEPORT_SUCCEED_SOUND
import ink.pmc.framework.bridge.player.toBridge
import ink.pmc.framework.command.ensurePlayer
import ink.pmc.framework.player.switchServer
import ink.pmc.framework.chat.mochaMaroon
import ink.pmc.framework.chat.mochaText
import ink.pmc.serverselector.VelocityServerSelectorConfig
import net.kyori.adventure.util.Ticks
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.jvm.optionals.getOrNull

object LobbyCommand : KoinComponent {
    private val config by inject<VelocityServerSelectorConfig>()

    @Command("lobby|hub")
    @Permission("server_selector.command")
    suspend fun CommandSource.lobby() = ensurePlayer {
        if (currentServer.getOrNull()?.serverInfo?.name == config.transferServer) {
            send {
                text("无法在此处使用该命令") with mochaMaroon without italic()
            }
            return@ensurePlayer
        }
        if (switchServer(config.transferServer).isSuccessful) {
            toBridge().playSound(TELEPORT_SUCCEED_SOUND)
            return@ensurePlayer
        }
        val bridge = toBridge()
        bridge.showTitle {
            times {
                fadeIn(Ticks.duration(5))
                stay(Ticks.duration(35))
                fadeOut(Ticks.duration(20))
            }
            mainTitle {
                text("传送失败") with mochaMaroon
            }
            subTitle {
                text("请再试一次") with mochaText
            }
        }
        bridge.playSound(TELEPORT_FAILED_SOUND)
    }
}