package plutoproject.feature.common.whitelist_v2

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.common.api.whitelist_v2.*
import plutoproject.feature.common.whitelist_v2.model.*
import plutoproject.feature.common.whitelist_v2.repository.VisitorRecordRepository
import plutoproject.feature.common.whitelist_v2.repository.WhitelistRecordRepository
import plutoproject.framework.common.util.network.parseInetSocketAddress
import plutoproject.framework.common.util.network.toHostPortString
import java.net.Inet6Address
import java.net.InetAddress
import java.util.*
import kotlin.time.Duration

class WhitelistImpl : Whitelist, KoinComponent {
    private val whitelistRecordRepository: WhitelistRecordRepository by inject()
    private val visitorRecordRepository: VisitorRecordRepository by inject()

    private val knownVisitors = mutableSetOf<UUID>()

    override suspend fun isWhitelisted(uniqueId: UUID): Boolean {
        return whitelistRecordRepository.hasActiveByUniqueId(uniqueId)
    }

    override suspend fun lookupWhitelistRecord(uniqueId: UUID): WhitelistRecord? {
        val model = whitelistRecordRepository.findByUniqueId(uniqueId) ?: return null
        return model.toWhitelistRecord()
    }

    override suspend fun grantWhitelist(
        uniqueId: UUID,
        operator: WhitelistOperator
    ): Boolean {
        if (whitelistRecordRepository.hasActiveByUniqueId(uniqueId)) {
            return false
        }

        val existingRecord = whitelistRecordRepository.findByUniqueId(uniqueId)
        if (existingRecord != null && !existingRecord.isRevoked) {
            return false
        }

        val hasVisitorRecord = visitorRecordRepository.hasByUniqueId(uniqueId)
        val username = "Unknown"

        val model = WhitelistRecordModel(
            uniqueId = uniqueId,
            username = username,
            granter = operator.toModel(),
            joinedAsVisitorBefore = hasVisitorRecord,
            isMigrated = false,
            isRevoked = false,
            revoker = null,
            revokeReason = null
        )

        whitelistRecordRepository.save(model)
        return true
    }

    override suspend fun revokeWhitelist(
        uniqueId: UUID,
        operator: WhitelistOperator,
        reason: WhitelistRevokeReason
    ): Boolean {
        val model = whitelistRecordRepository.findActiveByUniqueId(uniqueId) ?: return false

        val updatedModel = model.copy(
            isRevoked = true,
            revoker = operator.toModel(),
            revokeReason = reason
        )

        whitelistRecordRepository.update(updatedModel)
        return true
    }

    override fun isKnownVisitor(uniqueId: UUID): Boolean {
        return knownVisitors.contains(uniqueId)
    }

    override suspend fun lookupVisitorRecord(uniqueId: UUID): List<VisitorRecord> {
        return visitorRecordRepository.findByUniqueId(uniqueId).map { it.toVisitorRecord() }
    }

    override suspend fun createVisitorRecord(
        uniqueId: UUID,
        params: VisitorRecordParams
    ): VisitorRecord {
        val model = VisitorRecordModel(
            uniqueId = uniqueId,
            ipAddress = params.ipAddress.toModel(),
            virtualHost = params.virtualHost.toHostPortString(),
            visitedAt = params.visitedAt,
            durationMillis = params.duration.inWholeMilliseconds,
            visitedServers = params.visitedServers
        )

        visitorRecordRepository.save(model)
        return model.toVisitorRecord()
    }

    fun addKnownVisitor(uniqueId: UUID) {
        knownVisitors.add(uniqueId)
    }

    fun removeKnownVisitor(uniqueId: UUID) {
        knownVisitors.remove(uniqueId)
    }

    private fun WhitelistRecordModel.toWhitelistRecord(): WhitelistRecord {
        return WhitelistRecordImpl(
            uniqueId = uniqueId,
            username = username,
            granter = granter.toOperator(),
            createdAt = createdAt,
            joinedAsVisitorBefore = joinedAsVisitorBefore,
            isMigrated = isMigrated,
            isRevoked = isRevoked,
            revoker = revoker?.toOperator(),
            revokeReason = revokeReason
        )
    }

    private fun VisitorRecordModel.toVisitorRecord(): VisitorRecord {
        return VisitorRecordImpl(
            uniqueId = uniqueId,
            ipAddress = ipAddress.toInetAddress(),
            virtualHost = virtualHost.parseInetSocketAddress(),
            visitedAt = visitedAt,
            createdAt = createdAt,
            duration = Duration.parse("${durationMillis}ms"),
            visitedServers = visitedServers
        )
    }

    private fun WhitelistOperator.toModel(): WhitelistOperatorModel {
        return when (this) {
            is WhitelistOperator.Console -> WhitelistOperatorModel(
                type = WhitelistOperatorModelType.CONSOLE
            )

            is WhitelistOperator.Administrator -> WhitelistOperatorModel(
                type = WhitelistOperatorModelType.ADMINISTRATOR,
                administrator = uniqueId
            )
        }
    }

    private fun WhitelistOperatorModel.toOperator(): WhitelistOperator {
        return when (type) {
            WhitelistOperatorModelType.CONSOLE -> WhitelistOperator.Console()
            WhitelistOperatorModelType.ADMINISTRATOR -> WhitelistOperator.Administrator(
                uniqueId = administrator ?: throw IllegalStateException("Administrator UUID is null")
            )
        }
    }

    private fun InetAddress.toModel(): IpAddressModel {
        val ipString = hostAddress
        val bytes = address
        val version = if (this is Inet6Address) 6 else 4
        val (high, low) = toLongs()
        return IpAddressModel(
            ip = ipString,
            ipBinary = bytes,
            ipVersion = version,
            ipHigh = high,
            ipLow = low
        )
    }

    private fun IpAddressModel.toInetAddress(): InetAddress {
        return InetAddress.getByAddress(ipBinary)
    }
}
