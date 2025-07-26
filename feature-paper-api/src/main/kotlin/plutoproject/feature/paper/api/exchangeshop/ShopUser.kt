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
     * 该玩家的兑换券数。
     *
     * 仅当 [ShopUser] 被加载到内存时会实时恢复，获取 [ShopUser] 时会计算离线恢复量。
     *
     * 对该属性的设置仅保存在内存中，需要调用 [save] 保存。
     */
    var ticket: Int

    /**
     * 该玩家上次恢复兑换券的时间，若还没有恢复过则为空。
     */
    val lastTicketRecoveryOn: Instant?

    /**
     * 减少一定的兑换券数量。
     *
     * 修改后的值仅保存在内存中，需要调用 [save] 保存。
     *
     * @param amount 要减少的兑换券数量
     * @return 减少后剩余的值，若玩家持有的兑换券小于 [amount] 时为 [IllegalArgumentException]
     */
    fun withdrawTicket(amount: Int): Result<Int>

    /**
     * 增加一定的兑换券数量。
     *
     * 修改后的值仅保存在内存中，需要调用 [save] 保存。
     *
     * @param amount 要增加的兑换券数量
     * @return 增加后剩余的值
     */
    fun depositTicket(amount: Int): Int

    /**
     * 查询该玩家的交易记录。
     *
     * @param skip 要跳过的条目数
     * @param limit 返回的条目数限制
     * @param filters 查询条件
     * @return 指定参数下查询到的交易记录，若列表为空则没有匹配的记录
     */
    suspend fun findTransactions(
        skip: Int? = null,
        limit: Int? = null,
        filters: TransactionFilterDsl.() -> Unit = {},
    ): Flow<ShopTransaction>

    /**
     * 查询该玩家的交易记录数。
     *
     * @return 该玩家的交易记录数。
     */
    suspend fun countTransactions(): Int

    /**
     * 为该玩家执行交易。
     *
     * 该操作不会存储在内存中，而是直接写入数据库，仅当数据库操作成功时会给予物品。
     *
     * 若有未保存的操作则会先将它们保存。
     *
     * @param itemId 需要购买的商品 ID
     * @param count 需要购买的数量
     * @return 成功时为 [ShopTransaction]，失败时为 [ShopTransactionException]
     * - [ShopTransactionException.PlayerOffline] 玩家不在线
     * - [ShopTransactionException.TicketNotEnough] 兑换券不足
     * - [ShopTransactionException.BalanceNotEnough] 余额不足
     * - [ShopTransactionException.DatabaseFailure] 数据库操作失败
     */
    suspend fun makeTransaction(itemId: String, count: Int): Result<ShopTransaction>

    /**
     * 将更改存入数据库。
     */
    suspend fun save()
}
