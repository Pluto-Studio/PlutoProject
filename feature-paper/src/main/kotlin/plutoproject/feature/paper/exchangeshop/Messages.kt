package plutoproject.feature.paper.exchangeshop

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.newline
import ink.pmc.advkt.component.text
import plutoproject.framework.common.util.chat.ECONOMY_SYMBOL
import plutoproject.framework.common.util.chat.palettes.*

const val EXCHANGE_SHOP_NAME = "琳物堂"
const val TICKET_NAME = "琳物券"
const val TICKET_SYMBOL = "\uD83C\uDF9F"
const val RECOVERY_SYMBOL = "⌛"
const val MULTIPLICATION_SYMBOL = "×"

val EXCHANGE_SHOP_BUTTON = component {
    text(EXCHANGE_SHOP_NAME) with mochaPeach
}

val EXCHANGE_SHOP_BUTTON_LORE_DESC_LINE_1 = component {
    text("出售各类珍品的小店") with mochaSubtext0
}

val EXCHANGE_SHOP_BUTTON_LORE_DESC_LINE_2 = component {
    text("也许有意想不到的宝贝...") with mochaSubtext0
}

val EXCHANGE_SHOP_BUTTON_LORE_OPERATION = component {
    text("左键 ") with mochaLavender
    text("打开$EXCHANGE_SHOP_NAME") with mochaText
}

val EXCHANGE_SHOP_BUTTON_LORE_TICKET = component {
    text("$TICKET_SYMBOL $TICKET_NAME ") with mochaText
    text("<ticket>/<cap>") with mochaYellow
}

val EXCHANGE_SHOP_BUTTON_LORE_TICKET_RECOVERY_INTERVAL = component {
    text("$RECOVERY_SYMBOL ") with mochaText
    text("<interval> ") with mochaLavender
    text("后恢复") with mochaText
}

val EXCHANGE_SHOP_BUTTON_LORE_TICKET_FULL = component {
    text("$RECOVERY_SYMBOL 已完全恢复") with mochaText
}

val EXCHANGE_SHOP_AVAILABILITY = component {
    text("限期供应") with mochaText
}

val EXCHANGE_SHOP_ECONOMY = component {
    text("星币") with mochaText
}

val EXCHANGE_SHOP_ECONOMY_BALANCE = component {
    text("余额: ") with mochaSubtext0
    text("<balance>$ECONOMY_SYMBOL") with mochaText
}

val EXCHANGE_SHOP_ECONOMY_BALANCE_LORE_DESC = component {
    text("可在「礼记」中到访以获取星币") with mochaSubtext0
}

val EXCHANGE_SHOP_ECONOMY_BALANCE_LORE_OPERATION = component {
    text("左键 ") with mochaLavender
    text("打开礼记日历") with mochaText
}

val EXCHANGE_SHOP_TICKET = component {
    text(TICKET_NAME) with mochaText
}

val EXCHANGE_SHOP_TICKET_LORE_TICKET = component {
    text("$TICKET_SYMBOL ") with mochaText
    text("<ticket>/<cap>") with mochaYellow
}

val EXCHANGE_SHOP_TICKET_LORE_DESC_1 = component {
    text("在店内购买商品时") with mochaSubtext0
}

val EXCHANGE_SHOP_TICKET_LORE_DESC_2 = component {
    text("除了支付星币外，还需支付${TICKET_NAME}") with mochaSubtext0
}

val EXCHANGE_SHOP_TICKET_LORE_DESC_3 = component {
    text("它会随着时间流逝恢复") with mochaSubtext0
}

val EXCHANGE_SHOP_TICKET_LORE_OPERATION_BUY_TICKET = component {
    text("左键 ") with mochaLavender
    text("使用星币补充") with mochaText
}

val EXCHANGE_SHOP_TRANSACTION_HISTORY = component {
    text("购买记录") with mochaText
}

val EXCHANGE_SHOP_TRANSACTION_HISTORY_LORE_DESC = component {
    text("已购买过 ") with mochaSubtext0
    text("<time> ") with mochaText
    text("次商品") with mochaSubtext0
}

