package plutoproject.feature.exchangeshop.paper.commands
import plutoproject.kernel.api.koinGet

import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.kernel.api.koinInject
import plutoproject.feature.exchangeshop.api.paper.ShopCategory
import plutoproject.feature.exchangeshop.api.paper.ShopUser
import plutoproject.feature.exchangeshop.paper.*
import plutoproject.feature.exchangeshop.paper.repositories.TransactionRepository
import plutoproject.feature.exchangeshop.paper.repositories.UserRepository
import plutoproject.feature.exchangeshop.paper.ui.ExchangeShopScreen
import plutoproject.feature.exchangeshop.paper.ui.ShopCategoryScreen
import plutoproject.feature.exchangeshop.paper.ui.TransactionHistoryScreen
import plutoproject.foundation.common.text.replace
import plutoproject.foundation.common.time.format
import plutoproject.feature.exchangeshop.paper.startScreen
import plutoproject.foundation.paper.command.ensurePlayer
import java.time.ZoneId

@Suppress("UNUSED")
object ExchangeShopCommand : Any() {
    private val config by koinInject<ExchangeShopConfig>()
    private val userRepo by koinInject<UserRepository>()
    private val transactionRepo by koinInject<TransactionRepository>()

    @Command("exchangeshop")
    @Permission("plutoproject.exchange_shop.command.exchangeshop")
    fun exchangeShop(sender: CommandSender) = sender.ensurePlayer {
        startScreen(ExchangeShopScreen())
    }

    @Command("exchangeshop category <shop-category>")
    @Permission("plutoproject.exchange_shop.command.exchangeshop.category")
    fun exchangeShopCategory(
        sender: CommandSender,
        @Argument("shop-category", parserName = "shop-category") category: ShopCategory
    ) = sender.ensurePlayer {
        startScreen(ShopCategoryScreen(category))
    }

    @Command("exchangeshop transactions <shop-user>")
    @Permission(COMMAND_EXCHANGE_SHOP_TRANSACTIONS_PERMISSION)
    fun exchangeShopTransactions(
        sender: CommandSender,
        @Argument("shop-user", parserName = "shop-user") shopUser: ShopUser
    ) = sender.ensurePlayer {
        startScreen(TransactionHistoryScreen(shopUser))
    }

    @Command("exchangeshop ticket <shop-user>")
    @Permission(COMMAND_EXCHANGE_SHOP_TICKET_PERMISSION)
    fun exchangeShopTicket(
        sender: CommandSender,
        @Argument("shop-user", parserName = "shop-user") shopUser: ShopUser
    ) {
        val player = shopUser.player
        val message = COMMAND_EXCHANGE_SHOP_TICKET
            .replace("<player>", player.name)
            .replace("<currentTicket>", shopUser.ticket)
            .replace("<recoveryCap>", config.ticket.recoveryCap)
            .replace(
                "<lastTicketRecoveryTime>",
                shopUser.lastTicketRecoveryTime?.atZone(ZoneId.systemDefault())?.format() ?: EXCHANGE_SHOP_NONE
            )
            .replace(
                "<nextTicketRecoveryTime>",
                shopUser.scheduledTicketRecoveryTime?.atZone(ZoneId.systemDefault())?.format() ?: EXCHANGE_SHOP_NONE
            )
            .replace(
                "<fullTicketRecoveryTime>",
                shopUser.fullTicketRecoveryTime?.atZone(ZoneId.systemDefault())?.format() ?: EXCHANGE_SHOP_NONE
            )
        sender.sendMessage(message)
    }

    @Command("exchangeshop ticket <shop-user> set <amount>")
    @Permission(COMMAND_EXCHANGE_SHOP_TICKET_SET_PERMISSION)
    suspend fun exchangeShopTicketSet(
        sender: CommandSender,
        @Argument("shop-user", parserName = "shop-user") shopUser: ShopUser,
        @Argument("amount") amount: Long
    ) {
        if (amount < 0) {
            sender.sendMessage(COMMAND_EXCHANGE_SHOP_TICKET_OPERATION_FAILED_CANNOT_BE_NEGATIVE)
            return
        }

        shopUser.setTicket(amount)
        shopUser.save()
        sender.sendMessage(
            COMMAND_EXCHANGE_SHOP_TICKET_SET
                .replace("<player>", shopUser.player.name)
                .replace("<amount>", amount)
        )
    }

    @Command("exchangeshop ticket <shop-user> withdraw <amount>")
    @Permission(COMMAND_EXCHANGE_SHOP_TICKET_WITHDRAW_PERMISSION)
    suspend fun exchangeShopTicketWithdraw(
        sender: CommandSender,
        @Argument("shop-user", parserName = "shop-user") shopUser: ShopUser,
        @Argument("amount") amount: Long
    ) {
        if (amount < 0) {
            sender.sendMessage(COMMAND_EXCHANGE_SHOP_TICKET_OPERATION_FAILED_CANNOT_BE_NEGATIVE)
            return
        }

        if (shopUser.ticket < amount) {
            sender.sendMessage(
                COMMAND_EXCHANGE_SHOP_TICKET_WITHDRAW_FAILED_NOT_ENOUGH
                    .replace("<player>", shopUser.player.name)
            )
            return
        }

        shopUser.withdrawTicket(amount)
        shopUser.save()
        sender.sendMessage(
            COMMAND_EXCHANGE_SHOP_TICKET_WITHDRAW
                .replace("<player>", shopUser.player.name)
                .replace("<amount>", amount)
        )
    }

    @Command("exchangeshop ticket <shop-user> deposit <amount>")
    @Permission(COMMAND_EXCHANGE_SHOP_TICKET_DEPOSIT_PERMISSION)
    suspend fun exchangeShopTicketDeposit(
        sender: CommandSender,
        @Argument("shop-user", parserName = "shop-user") shopUser: ShopUser,
        @Argument("amount") amount: Long
    ) {
        if (amount < 0) {
            sender.sendMessage(COMMAND_EXCHANGE_SHOP_TICKET_OPERATION_FAILED_CANNOT_BE_NEGATIVE)
            return
        }

        shopUser.depositTicket(amount)
        shopUser.save()
        sender.sendMessage(
            COMMAND_EXCHANGE_SHOP_TICKET_DEPOSIT
                .replace("<player>", shopUser.player.name)
                .replace("<amount>", amount)
        )
    }

    @Command("exchangeshop stats")
    @Permission(COMMAND_EXCHANGE_SHOP_STATS_PERMISSION)
    suspend fun exchangeShopStats(sender: CommandSender) {
        val users = userRepo.count()
        val transactions = transactionRepo.count()
        sender.sendMessage(
            COMMAND_EXCHANGE_SHOP_STATS
                .replace("<users>", users)
                .replace("<transactions>", transactions)
        )
    }
}
