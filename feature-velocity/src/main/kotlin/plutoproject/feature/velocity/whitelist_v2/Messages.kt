package plutoproject.feature.velocity.whitelist_v2

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.newline
import ink.pmc.advkt.component.text
import plutoproject.framework.common.util.chat.palettes.mochaLavender
import plutoproject.framework.common.util.chat.palettes.mochaMaroon
import plutoproject.framework.common.util.chat.palettes.mochaPink
import plutoproject.framework.common.util.chat.palettes.mochaSubtext0
import plutoproject.framework.common.util.chat.palettes.mochaText

val PLAYER_NOT_WHITELISTED = component {
    text("你的账号未获得白名单") with mochaMaroon
    newline()
    text("若已通过审核，请联系「候车厅」内的管理员添加") with mochaSubtext0
}

val PLAYER_WHITELIST_STATE_MODIFIED = component {
    text("你的白名单状态已修改，请重新连接") with mochaText
}

val COMMAND_WHITELIST_ADD_ALREADY_EXISTS = component {
    text("玩家 ") with mochaMaroon
    text("<name> ") with mochaText
    text("已经拥有白名单") with mochaMaroon
}

val COMMAND_WHITELIST_ADD_FETCHING = component {
    text("正在获取数据，请稍等...") with mochaText
}

val COMMAND_WHITELIST_PROFILE_FETCH_TIMEOUT = component {
    text("数据获取超时，请重试") with mochaMaroon
}

val COMMAND_WHITELIST_PROFILE_FETCH_NOT_FOUND = component {
    text("未获取到玩家 ") with mochaMaroon
    text("<name> ") with mochaText
    text("的数据，请检查玩家名是否正确") with mochaMaroon
}

val COMMAND_WHITELIST_ADD_SUCCEED = component {
    text("已为玩家 ") with mochaPink
    text("<name> ") with mochaText
    text("授予白名单") with mochaPink
}

val COMMAND_WHITELIST_LOOKUP_NOT_FOUND = component {
    text("未查询到名为 ") with mochaMaroon
    text("<name> ") with mochaText
    text("的玩家") with mochaMaroon
}

val COMMAND_WHITELIST_LOOKUP_WHITELISTED = component {
    text("已查询到名为 ") with mochaPink
    text("<name> ") with mochaText
    text("的玩家") with mochaPink
}

val COMMAND_WHITELIST_REMOVE_NOT_FOUND = component {
    text("名为 ") with mochaMaroon
    text("<name> ") with mochaText
    text("的玩家未获得白名单") with mochaMaroon
}

val COMMAND_WHITELIST_REMOVE_SUCCEED = component {
    text("已撤销玩家 ") with mochaPink
    text("<name> ") with mochaText
    text("的白名单") with mochaPink
}

val COMMAND_WHITELIST_STATISTIC = component {
    text("当前有 ") with mochaText
    text("<count> ") with mochaLavender
    text("位玩家获得了白名单") with mochaText
}