val EXCHANGE_SHOP_TRANSACTION_HISTORY_LORE_DESC_NO_PURCHASE = component {
    text("尚未购买过商品") with mochaSubtext0
}

val EXCHANGE_SHOP_LOADING = component {
    text("<spinner> 正在加载...") with mochaSubtext0
}

val EXCHANGE_SHOP_TRANSACTION_HISTORY_LORE_OPERATION = component {
    text("左键 ") with mochaLavender
    text("查看购买记录") with mochaText
}

val EXCHANGE_SHOP_TRANSACTION_HISTORY_TITLE = component {
    text("购买记录")
}

val TRANSACTION_HISTORY = component {
    text("<name>") with mochaText
}

val TRANSACTION_HISTORY_TIME = component {
    text("购买时间: ") with mochaSubtext0
    text("<time>") with mochaText
}

val TRANSACTION_HISTORY_QUANTITY = component {
    text("数量: ") with mochaSubtext0
    text("<quantity>") with mochaText
}

val TRANSACTION_HISTORY_COST = component {
    text("支出货币: ") with mochaSubtext0
    text("<money>$ECONOMY_SYMBOL") with mochaText
}

val TRANSACTION_HISTORY_TICKET = component {
    text("消耗$TICKET_NAME: ") with mochaSubtext0
    text("<ticket>") with mochaText
}

val TRANSACTION_HISTORY_BALANCE = component {
    text("结余: ") with mochaSubtext0
    text("<balance>$ECONOMY_SYMBOL") with mochaText
}

val TRANSACTION_HISTORY_UNKNOWN_ITEM = component {
    text("❓ 未知物品") with mochaText
}

val TRANSACTION_HISTORY_LORE_UNKNOWN_ITEM_DESC_1 = component {
    text("此物品的类型无法被识别") with mochaMaroon
}

val TRANSACTION_HISTORY_LORE_UNKNOWN_ITEM_DESC_2 = component {
    text("这可能是一个服务器错误，请向管理组反馈") with mochaMaroon
}

val EXCHANGE_SHOP_AVAILABILITY_LORE_DESC_1 = component {
    text("一些商品的获取途径有限，需要限期供应") with mochaSubtext0
}

val EXCHANGE_SHOP_AVAILABILITY_LORE_DESC_2 = component {
    text("限期供应的商品仅在每周的特定日出售") with mochaSubtext0
}

val EXCHANGE_SHOP_AVAILABILITY_LORE_DESC_3 = component {
    text("以北京时间为准") with mochaSubtext0
}

val EXCHANGE_SHOP_AVAILABILITY_LORE_SHOW_UNAVAILABLE_ITEMS_ON = component {
    text("展示未出售的商品 ") with mochaText
    text("开") with mochaGreen
}

val EXCHANGE_SHOP_AVAILABILITY_LORE_SHOW_UNAVAILABLE_ITEMS_OFF = component {
    text("展示未出售的商品 ") with mochaText
    text("关") with mochaMaroon
}

val EXCHANGE_SHOP_AVAILABILITY_LORE_OPERATION_SHOW_UNAVAILABLE_ITEMS = component {
    text("左键 ") with mochaLavender
    text("开启展示") with mochaText
}

val EXCHANGE_SHOP_AVAILABILITY_LORE_OPERATION_HIDE_UNAVAILABLE_ITEMS = component {
    text("左键 ") with mochaLavender
    text("关闭展示") with mochaText
}

val EXCHANGE_SHOP_CATEGORY_SELECT_LORE_OPERATION = component {
    text("左键 ") with mochaLavender
    text("打开分类") with mochaText
}

val EXCHANGE_SHOP_CATEGORY_SELECT_LORE_ITEMS = component {
    text("内含 ") with mochaSubtext0
    text("<items> ") with mochaText
    text("个商品") with mochaSubtext0
}

val CATEGORY_PAGE_INDICATOR = component {
    text("第 <currentPage>/<totalPages> 页") with mochaText
}

val CATEGORY_PREVIOUS_PAGE_LORE_OPERATION = component {
    text("左键 ") with mochaLavender
    text("上一页") with mochaText
}

