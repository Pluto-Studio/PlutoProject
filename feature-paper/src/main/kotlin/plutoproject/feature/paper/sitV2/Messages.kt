package plutoproject.feature.paper.sitV2

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.keybind
import ink.pmc.advkt.component.text
import ink.pmc.advkt.title.*
import plutoproject.framework.common.util.chat.palettes.mochaLavender
import plutoproject.framework.common.util.chat.palettes.mochaMaroon
import plutoproject.framework.common.util.chat.palettes.mochaText
import kotlin.time.Duration.Companion.seconds

val STAND_UP_TIP = component {
    text("按下 ") with mochaText
    keybind("key.sneak") with mochaLavender
    text(" 起身") with mochaText
}

val COMMAND_SIT = component {
    text("已在此处坐下") with mochaText
}

val COMMAND_SIT_FAILED_ALREADY_SITTING = component {
    text("你已经坐在一个位置上了") with mochaMaroon
}

val COMMAND_SIT_FAILED_TARGET_OCCUPIED = component {
    text("此处已有其他人坐下") with mochaMaroon
}

val COMMAND_SIT_FAILED_INVALID_TARGET = component {
    text("无法在此处坐下") with mochaMaroon
}

val COMMAND_SIT_FAILED_BLOCKED_BY_BLOCKS = component {
    text("此处被方块遮挡，无法坐下") with mochaMaroon
}

val SIT_FAILED_TARGET_OCCUPIED_TITLE = title {
    mainTitle {
        text(" ")
    }
    subTitle {
        text("此处已有其他人坐下") with mochaMaroon
    }
    times {
        fadeIn(0.seconds)
        stay(1.seconds)
        fadeOut(0.seconds)
    }
}

val SIT_FAILED_BLOCKED_BY_BLOCKS_TITLE = title {
    mainTitle {
        text(" ")
    }
    subTitle {
        text("此处被方块遮挡，无法坐下") with mochaMaroon
    }
    times {
        fadeIn(0.seconds)
        stay(1.seconds)
        fadeOut(0.seconds)
    }
}
