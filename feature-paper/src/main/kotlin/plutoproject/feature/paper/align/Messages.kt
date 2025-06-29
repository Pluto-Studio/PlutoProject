package plutoproject.feature.paper.align

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.text
import plutoproject.framework.common.util.chat.palettes.mochaPink
import plutoproject.framework.common.util.chat.palettes.mochaText

val COMMAND_ALIGN = component {
    text("已对齐你的视角和位置") with mochaText
}

val COMMAND_ALIGN_POS = component {
    text("已对齐你的位置") with mochaText
}

val COMMAND_ALIGN_VIEW = component {
    text("已对齐你的视角") with mochaText
}
