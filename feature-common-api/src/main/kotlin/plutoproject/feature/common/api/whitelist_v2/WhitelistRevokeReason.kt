package plutoproject.feature.common.api.whitelist_v2

/**
 * 代表一次白名单撤销操作的原因。
 */
enum class WhitelistRevokeReason {
    /**
     * 因该玩家违规被撤销。
     */
    VIOLATION,

    /**
     * 因该玩家主动请求撤销。
     */
    REQUESTED,

    /**
     * 其他原因，此类型的撤销操作只能通过 API 进行。
     */
    OTHER
}
