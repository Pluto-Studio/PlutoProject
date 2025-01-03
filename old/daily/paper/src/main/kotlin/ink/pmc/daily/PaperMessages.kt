package ink.pmc.daily

import ink.pmc.advkt.component.*
import ink.pmc.framework.chat.*
import net.kyori.adventure.text.Component

val CHECK_IN = component {
    text("到访成功，本月已连续到访 ") with mochaPink
    text("<acc> ") with mochaText
    text("天") with mochaPink
}

val CHECKED_IN = component {
    text("今日已到访") with mochaSubtext0
}

val COIN_CLAIM = component {
    text("今日到访获得 ") with mochaSubtext0
    text("<amount>$ECONOMY_SYMBOL") with mochaText
}

val UI_TITLE = component {
    text("礼记日历 | <time>")
}

val NAVIGATE = component {
    text("<year> 年 <month> 月") with mochaText without italic()
}

private val NAVIGATE_LORE_PREV = component {
    text("左键 ") with mochaLavender without italic()
    text("上一页") with mochaText without italic()
}

private val NAVIGATE_LORE_NEXT = component {
    text("右键 ") with mochaLavender without italic()
    text("下一页") with mochaText without italic()
}

private val NAVIGATE_LORE_RESET = component {
    text("Shift + 左键 ") with mochaLavender without italic()
    text("回到现在") with mochaText without italic()
}

val NAVIGATE_LORE_CAN_RESET = listOf(
    Component.empty(),
    NAVIGATE_LORE_PREV,
    NAVIGATE_LORE_NEXT,
    NAVIGATE_LORE_RESET
)

val NAVIGATE_LORE = listOf(
    Component.empty(),
    NAVIGATE_LORE_PREV,
    NAVIGATE_LORE_NEXT,
)

val NAVIGATE_PREV_REACHED = component {
    text("仅限查看前 ") with mochaSubtext0 without italic()
    text("12 ") with mochaText without italic()
    text("个月的记录") with mochaSubtext0 without italic()
}

val NAVIGATE_LORE_PREV_REACHED = listOf(
    Component.empty(),
    NAVIGATE_PREV_REACHED,
    NAVIGATE_LORE_NEXT,
    NAVIGATE_LORE_RESET
)

val DAY = component {
    text("<time>") with mochaText without italic()
}

private val DAY_UNCHECKED_IN = component {
    text("此日未到访") with mochaSubtext0 without italic()
}

private val DAY_UNCHECKED_IN_TODAY = component {
    text("本日未到访") with mochaSubtext0 without italic()
}

private val DAY_CHECKED_IN = component {
    text("√ 已到访") with mochaGreen without italic()
}

private val DAY_CHECK_IN_OPREATION = component {
    text("左键 ") with mochaLavender without italic()
    text("到访一次") with mochaText without italic()
}

val DAY_LORE = listOf(
    DAY_UNCHECKED_IN_TODAY,
    component {
        text("可获得奖励 ") with mochaSubtext0 without italic()
        text("<reward>\uD83C\uDF1F") with mochaText without italic()
    },
    Component.empty(),
    DAY_CHECK_IN_OPREATION
)

private val DAY_LORE_TIME = component {
    text("<time>") with mochaSubtext0 without italic()
}

val DAY_LORE_CHECKED_IN = listOf(
    DAY_LORE_TIME,
    DAY_CHECKED_IN
)

val DAY_LORE_CHECKED_IN_REWARDED = listOf(
    DAY_LORE_TIME,
    component {
        text("已获得奖励 ") with mochaSubtext0 without italic()
        text("<reward>\uD83C\uDF1F") with mochaText without italic()
    },
    Component.empty(),
    DAY_CHECKED_IN
)

val DAY_LORE_PAST = listOf(
    DAY_UNCHECKED_IN
)

val DAY_LORE_FUTURE = listOf(
    component {
        text("不远的将来...") with mochaSubtext0 without italic()
    }
)

val PLAYER_NOT_CHECKIN_JOIN = component {
    text("✨ 今日尚未到访，到访可获取货币奖励 ") with mochaText
    text("[打开礼记]") with mochaLavender with showText {
        text("点此打开礼记") with mochaText
    } with runCommand("/plutoproject_daily:checkin gui")
}