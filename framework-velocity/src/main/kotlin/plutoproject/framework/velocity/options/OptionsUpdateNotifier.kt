package plutoproject.framework.velocity.options

import plutoproject.framework.common.options.OptionsUpdateNotifier
import plutoproject.framework.velocity.options.proto.OptionsRpc
import java.util.*

class OptionsUpdateNotifier : OptionsUpdateNotifier {
    override fun notify(player: UUID) {
        OptionsRpc.notifyBackendContainerUpdate(player)
    }
}
