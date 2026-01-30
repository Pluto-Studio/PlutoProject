package plutoproject.feature.paper.api.pvpToggle

import org.bukkit.entity.Player
import plutoproject.framework.common.util.inject.Koin

/**
 * PvP 开关 API。
 */
interface PvPToggle {
    companion object : PvPToggle by Koin.get()

    /**
     * 检查指定的玩家是否开启 PvP。
     *
     * @param player 要检查的玩家
     * @return 该玩家的 PvP 开关状态
     */
    fun isPvPEnabled(player: Player): Boolean

    /**
     * 设置指定的玩家的 PvP 状态。
     *
     * @param player 要设置的玩家
     * @param enabled PvP 状态
     */
    fun setPvPEnabled(player: Player, enabled: Boolean)
}
