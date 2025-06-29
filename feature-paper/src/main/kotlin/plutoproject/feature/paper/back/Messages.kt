package plutoproject.feature.paper.back

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.newline
import ink.pmc.advkt.component.text
import plutoproject.framework.common.util.chat.palettes.mochaMaroon
import plutoproject.framework.common.util.chat.palettes.mochaPink
import plutoproject.framework.common.util.chat.palettes.mochaSubtext0

val COMMAND_BACK_FAILED_NO_LOCATION = component {
    text("还没有记录到你先前的位置") with mochaMaroon
    newline()
    text("进行一段时间游玩后位置才会被记录") with mochaSubtext0
}

val COMMAND_BACK = component {
    text("已回到你先前的位置") with mochaPink
}
