package ink.pmc.framework.options

import java.util.*

interface OptionsUpdateNotifier {
    fun notify(player: UUID)
}