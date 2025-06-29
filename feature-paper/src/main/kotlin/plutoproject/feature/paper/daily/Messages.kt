package plutoproject.feature.paper.daily

import ink.pmc.advkt.component.*
import net.kyori.adventure.text.Component
import plutoproject.framework.common.util.chat.ECONOMY_SYMBOL
import plutoproject.framework.common.util.chat.palettes.*

val CHECK_IN = component {
    text("到访成功，本月已连续到访 ") with mochaPink
    text("<acc> ") with mochaText
    text("天") with mochaPink
}

val COMMAND_CHECKIN_ALREADY_CHECKIN = component {
    text("今日已到访") with mochaSubtext0
}

val NOT_CHECKED_IN_TODAY = component {
    text("✨ 今日尚未到访，到访可获取货币奖励 ") with mochaText
    text("[打开礼记]") with mochaLavender with showText {
        text("点此打开礼记") with mochaText
    } with runCommand("/plutoproject:dailycalender")
}

val COIN_CLAIMED = component {
    text("今日到访获得 ") with mochaSubtext0
    text("<amount>${ECONOMY_SYMBOL}") with mochaText
}

val UI_CALENDAR_TITLE = component {
    text("礼记日历 | <time>")
}

val UI_CALENDAR_NAVIGATION = component {
    text("<year> 年 <month> 月") with mochaText
}

val UI_CALENDAR_NAVIGATION_GO_PREVIOUS = component {
    text("左键 ") with mochaLavender
    text("上一页") with mochaText
}

val UI_CALENDAR_NAVIGATION_GO_NEXT = component {
    text("右键 ") with mochaLavender
    text("下一页") with mochaText
}

val UI_CALENDAR_NAVIGATION_RESET = component {
    text("Shift + 左键 ") with mochaLavender
    text("回到现在") with mochaText
}

val UI_CALENDAR_NAVIGATION_LORE_TODAY = listOf(
    Component.empty(),
    UI_CALENDAR_NAVIGATION_GO_PREVIOUS,
    UI_CALENDAR_NAVIGATION_GO_NEXT,
)

val UI_CALENDAR_NAVIGATION_LORE_DIFFERENT_DAY = listOf(
    Component.empty(),
    UI_CALENDAR_NAVIGATION_GO_PREVIOUS,
    UI_CALENDAR_NAVIGATION_GO_NEXT,
    UI_CALENDAR_NAVIGATION_RESET
)

val UI_CALENDAR_NAVIGATION_PREV_REACHED = component {
    text("仅限查看前 ") with mochaSubtext0
    text("12 ") with mochaText
    text("个月的记录") with mochaSubtext0
}

val UI_CALENDAR_NAVIGATION_LORE_PREVIOUS_LIMIT_REACHED = listOf(
    Component.empty(),
    UI_CALENDAR_NAVIGATION_PREV_REACHED,
    UI_CALENDAR_NAVIGATION_GO_NEXT,
    UI_CALENDAR_NAVIGATION_RESET
)

val UI_CALENDAR_DAY_DATE = component {
    text("<date>") with mochaText
}

val UI_CALENDAR_DAY_NOT_CHECKED_IN = component {
    text("此日未到访") with mochaSubtext0
}

val UI_CALENDAR_TODAY_NOT_CHECKED_IN = component {
    text("本日未到访") with mochaSubtext0
}

val UI_CALENDAR_DAY_CHECKED_IN = component {
    text("√ 已到访") with mochaGreen
}

val UI_CALENDAR_DAY_OPERATION = component {
    text("左键 ") with mochaLavender
    text("到访一次") with mochaText
}

val UI_CALENDAR_DAY_REWARD_AMOUNT = component {
    text("可获得奖励 ") with mochaSubtext0
    text("<reward>$ECONOMY_SYMBOL") with mochaText
}

val UI_CALENDAR_DAY_TIME = component {
    text("<time>") with mochaSubtext0
}

val UI_CALENDAR_DAY_LORE_TODAY_NOT_CHECKED_IN = listOf(
    UI_CALENDAR_TODAY_NOT_CHECKED_IN,
    UI_CALENDAR_DAY_REWARD_AMOUNT,
    Component.empty(),
    UI_CALENDAR_DAY_OPERATION
)

val UI_CALENDAR_DAY_LORE_CHECKED_IN = listOf(
    UI_CALENDAR_DAY_TIME,
    UI_CALENDAR_DAY_CHECKED_IN
)

val UI_CALENDAR_DAY_OBTAINED_REWARD_AMOUNT = component {
    text("已获得奖励 ") with mochaSubtext0
    text("<reward>\uD83C\uDF1F") with mochaText
}

val UI_CALENDAR_DAY_LORE_CHECKED_IN_REWARD = listOf(
    UI_CALENDAR_DAY_TIME,
    UI_CALENDAR_DAY_OBTAINED_REWARD_AMOUNT,
    Component.empty(),
    UI_CALENDAR_DAY_CHECKED_IN
)

val UI_CALENDAR_DAY_LORE_PAST_NOT_CHECKED_IN = listOf(
    UI_CALENDAR_DAY_NOT_CHECKED_IN
)

val UI_CALENDAR_DAY_LORE_FEATURE = listOf(
    component {
        text("不远的将来...") with mochaSubtext0
    }
)
