package plutoproject.feature.paper.exchangeshop

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.text
import plutoproject.framework.common.util.chat.palettes.*

const val EXCHANGE_SHOP_NAME = "琳物堂"
const val TICKET_NAME = "琳物券"
const val TICKET_SYMBOL = "\uD83C\uDF9F"
const val RECOVERY_SYMBOL = "⌛"

val EXCHANGE_SHOP_BUTTON = component {
    text(EXCHANGE_SHOP_NAME) with mochaPeach
}

val EXCHANGE_SHOP_BUTTON_DESCRIPTION_LINE_1 = component {
    text("出售各类珍品的小店") with mochaSubtext0
}

val EXCHANGE_SHOP_BUTTON_DESCRIPTION_LINE_2 = component {
    text("也许有意想不到的宝贝...") with mochaSubtext0
}

val EXCHANGE_SHOP_BUTTON_OPERATION = component {
    text("左键 ") with mochaLavender
    text("打开$EXCHANGE_SHOP_NAME") with mochaText
}

val EXCHANGE_SHOP_BUTTON_TICKET = component {
    text("$TICKET_SYMBOL $TICKET_NAME ") with mochaText
    text("<ticket>/<cap>") with mochaYellow
}

val EXCHANGE_SHOP_BUTTON_TICKET_RECOVERY_INTERVAL = component {
    text("$RECOVERY_SYMBOL ") with mochaText
    text("<interval> ") with mochaLavender
    text("后恢复") with mochaText
}

val EXCHANGE_SHOP_BUTTON_TICKET_FULL = component {
    text("$RECOVERY_SYMBOL 已完全恢复") with mochaText
}
