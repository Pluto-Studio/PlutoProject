package plutoproject.feature.paper.exchangeshop

import ink.pmc.advkt.component.newline
import ink.pmc.advkt.component.text
import ink.pmc.advkt.send
import kotlinx.coroutines.isActive
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.paper.api.exchangeshop.ExchangeShop
import plutoproject.feature.paper.api.exchangeshop.ShopTransactionException
import plutoproject.framework.common.util.chat.ECONOMY_SYMBOL
import plutoproject.framework.common.util.chat.palettes.mochaLavender
import plutoproject.framework.common.util.chat.palettes.mochaMaroon
import plutoproject.framework.common.util.chat.palettes.mochaSubtext0
import plutoproject.framework.common.util.chat.palettes.mochaText
import java.util.logging.Level

@Suppress("UNUSED")
object TestCommand : KoinComponent {
    private val exchangeShop by inject<InternalExchangeShop>()

    @Command("transaction <player> <category> <shopItemId> <amount> <checkAvailability>")
    suspend fun transaction(
        sender: CommandSender,
        @Argument("player") player: OfflinePlayer,
        @Argument("category") categoryId: String,
        @Argument("shopItemId") shopItemId: String,
        @Argument("amount") amount: Int,
        @Argument("checkAvailability") checkAvailability: Boolean
    ) {
        val user = ExchangeShop.getUser(player.uniqueId)
        val category = ExchangeShop.getCategory(categoryId)
        val item = category?.getItem(shopItemId)

        if (user == null) {
            sender.send {
                text("无法获取 ShopUser 实例") with mochaMaroon
            }
            return
        }

        if (category == null) {
            sender.send {
                text("未找到类别 ") with mochaMaroon
                text(categoryId) with mochaText
            }
            return
        }

        if (item == null) {
            sender.send {
                text("未在类别 ") with mochaMaroon
                text("$shopItemId ") with mochaText
                text("中找到 ") with mochaMaroon
                text(shopItemId) with mochaText
            }
            return
        }

        val transactionResult = user.makeTransaction(item, amount, checkAvailability)

        transactionResult.onFailure {
            when (it) {
                is ShopTransactionException.ShopItemNotAvailable -> sender.send {
                    text("交易失败，物品限期未至") with mochaMaroon
                }

                is ShopTransactionException.PlayerOffline -> sender.send {
                    text("交易失败，玩家不在线") with mochaMaroon
                }

                is ShopTransactionException.TicketNotEnough -> sender.send {
                    text("交易失败，兑换券不足，需要 ") with mochaMaroon
                    text("${it.required} ") with mochaText
                    text("个兑换券") with mochaMaroon
                }

                is ShopTransactionException.BalanceNotEnough -> sender.send {
                    text("交易失败，货币不足，需要 ") with mochaMaroon
                    text("${it.required}$ECONOMY_SYMBOL") with mochaText
                }

                is ShopTransactionException.DatabaseFailure -> sender.send {
                    text("交易失败，数据库操作失败，请查看日志") with mochaMaroon
                    featureLogger.log(Level.SEVERE, "Transaction failed", it)
                }

                else -> sender.send {
                    text("交易失败，未知错误") with mochaMaroon
                }
            }
            return
        }

        val transaction = transactionResult.getOrThrow()

        sender.send {
            text("交易成功！") with mochaText
            newline()
            text("花费 ") with mochaSubtext0
            text("${transaction.cost}$ECONOMY_SYMBOL ") with mochaText
            text("与 ") with mochaSubtext0
            text("${transaction.ticket} ") with mochaText
            text("兑换券购买了 ") with mochaSubtext0
            text("${transaction.itemStack?.type}") with mochaText
        }
    }

    @Command("set-ticket <player> <amount>")
    suspend fun setTicket(
        sender: CommandSender,
        @Argument("player") player: OfflinePlayer,
        @Argument("amount") amount: Long
    ) {
        val user = ExchangeShop.getUser(player.uniqueId)

        if (user == null) {
            sender.send {
                text("无法获取 ShopUser 实例") with mochaMaroon
            }
            return
        }

        user.ticket = amount
        user.save()

        sender.send {
            text("已将 ") with mochaText
            text("${user.player.name} ") with mochaLavender
            text("的兑换券设为 ") with mochaText
            text(amount) with mochaLavender
        }
    }

    @Command("is-exchange-shop-coroutine-active")
    fun isExchangeShopCoroutineActive(sender: CommandSender) {
        sender.send {
            text("活跃状态 ") with mochaText
            text(exchangeShop.coroutineScope.isActive) with mochaLavender
        }
    }
}
