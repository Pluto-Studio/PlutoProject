package plutoproject.feature.velocity.motd

import com.velocitypowered.api.command.CommandSource
import ink.pmc.advkt.component.text
import ink.pmc.advkt.send
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.framework.common.util.chat.palettes.mochaGreen
import plutoproject.framework.common.util.chat.palettes.mochaMaroon

@Suppress("UNUSED")
object MotdCommand : KoinComponent {
    private val service by inject<MotdService>()

    @Command("motd reload")
    @Permission(PERMISSION_COMMAND_MOTD_RELOAD)
    fun CommandSource.reload() {
        val ok = service.reload()
        if (ok) {
            send { text("MOTD 配置已重载") with mochaGreen }
        } else {
            send { text("MOTD 配置重载失败，请检查控制台报错") with mochaMaroon }
        }
    }
}
