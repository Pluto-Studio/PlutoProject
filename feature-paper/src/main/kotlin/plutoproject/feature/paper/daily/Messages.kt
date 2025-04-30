package plutoproject.feature.paper.daily

import ink.pmc.advkt.component.*
import net.kyori.adventure.text.Component
import plutoproject.framework.common.util.chat.ECONOMY_SYMBOL
import plutoproject.framework.common.util.chat.palettes.*

val CHECK_IN_SUCCEED = component {
    text("到访成功，本月已连续到访 ") with mochaPink
    text("<acc> ") with mochaText
    text("天") with mochaPink
}

val ALREADY_CHECK_IN = component {
    text("今日已到访") with mochaSubtext0
}

val COIN_CLAIMED = component {
    text("今日到访获得 ") with mochaSubtext0
    text("<amount>${ECONOMY_SYMBOL}") with mochaText
}

val PLAYER_NOT_CHECKED_IN = component {
    text("✨ 今日尚未到访，到访可获取货币奖励 ") with mochaText
    text("[打开礼记]") with mochaLavender with showText {
        text("点此打开礼记") with mochaText
    } with runCommand("/plutoproject:checkin gui")
}

val CALENDAR_TITLE = component {
    text("礼记日历 | <time>")
}

val CALENDAR_NAVIGATION = component {
    text("<year> 年 <month> 月") with mochaText without italic()
}

private val CALENDAR_NAVIGATION_LORE_PREV = component {
    text("左键 ") with mochaLavender without italic()
    text("上一页") with mochaText without italic()
}

private val CALENDAR_NAVIGATION_LORE_NEXT = component {
    text("右键 ") with mochaLavender without italic()
    text("下一页") with mochaText without italic()
}

private val CALENDAR_NAVIGATION_LORE_RESET = component {
    text("Shift + 左键 ") with mochaLavender without italic()
    text("回到现在") with mochaText without italic()
}

val CALENDAR_NAVIGATION_LORE_CAN_RESET = listOf(
    Component.empty(),
    CALENDAR_NAVIGATION_LORE_PREV,
    CALENDAR_NAVIGATION_LORE_NEXT,
    CALENDAR_NAVIGATION_LORE_RESET
)

val CALENDAR_NAVIGATION_LORE = listOf(
    Component.empty(),
    CALENDAR_NAVIGATION_LORE_PREV,
    CALENDAR_NAVIGATION_LORE_NEXT,
)

val CALENDAR_NAVIGATION_PREV_REACHED = component {
    text("仅限查看前 ") with mochaSubtext0 without italic()
    text("12 ") with mochaText without italic()
    text("个月的记录") with mochaSubtext0 without italic()
}

val CALENDAR_NAVIGATION_LORE_PREV_REACHED = listOf(
    Component.empty(),
    CALENDAR_NAVIGATION_PREV_REACHED,
    CALENDAR_NAVIGATION_LORE_NEXT,
    CALENDAR_NAVIGATION_LORE_RESET
)

val CALENDAR_DAY = component {
    text("<time>") with mochaText without italic()
}

private val CALENDAR_DAY_UNCHECKED_IN = component {
    text("此日未到访") with mochaSubtext0 without italic()
}

private val CALENDAR_TODAY_UNCHECKED_IN = component {
    text("本日未到访") with mochaSubtext0 without italic()
}

private val CALENDAR_DAY_CHECKED_IN = component {
    text("√ 已到访") with mochaGreen without italic()
}

private val CALENDAR_DAY_OPERATION = component {
    text("左键 ") with mochaLavender without italic()
    text("到访一次") with mochaText without italic()
}

val CALENDAR_DAY_LORE = listOf(
    CALENDAR_TODAY_UNCHECKED_IN,
    component {
        text("可获得奖励 ") with mochaSubtext0 without italic()
        text("<reward>\uD83C\uDF1F") with mochaText without italic()
    },
    Component.empty(),
    CALENDAR_DAY_OPERATION
)

private val CALENDAR_DAY_LORE_TIME = component {
    text("<time>") with mochaSubtext0 without italic()
}

val CALENDAR_DAY_LORE_ALREADY_CHECKED_IN = listOf(
    CALENDAR_DAY_LORE_TIME,
    CALENDAR_DAY_CHECKED_IN
)

val CALENDAR_DAY_LORE_CHECKED_IN_REWARDED = listOf(
    CALENDAR_DAY_LORE_TIME,
    component {
        text("已获得奖励 ") with mochaSubtext0 without italic()
        text("<reward>\uD83C\uDF1F") with mochaText without italic()
    },
    Component.empty(),
    CALENDAR_DAY_CHECKED_IN
)

val CALENDAR_DAY_LORE_PAST = listOf(
    CALENDAR_DAY_UNCHECKED_IN
)

val CALENDAR_DAY_LORE_FEATURE = listOf(
    component {
        text("不远的将来...") with mochaSubtext0 without italic()
    }
)