val CATEGORY_NEXT_PAGE_LORE_OPERATION = component {
    text("左键 ") with mochaLavender
    text("下一页") with mochaText
}

val SHOP_ITEM_LORE_AVAILABLE_DAYS = component {
    text("✨ 限期供应 ") with mochaYellow
}

val SHOP_ITEM_LORE_AVAILABLE_DAYS_DAY_OF_WEEK = component {
    text("<day>") with mochaText
}

val SHOP_ITEM_LORE_AVAILABLE_DAYS_SEPARATOR = component {
    text(" / ") with mochaText
}

val SHOP_ITEM_LORE_PRICE_COST = component {
    text("<cost>$ECONOMY_SYMBOL ") with mochaText
}

val SHOP_ITEM_LORE_PRICE_AND = component {
    text("与 ") with mochaSubtext0
}

val SHOP_ITEM_LORE_PRICE_TICKET = component {
    text("<ticket> ") with mochaText
    text("$TICKET_NAME ") with mochaSubtext0
}

val SHOP_ITEM_LORE_QUANTITY_SINGLE = component {
    text("/ 个") with mochaSubtext0
}

val SHOP_ITEM_LORE_QUANTITY_MULTIPLE = component {
    text("/ ") with mochaSubtext0
    text("<quantity >") with mochaText
    text("个") with mochaSubtext0
}

val SHOP_ITEM_LORE_PRICE_FREE = component {
    text("免费") with mochaText
}

val SHOP_ITEM_LORE_PURCHASABLE_QUANTITY = component {
    text("可购买 ") with mochaSubtext0
    text("<quantity> ") with mochaText
    text("个") with mochaSubtext0
}

val SHOP_ITEM_LORE_UNAVAILABLE = component {
    text("今日未出售") with mochaSubtext0
}

val SHOP_ITEM_LORE_BALANCE_NOT_ENOUGH = component {
    text("星币不足") with mochaMaroon
}

val SHOP_ITEM_LORE_TICKET_NOT_ENOUGH = component {
    text("${TICKET_NAME}不足") with mochaMaroon
}

val SHOP_ITEM_LORE_OPERATION = component {
    text("左键 ") with mochaLavender
    text("购买商品") with mochaText
}

val SHOP_ITEM_PURCHASE_TITLE = component {
    text("购买商品") with mochaText
}

val SHOP_ITEM_PURCHASE_PURCHASING = component {
    text("正在购买 ") with mochaText
    text("<item>") with mochaLavender
}

val SHOP_ITEM_PURCHASE_QUANTITY = component {
    text("购买数量")
}

val SHOP_ITEM_PURCHASE_BACK = component {
    text("返回")
}

val SHOP_ITEM_PURCHASE_SUBMIT = component {
    text("购买")
}

val SHOP_ITEM_PURCHASE_CONFIRMATION_TITLE = component {
    text("确认购买") with mochaText
}

val SHOP_ITEM_PURCHASE_CONFIRMATION = component {
    text("确认要购买 ") with mochaText
    text("<item> $MULTIPLICATION_SYMBOL <quantity> ") with mochaLavender
    text("吗?") with mochaText
}

val SHOP_ITEM_PURCHASE_CONFIRMATION_PRICE = component {
    text("将花费 ") with mochaText
}

val SHOP_ITEM_PURCHASE_CONFIRMATION_PRICE_MONEY = component {
    text("<cost>$ECONOMY_SYMBOL") with mochaLavender
}

val SHOP_ITEM_PURCHASE_CONFIRMATION_PRICE_AND = component {
    text(" 与 ") with mochaText
}

val SHOP_ITEM_PURCHASE_CONFIRMATION_PRICE_TICKET = component {
    text("<ticket> ") with mochaLavender
    text(TICKET_NAME) with mochaText
}

val SHOP_ITEM_PURCHASE_CONFIRMATION_CANCEL = component {
    text("取消购买")
}

val SHOP_ITEM_PURCHASE_CONFIRMATION_CONFIRM = component {
    text("确认购买")
}

