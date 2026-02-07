package plutoproject.feature.whitelist_v2.api

import plutoproject.feature.whitelist_v2.api.hook.WhitelistHookParam
import plutoproject.feature.whitelist_v2.api.hook.WhitelistHookType
import java.net.InetAddress
import java.util.UUID

interface Whitelist {
    /**
     * 检查指定 UUID 的玩家是否被授予了白名单。
     *
     * @param uniqueId 要查询的玩家 UUID
     * @return 是否被授予了白名单
     */
    suspend fun isWhitelisted(uniqueId: UUID): Boolean

    /**
     * 查询指定 UUID 的玩家的白名单记录。
     *
     * @param uniqueId 要查询的玩家 UUID
     * @return 该玩家的白名单记录，若未查询到相关记录则为 null
     */
    suspend fun lookupWhitelistRecord(uniqueId: UUID): WhitelistRecord?

    /**
     * 为指定 UUID 的玩家授予白名单。
     *
     * @param uniqueId 要授予的玩家 UUID
     * @param username 要授予的玩家用户名
     * @param operator 白名单操作者
     * @return 若添加成功则为 true，该玩家已被授予白名单则为 false
     */
    suspend fun grantWhitelist(uniqueId: UUID, username: String, operator: WhitelistOperator): Boolean

    /**
     * 为指定 UUID 的玩家撤销白名单。
     *
     * @param uniqueId 要撤销的玩家 UUID
     * @param operator 白名单操作者
     * @param reason 进行撤销的原因
     * @return 若撤销成功则为 true，该玩家未被授予白名单则为 false
     */
    suspend fun revokeWhitelist(uniqueId: UUID, operator: WhitelistOperator, reason: WhitelistRevokeReason): Boolean

    /**
     * 检查指定 UUID 的玩家是否为已知的访客。
     *
     * 仅当该玩家作为访客连接到当前子服时才算已知，
     * 在代理端上所有玩家都视为已连接。
     *
     * 注意：对于此处已知的访客，不代表存在其对应的访问记录。
     * 访问记录只会在访客断开连接后创建。
     *
     * @param uniqueId 要检查的玩家 UUID
     * @return 是否为已知访客
     */
    fun isKnownVisitor(uniqueId: UUID): Boolean

    /**
     * 查询指定 UUID 的玩家的访客记录。
     *
     * @param uniqueId 要查询的玩家 UUID
     * @return 该玩家的来访记录，按先后顺序排列
     */
    suspend fun lookupVisitorRecord(uniqueId: UUID): List<VisitorRecord>

    /**
     * 为指定 UUID 的玩家创建访客记录。
     *
     * @param uniqueId 要创建的玩家 UUID
     * @param params 访客记录参数
     */
    suspend fun createVisitorRecord(uniqueId: UUID, params: VisitorRecordParams): VisitorRecord

    /**
     * 根据 CIDR 网段查询访客记录。
     *
     * @param cidr 要查询的 CIDR 格式网段
     * @return 该网段内的所有访客记录，按先后顺序排列
     */
    suspend fun lookupVisitorRecordsByCidr(cidr: String): List<VisitorRecord>

    /**
     * 根据精确 IP 地址查询访客记录。
     *
     * @param ipAddress 要查询的 IP 地址
     * @return 该 IP 的所有访客记录，按先后顺序排列
     */
    suspend fun lookupVisitorRecordsByIp(ipAddress: InetAddress): List<VisitorRecord>

    /**
     * 注册白名单系统钩子。
     *
     * @param type 钩子类型
     * @param hook 钩子函数
     */
    fun <T : WhitelistHookParam> registerHook(type: WhitelistHookType<T>, hook: (T) -> Unit)
}
