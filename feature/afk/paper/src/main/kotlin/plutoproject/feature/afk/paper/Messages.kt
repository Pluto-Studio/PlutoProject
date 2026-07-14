package plutoproject.feature.afk.paper

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.text
import plutoproject.foundation.common.text.mochaSubtext0

val PLAYER_ENTER_AFK_BROADCAST = component {
    text("* <player> 暂时离开了") with mochaSubtext0
}

val PLAYER_EXIT_AFK_BROADCAST = component {
    text("* <player> 回来了") with mochaSubtext0
}
