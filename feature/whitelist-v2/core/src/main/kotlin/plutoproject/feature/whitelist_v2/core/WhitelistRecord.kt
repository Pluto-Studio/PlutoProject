package plutoproject.feature.whitelist_v2.core

import java.time.Instant
import java.util.*

class WhitelistRecord(
    val uniqueId: UUID,
    username: String,
    granter: WhitelistOperator,
    joinedAsVisitorBefore: Boolean,
    isRevoked: Boolean,
    revoker: WhitelistOperator?,
    revokeReason: WhitelistRevokeReason?,
    revokeAt: Instant?,
    val createdAt: Instant,
    val isMigrated: Boolean,
) {
    var username = username
        private set
    var granter = granter
        private set
    var joinedAsVisitorBefore = joinedAsVisitorBefore
        private set
    var isRevoked = isRevoked
        private set
    var revoker = revoker
        private set
    var revokeReason = revokeReason
        private set
    var revokeAt = revokeAt
        private set

    fun grant(
        username: String,
        operator: WhitelistOperator,
        joinedAsVisitorBefore: Boolean,
    ) {
        check(isRevoked) { "Cannot grant an active whitelist record" }
        this.username = username
        this.granter = operator
        this.joinedAsVisitorBefore = joinedAsVisitorBefore
        this.isRevoked = false
        this.revoker = null
        this.revokeReason = null
        this.revokeAt = null
    }

    fun revoke(
        operator: WhitelistOperator,
        reason: WhitelistRevokeReason,
        time: Instant
    ) {
        check(!isRevoked) { "Cannot revoke a non-active whitelist record" }
        this.isRevoked = true
        this.revoker = operator
        this.revokeReason = reason
        this.revokeAt = time
    }

    fun changeUsername(username: String) {
        this.username = username
    }
}
