package plutoproject.feature.velocity.whitelist_v2

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.newline
import ink.pmc.advkt.component.text
import net.kyori.adventure.text.Component
import plutoproject.framework.common.util.chat.palettes.*

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

val COMMAND_WHITELIST_LOOKUP_NO_RECORD = component {
    text("玩家 ") with mochaMaroon
    text("<name> ") with mochaText
    text("没有白名单记录") with mochaMaroon
}

val COMMAND_WHITELIST_OPERATOR_CONSOLE = Component.text("控制台")

val COMMAND_WHITELIST_OPERATOR_ADMIN = component {
    text("管理员 ")
    text("(<name>)") with mochaSubtext0
}

val COMMAND_WHITELIST_REVOKE_REASON_VIOLATION = Component.text("违规").color(mochaMaroon)

val COMMAND_WHITELIST_REVOKE_REASON_REQUESTED = Component.text("主动请求").color(mochaYellow)

val COMMAND_WHITELIST_REVOKE_REASON_OTHER = Component.text("其他").color(mochaSubtext0)

val COMMAND_WHITELIST_LOOKUP_HEADER = component {
    text("玩家 ") with mochaText
    text("<name> ") with mochaYellow
    text("的白名单信息：") with mochaText
}

val COMMAND_WHITELIST_LOOKUP_UUID = component {
    text("- ") with mochaSubtext0
    text("UUID: ") with mochaText
    text("<uuid>") with mochaLavender
}

val COMMAND_WHITELIST_LOOKUP_USERNAME = component {
    text("- ") with mochaSubtext0
    text("用户名: ") with mochaText
    text("<username>") with mochaLavender
}

val COMMAND_WHITELIST_LOOKUP_GRANTER = component {
    text("- ") with mochaSubtext0
    text("授权者: ") with mochaText
    text("<granter>") with mochaLavender
}

val COMMAND_WHITELIST_LOOKUP_CREATED_AT = component {
    text("- ") with mochaSubtext0
    text("创建时间: ") with mochaText
    text("<created_at>") with mochaLavender
}

val COMMAND_WHITELIST_LOOKUP_VISITOR_BEFORE = component {
    text("- ") with mochaSubtext0
    text("此前访客: ") with mochaText
    text("<status>") with mochaLavender
}

val COMMAND_WHITELIST_LOOKUP_MIGRATED = component {
    text("- ") with mochaSubtext0
    text("迁移记录: ") with mochaText
    text("<status>") with mochaLavender
}

val COMMAND_WHITELIST_LOOKUP_REVOKED = component {
    text("- ") with mochaSubtext0
    text("撤销状态: ") with mochaText
    text("<status>") with mochaLavender
}

val COMMAND_WHITELIST_LOOKUP_REVOKER = component {
    text("- ") with mochaSubtext0
    text("撤销者: ") with mochaText
    text("<revoker>") with mochaLavender
}

val COMMAND_WHITELIST_LOOKUP_REVOKE_REASON = component {
    text("- ") with mochaSubtext0
    text("撤销原因: ") with mochaText
    text("<reason>") with mochaLavender
}

val COMMAND_WHITELIST_LOOKUP_REVOKE_TIME = component {
    text("- ") with mochaSubtext0
    text("撤销时间: ") with mochaText
    text("<time>") with mochaLavender
}

val COMMAND_WHITELIST_MIGRATE_START = component {
    text("正在迁移旧版白名单数据，请稍等...") with mochaText
}

val COMMAND_WHITELIST_MIGRATE_COMPLETE = component {
    text("迁移完成，共迁移了 ") with mochaPink
    text("<count> ") with mochaLavender
    text("条数据") with mochaPink
}
