package plutoproject.feature.paper.status

import ink.pmc.advkt.component.newline
import ink.pmc.advkt.component.raw
import ink.pmc.advkt.send
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.framework.paper.util.coroutine.withSync

@Suppress("UNUSED")
object StatusCommand {
    @Command("status|tps|mspt")
    @Permission("hypervisor.status")
    suspend fun CommandSender.status() {
        val statusMessage = withSync { getStatusMessage() }
        send {
            raw(INDICATOR)
            raw(statusMessage)
            newline()
            newline()
            raw(INDICATOR)
            raw(getPromptMessage())
        }
    }
}
