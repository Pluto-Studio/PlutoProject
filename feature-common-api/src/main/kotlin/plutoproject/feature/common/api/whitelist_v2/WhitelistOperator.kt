package plutoproject.feature.common.api.whitelist_v2

import java.util.*

/**
 * 代表白名单的授予或撤销的操作者。
 */
sealed class WhitelistOperator {
    /**
     * 代表该白名单的授予或撤销通过控制台命令操作。
     */
    class Console : WhitelistOperator()

    /**
     * 代表该白名单的授予或撤销由管理员通过游戏内命令操作。
     */
    class Administrator(
        /**
         * 进行授予操作的管理员 UUID。
         */
        val uniqueId: UUID
    ) : WhitelistOperator()
}
