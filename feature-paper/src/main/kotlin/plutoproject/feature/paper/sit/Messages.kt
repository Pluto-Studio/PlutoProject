package plutoproject.feature.paper.sit

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.keybind
import ink.pmc.advkt.component.text
import ink.pmc.advkt.title.*
import plutoproject.framework.common.util.chat.palettes.mochaMaroon
import plutoproject.framework.common.util.chat.palettes.mochaPink
import plutoproject.framework.common.util.chat.palettes.mochaText
import kotlin.time.Duration.Companion.seconds

val PRESS_KEY_TO_STAND = component {
    text("使用 ") with mochaPink
    keybind("key.sneak") with mochaText
    text(" 键起身") with mochaPink
}

val ILLEGAL_LOCATION = component {
    text("无法在此处坐下，请检查是否有实体方块以及足够的空间") with mochaMaroon
}

val LOCATION_OCCUPIED_BY_OTHER_TITLE = title {
    mainTitle {
        text(" ")
    }
    subTitle {
        text("此位置已有其他人坐下") with mochaMaroon
    }
    times {
        fadeIn(0.seconds)
        stay(1.seconds)
        fadeOut(0.seconds)
    }
}
