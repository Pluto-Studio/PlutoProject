package plutoproject.feature.whip.paper

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.newline
import ink.pmc.advkt.component.text
import plutoproject.foundation.common.text.*

const val WHIP_PLACEHOLDER_LEVEL = "<level>"
const val WHIP_PLACEHOLDER_LENGTH = "<length>"
const val WHIP_PLACEHOLDER_PLAYER = "<player>"

val WHIP_ITEM_NAME = component {
    text("皮鞭") with mochaYellow
}

val WHIP_ITEM_LORE_LEVEL = component {
    text("等级：") with mochaSubtext0
    text(WHIP_PLACEHOLDER_LEVEL) with mochaText
}

val WHIP_ITEM_LORE_LENGTH = component {
    text("长度：") with mochaSubtext0
    text(WHIP_PLACEHOLDER_LENGTH) with mochaText
    text(" 格") with mochaSubtext0
}

val WHIP_COMMAND_PLAYER_ONLY = component {
    text("该命令只能由玩家执行，或指定在线玩家") with mochaMaroon
}

val WHIP_COMMAND_INVALID_LEVEL = component {
    text("无效的皮鞭等级：") with mochaMaroon
    text(WHIP_PLACEHOLDER_LEVEL) with mochaText
    newline()
    text("可用等级为 I、II、III、IV、V") with mochaSubtext0
}

val WHIP_COMMAND_INVENTORY_FULL = component {
    text("目标背包已满，未给予皮鞭") with mochaMaroon
}

val WHIP_COMMAND_GIVE_SUCCESS = component {
    text("已给予 ") with mochaPink
    text(WHIP_PLACEHOLDER_PLAYER) with mochaFlamingo
    text(" 一条 ") with mochaPink
    text(WHIP_PLACEHOLDER_LEVEL) with mochaFlamingo
    text(" 级皮鞭") with mochaPink
}

val WHIP_COMMAND_GIVE_TARGET = component {
    text("你收到了一条 ") with mochaPink
    text(WHIP_PLACEHOLDER_LEVEL) with mochaFlamingo
    text(" 级皮鞭") with mochaPink
}
