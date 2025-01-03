package plutoproject.framework.paper.options

import plutoproject.framework.common.options.OptionsUpdateNotifier
import java.util.*

class BackendOptionsUpdateNotifier : OptionsUpdateNotifier {
    override fun notify(player: UUID) {
        sendContainerUpdateNotify(player)
    }
}
