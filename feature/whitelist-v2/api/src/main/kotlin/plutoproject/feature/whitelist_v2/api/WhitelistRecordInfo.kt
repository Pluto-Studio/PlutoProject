package plutoproject.feature.whitelist_v2.api

import java.time.Instant
import java.util.UUID

interface WhitelistRecordInfo {
    /**
     * 该白名单记录的玩家 UUID。
     */
    val uniqueId: UUID

    /**
     * 该白名单记录的用户名。
     *
     * 玩家每次进入服务器时都会进行用户名同步，因此此处将是该玩家最后一次连接时使用的用户名。
     */
    val username: String

    /**
     * 为该玩家授予白名单的操作者。
     *
     * 对于从旧版系统迁移而来的白名单记录，其操作者为 [WhitelistOperator.Console]。
     */
    val granter: WhitelistOperator

    /**
     * 该白名单记录创建的时间。
     */
    val createdAt: Instant

    /**
     * 该玩家是否作为访客加入过服务器。
     *
     * 仅记录此白名单被授予时，数据库中已存在的访客数据。
     */
    val joinedAsVisitorBefore: Boolean

    /**
     * 该白名单记录是否从旧版系统迁移而来。
     */
    val isMigrated: Boolean

    /**
     * 该白名单是否被撤销。
     */
    val isRevoked: Boolean

    /**
     * 撤销该白名单的操作者，若未被撤销则为 null。
     *
     * 旧版系统中对白名单的撤销即删除，因此无法迁移到新版系统。
     */
    val revoker: WhitelistOperator?

    /**
     * 撤销该白名单的原因，若未被撤销则为 null。
     */
    val revokeReason: WhitelistRevokeReason?

    /**
     * 撤销该白名单的时间，若未被撤销则为 null。
     */
    val revokeAt: Instant?
}
