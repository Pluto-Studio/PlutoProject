package plutoproject.feature.paper.api.exchangeshop

import kotlinx.coroutines.flow.Flow
import org.bukkit.OfflinePlayer
import java.time.Instant
import java.util.*

/**
 * 代表兑换商店中玩家的用户数据。
 */
interface ShopUser {
    /**
     * 该玩家的 UUID。
     */
    val uniqueId: UUID

    /**
     * 该玩家的 Bukkit 玩家对象。
     */
    val player: OfflinePlayer

    /**
     * 该玩家数据创建的时间。
     */
    val createdAt: Instant

    /**
     * 该 [ShopUser] 实例是否可用，用于判断是否被卸载。
     *
     * 被卸载后尝试进行任何操作都会抛出 [IllegalStateException]。
     */
    val isValid: Boolean

    /**
     * 该玩家的兑换券数。
     *
     * 仅当 [ShopUser] 被加载到内存时会实时恢复，获取 [ShopUser] 时会计算离线恢复量。
     *
     * 对该属性的设置操作可能需要获取锁，等待锁时会阻塞，可使用 [setTicket] 以避免。
     *
     * 修改后的值仅保存在内存中，需要调用 [save] 保存。
     */
    var ticket: Long

    /**
     * 该玩家上次恢复兑换券的时间，若还没有恢复过则为空。
     */
    val lastTicketRecoveryTime: Instant?

    /**
     * 该玩家下次恢复兑换券的时间，若还没有计划恢复则为空。
     */
    val scheduledTicketRecoveryTime: Instant?

    /**
     * 该玩家兑换券恢复满的时间，若已满则为空。
     */
    val fullTicketRecoveryTime: Instant?

    /**
     * 减少一定的兑换券数量。
     *
     * 此操作可能需要获取锁，等待锁时会挂起。
     *
     * 修改后的值仅保存在内存中，需要调用 [save] 保存。
     *
     * @param amount 要减少的兑换券数量
     * @return 减少后剩余的值
     * @throws IllegalArgumentException 若 [amount] 为负数或当玩家剩余兑换券数量小于 [amount] 时
     */
    suspend fun withdrawTicket(amount: Long): Long

    /**
     * 增加一定的兑换券数量。
     *
     * 此操作可能需要获取锁，等待锁时会挂起。
     *
     * 修改后的值仅保存在内存中，需要调用 [save] 保存。
     *
     * @param amount 要增加的兑换券数量
     * @return 增加后剩余的值
     * @throws IllegalArgumentException 若 [amount] 为负数
     */
    suspend fun depositTicket(amount: Long): Long

    /**
     * 设置一定的兑换券数量。
     *
     * 此操作可能需要获取锁，等待锁时会挂起。
     *
     * 修改后的值仅保存在内存中，需要调用 [save] 保存。
     *
     * @param amount 要设置的兑换券数量
     * @return 设置后剩余的值
     * @throws IllegalArgumentException 若 [amount] 为负数
     */
    suspend fun setTicket(amount: Long): Long

    /**
     * 获取该玩家所剩资源（货币与兑换券）还能购买多少个指定物品。
     *
     * @return 该玩家所剩资源还可购买指定商品的数量，若商品免费则为 [Long.MAX_VALUE]
     */
    fun calculatePurchasableQuantity(shopItem: ShopItem): Long

    /**
     * 查询该玩家的交易记录。
     *
     * @param skip 要跳过的条目数
     * @param limit 返回的条目数限制
     * @param filterBlock 查询条件
     * @return 指定条件下查询到的交易记录，若列表为空则没有匹配的记录
     */
    fun findTransactions(
        skip: Int? = null,
        limit: Int? = null,
        filterBlock: TransactionFilterDsl.() -> Unit = {},
    ): Flow<ShopTransaction>

    /**
     * 统计该玩家的交易记录数。
     *
     * @param filterBlock 统计条件
     * @return 指定条件下统计到的交易记录数
     */
    suspend fun countTransactions(filterBlock: TransactionFilterDsl.() -> Unit = {}): Long

    /**
     * 为该玩家执行交易。
     *
     * 该操作不会存储在内存中，而是直接写入数据库，仅当数据库操作成功时会给予物品。
     *
     * 若有未保存的操作则会先将它们保存。
     *
     * @param shopItem 需要购买的商品
     * @param amount 需要购买的数量
     * @param checkAvailability 是否检查商品限期，若配置文件中关闭了限期功能则无论如何都不检查
     * @return 成功时为 [ShopTransaction]，失败时为 [ShopTransactionException]
     * - [ShopTransactionException.ShopItemNotAvailable] 兑换的商品限期未至
     * - [ShopTransactionException.PlayerOffline] 玩家不在线
     * - [ShopTransactionException.TicketNotEnough] 兑换券不足
     * - [ShopTransactionException.BalanceNotEnough] 余额不足
     * - [ShopTransactionException.DatabaseFailure] 数据库操作失败
     */
    suspend fun makeTransaction(
        shopItem: ShopItem,
        amount: Int,
        checkAvailability: Boolean = true
    ): Result<ShopTransaction>

    /**
     * 为该玩家执行批量交易。
     *
     * 该操作不会存储在内存中，而是直接写入数据库，仅当数据库操作成功时会给予物品。
     *
     * 每条交易会被独立执行，并各自返回一条交易记录。
     *
     * @param purchases 需要执行的交易，key 为商品，value 为要购买的数量
     * @return 交易结果，每个商品 ID 对应一个 [Result]
     * @see makeTransaction
     */
    suspend fun batchTransaction(purchases: Map<ShopItem, ShopTransactionParameters>): Map<ShopItem, Result<ShopTransaction>>

    /**
     * 将更改存入数据库。
     */
    suspend fun save()
}
