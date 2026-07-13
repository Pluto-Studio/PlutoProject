package plutoproject.feature.motd.velocity

import com.velocitypowered.api.command.CommandSource
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission

internal class MotdCommand(private val service: MotdService) {
    @Command("motd reload")
    @Permission(PERMISSION_COMMAND_MOTD_RELOAD)
    fun CommandSource.reload() {
        sendMessage(if (service.reload()) MOTD_RELOAD_SUCCESS else MOTD_RELOAD_FAILURE)
    }
}
