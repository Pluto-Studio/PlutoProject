package plutoproject.framework.common.util.chat

import ink.pmc.advkt.component.*
import plutoproject.framework.common.util.chat.palettes.mochaMaroon
import plutoproject.framework.common.util.chat.palettes.mochaSubtext0
import plutoproject.framework.common.util.chat.palettes.mochaYellow

val GENERIC_TEXT_SERVER_BRAND = component {
    miniMessage("<gradient:#c6a0f6:#f5bde6:#f0c6c6:#f4dbd6>星社 Project</gradient>")
}

val GENERIC_TEXT_SERVER_BRAND_ENGLISH = component {
    miniMessage("<gradient:#c6a0f6:#f5bde6:#f0c6c6:#f4dbd6>PlutoProject</gradient>")
}

const val ECONOMY_SYMBOL = "\uD83C\uDF1F"

val EMPTY_LINE = component { }

val PLAYER_ONLY_COMMAND = component {
    text("该命令仅限玩家使用") with mochaMaroon
}

val PLAYER_OFFLINE = component {
    text("该玩家不在线") with mochaMaroon
}

val PERMISSION_DENIED = component {
    text("你似乎没有权限这么做") with mochaMaroon
    newline()
    text("如果你认为这是一个错误的话，请向管理组报告") with mochaSubtext0
}

val UNUSUAL_ISSUE_OCCURRED = component {
    text("看起来你似乎遇见了一个很罕见的问题") with mochaMaroon
    newline()
    text("我们建议你反馈这个问题，有助于将服务器变得更好") with mochaSubtext0
}

val WORK_IN_PROGRESS = component {
    text("正在施工中...") with mochaMaroon
    newline()
    text("前面的区域，以后再来探索吧！") with mochaSubtext0
}

val UI_CLOSE = component {
    text("关闭") with mochaMaroon
}

val UI_BACK = component {
    text("返回") with mochaYellow
}

val UI_BACK_OPERATION = component {
    text("返回上一页")
}
