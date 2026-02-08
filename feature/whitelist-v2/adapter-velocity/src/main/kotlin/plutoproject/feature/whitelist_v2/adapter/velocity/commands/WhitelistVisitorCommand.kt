package plutoproject.feature.whitelist_v2.adapter.velocity.commands

import com.velocitypowered.api.command.CommandSource
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.koin.core.component.KoinComponent
import plutoproject.feature.whitelist_v2.adapter.velocity.COMMAND_VISITOR_MODE_TOGGLED_OFF
import plutoproject.feature.whitelist_v2.adapter.velocity.COMMAND_VISITOR_MODE_TOGGLED_ON
import plutoproject.feature.whitelist_v2.adapter.velocity.PERMISSION_COMMAND_WHITELIST_VISITOR_TOGGLE
import plutoproject.feature.whitelist_v2.adapter.velocity.VisitorState

@Suppress("UNUSED")
object WhitelistVisitorCommand : KoinComponent {
    @Command("whitelist visitor toggle")
    @Permission(PERMISSION_COMMAND_WHITELIST_VISITOR_TOGGLE)
    fun CommandSource.toggleVisitorMode() {
        val newState = VisitorState.toggle()
        if (newState) {
            sendMessage(COMMAND_VISITOR_MODE_TOGGLED_ON)
        } else {
            sendMessage(COMMAND_VISITOR_MODE_TOGGLED_OFF)
        }
    }
}
