package plutoproject.feature.paper.api.exchangeshop

/**
 * 代表批量交易时的交易参数。
 */
data class ShopTransactionParameters(
    /**
     * 需要购买的数量。
     */
    val amount: Int,

    /**
     * 是否检查商品限期。
     */
    val checkAvailability: Boolean = true,
)
