package plutoproject.feature.whitelist_v2.core

import plutoproject.feature.whitelist_v2.api.VisitorRecord
import plutoproject.feature.whitelist_v2.api.VisitorRecordParams
import plutoproject.feature.whitelist_v2.api.WhitelistOperator
import plutoproject.feature.whitelist_v2.api.WhitelistRecord
import plutoproject.feature.whitelist_v2.api.WhitelistRevokeReason
import java.net.InetAddress
import java.time.Clock
import java.time.Instant
import java.util.UUID

class WhitelistCore(
    private val whitelistRecords: WhitelistRecordRepository,
    private val visitorRecords: VisitorRecordRepository,
    private val clock: Clock,
) {
    suspend fun isWhitelisted(uniqueId: UUID): Boolean {
        return whitelistRecords.hasActiveByUniqueId(uniqueId)
    }

    suspend fun lookupWhitelistRecord(uniqueId: UUID): WhitelistRecord? {
        return whitelistRecords.findByUniqueId(uniqueId)
    }

    suspend fun grantWhitelist(uniqueId: UUID, username: String, operator: WhitelistOperator): Boolean {
        if (whitelistRecords.hasActiveByUniqueId(uniqueId)) {
            return false
        }

        val hasVisitorRecord = visitorRecords.hasByUniqueId(uniqueId)
        val existing = whitelistRecords.findByUniqueId(uniqueId)

        val record = if (existing != null) {
            existing.copy(
                username = username,
                granter = operator,
                joinedAsVisitorBefore = hasVisitorRecord,
                isRevoked = false,
                revoker = null,
                revokeReason = null,
                revokeAt = null,
            )
        } else {
            WhitelistRecordData(
                uniqueId = uniqueId,
                username = username,
                granter = operator,
                createdAt = clock.instant(),
                joinedAsVisitorBefore = hasVisitorRecord,
                isMigrated = false,
                isRevoked = false,
                revoker = null,
                revokeReason = null,
                revokeAt = null,
            )
        }

        whitelistRecords.saveOrUpdate(record)
        return true
    }

    suspend fun revokeWhitelist(uniqueId: UUID, operator: WhitelistOperator, reason: WhitelistRevokeReason): Boolean {
        val record = whitelistRecords.findActiveByUniqueId(uniqueId) ?: return false

        whitelistRecords.saveOrUpdate(
            record.copy(
                isRevoked = true,
                revoker = operator,
                revokeReason = reason,
                revokeAt = clock.instant(),
            )
        )
        return true
    }

    suspend fun lookupVisitorRecord(uniqueId: UUID): List<VisitorRecord> {
        return visitorRecords.findByUniqueId(uniqueId)
    }

    suspend fun createVisitorRecord(uniqueId: UUID, params: VisitorRecordParams): VisitorRecord {
        val record = VisitorRecordData(
            uniqueId = uniqueId,
            ipAddress = params.ipAddress,
            virtualHost = params.virtualHost,
            visitedAt = params.visitedAt,
            createdAt = clock.instant(),
            duration = params.duration,
            visitedServers = params.visitedServers,
        )
        visitorRecords.save(record)
        return record
    }

    suspend fun lookupVisitorRecordsByCidr(cidr: String): List<VisitorRecord> {
        return visitorRecords.findByCidr(cidr)
    }

    suspend fun lookupVisitorRecordsByIp(ipAddress: InetAddress): List<VisitorRecord> {
        return visitorRecords.findByIpAddress(ipAddress)
    }
}
