package plutoproject.feature.lecternprotection.paper

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.text
import plutoproject.foundation.common.text.*

val COMMAND_LECTERN_FAILED_NO_LECTERN = component {
    text("你需要对着一个讲台才可以这么做") with mochaMaroon
}

val COMMAND_LECTERN_PROTECTION_ON_SUCCEED = component {
    text("已将你面前的讲台保护") with mochaPink
}

val COMMAND_LECTERN_PROTECTION_OFF_SUCCEED = component {
    text("已将你面前的讲台取消保护") with mochaPink
}

val LECTERN_PROTECTED_ON_ACTION = component {
    text("此讲台已被 ") with mochaSubtext0
    text("<player> ") with mochaText
    text("保护") with mochaSubtext0
}

const val LECTERN_PROTECTION_UNKNOWN_PLAYER = "未知玩家"
