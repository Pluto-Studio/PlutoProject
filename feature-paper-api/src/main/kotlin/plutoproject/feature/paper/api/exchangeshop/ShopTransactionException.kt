package plutoproject.feature.paper.api.exchangeshop

import java.math.BigDecimal

/**
 * 代表进行兑换商店交易时发生的异常。
 */
sealed class ShopTransactionException : Exception {
    constructor(user: ShopUser, message: String) : super(transactionError(user, message))
    constructor(user: ShopUser, message: String, cause: Throwable) : super(transactionError(user, message), cause)

    /**
     * 要兑换的商品限期未至。
     */
    class ShopItemNotAvailable(user: ShopUser, item: ShopItem) :
        ShopTransactionException(user, "Shop item ${item.id} is not available today")

    /**
     * 该玩家不在线。
     */
    class PlayerOffline(user: ShopUser) :
        ShopTransactionException(user, "player is offline")

    /**
     * 该玩家所持有的兑换券不足。
     */
    class TicketNotEnough(user: ShopUser, required: Int) :
        ShopTransactionException(user, "insufficient tickets, required: $required")

    /**
     * 该玩家所持有的余额不足。
     */
    class BalanceNotEnough(user: ShopUser, required: BigDecimal) :
        ShopTransactionException(user, "insufficient balance, required: $required")

    /**
     * 数据库操作失败。
     */
    class DatabaseFailure(user: ShopUser, cause: Throwable) :
        ShopTransactionException(user, "database operation failed", cause)
}

private fun transactionError(user: ShopUser, message: String): String {
    return "Error occurred while making transaction for player ${user.player.name}: $message"
}
