package plutoproject.feature.paper.api.sit.player

import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import plutoproject.feature.paper.api.sit.SitOptions
import plutoproject.framework.common.util.inject.Koin

/**
 * 玩家乘坐玩家功能基础接口。
 */
interface PlayerSit {
    companion object : PlayerSit by Koin.get()

    /**
     * 已创建的 [PlayerStack] 实例。
     */
    val stacks: Collection<PlayerStack>

    /**
     * 创建新的玩家堆。
     *
     * @param carrier 初始玩家，将成为最底部的支撑者
     * @param options 该玩家堆的设置项
     */
    fun createStack(carrier: Player, options: StackOptions = StackOptions()): PlayerStack?

    /**
     * 摧毁一个玩家堆。
     *
     * @param stack 要摧毁的玩家堆实例
     * @param cause 摧毁原因，默认为 [PlayerStackDestroyCause.PLUGIN]
     *
     * @see PlayerStack.destroy
     */
    fun destroyStack(stack: PlayerStack, cause: PlayerStackDestroyCause = PlayerStackDestroyCause.PLUGIN): Boolean

    /**
     * 获取一个玩家堆实例。
     *
     * @param player 处于该堆中的一个玩家
     * @return 包含 [player] 的玩家堆实例
     */
    fun getStack(player: Player): PlayerStack?

    /**
     * 获取一个玩家的坐下设置。
     *
     * @param player 需要获取设置的玩家
     * @return 该玩家的坐下设置，若未坐下则为 null
     */
    fun getOptions(player: Player): SitOptions?

    /**
     * 检查一个玩家是否在玩家堆里。
     *
     * @param player 需要检查的玩家
     * @return 若该玩家处于任意一个玩家堆中则为 true，反之 false
     */
    fun isInStack(player: Player): Boolean

    /**
     * 检查一个玩家是否为一个玩家堆中的支撑者。
     *
     * @param player 需要检查的玩家
     * @return 若该玩家处于任意一个玩家堆中且是该堆的支撑者则为 true，反之 false
     */
    fun isCarrier(player: Player): Boolean

    /**
     * 检查一个玩家是否为一个玩家堆中的乘客。
     *
     * @param player 需要检查的玩家
     * @return 若该玩家处于任意一个玩家堆中且是该堆的乘客则为 true，反之 false
     */
    fun isPassenger(player: Player): Boolean

    /**
     * 检查一个实体是否为临时座位实体。
     *
     * PlayerSit 会为每个乘客创建一个 AreaEffectCloud 实体。
     *
     * @param entity 需要检查的实体
     * @return 若该实体是任意一个玩家的临时作为实体则为 true，反之 false
     *
     * @see plutoproject.feature.paper.sit.player.SeatEntity
     */
    fun isTemporarySeatEntity(entity: Entity): Boolean

    /**
     * 检查一个玩家是否开启了 PlayerSit 功能。
     *
     * @param player 需要检查的玩家
     * @return 若该玩家开启了功能则为 true，反之 false
     */
    suspend fun isFeatureEnabled(player: Player): Boolean

    /**
     * 为一个玩家设置 PlayerSit 功能的开启状态。
     *
     * @param player 需要设置的玩家
     * @param state 新的开启状态
     */
    suspend fun toggleFeature(player: Player, state: Boolean)
}
