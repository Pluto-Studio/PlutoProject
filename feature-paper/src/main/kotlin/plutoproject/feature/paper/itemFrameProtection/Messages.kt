package plutoproject.feature.paper.itemFrameProtection

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.text
import net.kyori.adventure.text.Component
import plutoproject.framework.common.util.chat.palettes.mochaMaroon
import plutoproject.framework.common.util.chat.palettes.mochaPink
import plutoproject.framework.common.util.chat.palettes.mochaSubtext0
import plutoproject.framework.common.util.chat.palettes.mochaText


val COMMAND_ITEMFRAME_FAILED_NO_ITEMFRAME = component {
    text("你需要对着一个物品展示框才可以这么做") with mochaMaroon
}

val COMMAND_ITEMFRAME_TOGGLE_ON_INVISBLE = component {
    text("已将你面前的展示框隐藏") with mochaPink
}

val COMMAND_ITEMFRAME_TOGGLE_OFF_INVISBLE = component {
    text("已将你面前的展示框显现") with mochaPink
}

val COMMAND_ITEMFRAME_TOGGLE_ON_PROTECTION = component {
    text("已将你面前的展示框保护") with mochaPink
}

val COMMAND_ITEMFRAME_TOGGLE_OFF_PROTECTION = component {
    text("已将你面前的展示框取消保护") with mochaPink
}

val ITEMFRAME_PROTECTED = component {
    text("此展示框已被 ") with mochaSubtext0
    text("<player> ") with mochaText
    text("保护") with mochaSubtext0
}

const val ITEMFRAME_PROTECTION_UNKNOWN_PLAYER = "未知玩家"

val ITEMFRAME_PROTECTION_UNFINISHED_BOOK = Component.text("未完成的书")

val ITEMFRAME_PROTECTION_UNKNOWN_AUTHOR = Component.text("未知作者")