val SHOP_ITEM_PURCHASE_CONFIRMATION_SUCCEED = component {
    text("√ 购买成功") with mochaGreen
}

val SHOP_ITEM_PURCHASE_CONFIRMATION_BALANCE_NOT_ENOUGH = component {
    text("星币不足") with mochaMaroon
}

val SHOP_ITEM_PURCHASE_CONFIRMATION_TICKET_NOT_ENOUGH = component {
    text("${TICKET_NAME}不足") with mochaMaroon
}

val SHOP_ITEM_PURCHASE_CONFIRMATION_DATABASE_FAILURE = component {
    text("数据库操作失败") with mochaMaroon
    newline()
    text("这可能是一个服务器错误，请向管理组反馈") with mochaText
}

val SHOP_ITEM_PURCHASE_CONFIRMATION_ITEM_NOT_AVAILABLE = component {
    text("该商品今日未出售") with mochaMaroon
}

val COMMAND_EXCHANGE_SHOP_CATEGORY_NOT_FOUND = component {
    text("名为 ") with mochaMaroon
    text("<categoryId> ") with mochaText
    text("的类别未找到") with mochaMaroon
}

val COMMAND_EXCHANGE_SHOP_TRANSACTIONS_SHOP_USER_NOT_FOUND = component {
    text("玩家 ") with mochaMaroon
    text("<input> ") with mochaText
    text("的用户数据未找到") with mochaMaroon
}

const val EXCHANGE_SHOP_NONE = "无"

val COMMAND_EXCHANGE_SHOP_TICKET = component {
    text("» ") with mochaSubtext0
    text("玩家 ") with mochaText
    text("<player> ") with mochaFlamingo
    text("的${TICKET_NAME}信息") with mochaText
    newline()

    text("- ") with mochaSubtext0
    text("当前持有: ") with mochaText
    text("<currentTicket>/<recoveryCap>") with mochaYellow
    newline()

    text("- ") with mochaSubtext0
    text("上次恢复时间: ") with mochaText
    text("<lastTicketRecoveryTime>") with mochaLavender
    newline()

    text("- ") with mochaSubtext0
    text("计划恢复时间: ") with mochaText
    text("<nextTicketRecoveryTime>") with mochaLavender
    newline()

    text("- ") with mochaSubtext0
    text("完全恢复时间: ") with mochaText
    text("<fullTicketRecoveryTime>") with mochaLavender
}

val COMMAND_EXCHANGE_SHOP_TICKET_OPERATION_FAILED_CANNOT_BE_NEGATIVE = component {
    text("${TICKET_NAME}不能是负数") with mochaMaroon
}

val COMMAND_EXCHANGE_SHOP_TICKET_SET = component {
    text("已将玩家 ") with mochaText
    text("<player> ") with mochaFlamingo
    text("的${TICKET_NAME}余额设置为 ") with mochaText
    text("<amount>") with mochaLavender
}

val COMMAND_EXCHANGE_SHOP_TICKET_WITHDRAW = component {
    text("已为玩家 ") with mochaText
    text("<player> ") with mochaFlamingo
    text("减少 ") with mochaText
    text("<amount> ") with mochaLavender
    text(TICKET_NAME) with mochaText
}

val COMMAND_EXCHANGE_SHOP_TICKET_WITHDRAW_FAILED_NOT_ENOUGH = component {
    text("玩家 ") with mochaMaroon
    text("<player> ") with mochaText
    text("的${TICKET_NAME}不足") with mochaMaroon
}

val COMMAND_EXCHANGE_SHOP_TICKET_DEPOSIT = component {
    text("已为玩家 ") with mochaText
    text("<player> ") with mochaFlamingo
    text("增加 ") with mochaText
    text("<amount> ") with mochaLavender
    text(TICKET_NAME) with mochaText
}

val COMMAND_EXCHANGE_SHOP_STATS = component {
    text("数据库中共有 ") with mochaText
    text("<users> ") with mochaLavender
    text("条玩家数据与 ") with mochaText
    text("<transactions> ") with mochaLavender
    text("条交易信息") with mochaText
}
