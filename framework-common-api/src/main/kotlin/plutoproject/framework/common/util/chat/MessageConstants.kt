package plutoproject.framework.common.util.chat

import ink.pmc.advkt.component.*
import plutoproject.framework.common.util.chat.palettes.mochaMaroon
import plutoproject.framework.common.util.chat.palettes.mochaSubtext0
import plutoproject.framework.common.util.chat.palettes.mochaYellow

object MessageConstants {
    const val ECONOMY_SYMBOL = "\uD83C\uDF1F"
    val emptyLine = component { }
    val nonPlayer = component {
        text("该命令仅限玩家使用") with mochaMaroon
    }
    val plutoProject = component {
        miniMessage("<gradient:#c6a0f6:#f5bde6:#f0c6c6:#f4dbd6>星社 ᴘʀᴏᴊᴇᴄᴛ</gradient>")
    }
    val playerNotOnline = component {
        text("该玩家不在线") with mochaMaroon
    }
    val noPermission = component {
        text("你似乎没有权限这么做") with mochaMaroon
        newline()
        text("如果你认为这是一个错误的话，请向管理组报告") with mochaSubtext0
    }
    val unusualIssue = component {
        text("看起来你似乎遇见了一个很罕见的问题") with mochaMaroon
        newline()
        text("我们建议你反馈这个问题，有助于将服务器变得更好") with mochaSubtext0
    }
    val inProgress = component {
        text("正在施工中...") with mochaMaroon
        newline()
        text("前面的路，以后再来探索吧！") with mochaSubtext0
    }

    object UI {
        val close = component {
            text("关闭") with mochaMaroon without italic()
        }
        val back = component {
            text("返回") with mochaYellow without italic()
        }
    }
}
