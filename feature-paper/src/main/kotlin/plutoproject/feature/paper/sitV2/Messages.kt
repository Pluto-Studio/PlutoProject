package plutoproject.feature.paper.sitV2

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.keybind
import ink.pmc.advkt.component.text
import plutoproject.framework.common.util.chat.palettes.mochaLavender
import plutoproject.framework.common.util.chat.palettes.mochaText

val STAND_UP_TIP = component {
    text("按下 ") with mochaText
    keybind("key.sneak") with mochaLavender
    text(" 起身") with mochaText
}
