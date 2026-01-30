package plutoproject.feature.common.whitelist_v2

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.common.api.whitelist_v2.*
import plutoproject.feature.common.api.whitelist_v2.hook.WhitelistHookParam
import plutoproject.feature.common.api.whitelist_v2.hook.WhitelistHookType
import plutoproject.feature.common.whitelist_v2.model.*
import plutoproject.feature.common.whitelist_v2.repository.VisitorRecordRepository
import plutoproject.feature.common.whitelist_v2.repository.WhitelistRecordRepository
import plutoproject.framework.common.util.network.parseInetSocketAddress
import plutoproject.framework.common.util.network.toHostPortString
import java.net.Inet6Address
import java.net.InetAddress
import java.util.*
import kotlin.time.Duration

private typealias WhitelistHook = (WhitelistHookParam) -> Unit

class WhitelistImpl : Whitelist, KoinComponent {
    private val whitelistRecordRepository: WhitelistRecordRepository by inject()
    private val visitorRecordRepository: VisitorRecordRepository by inject()
    private val registeredHooks = mutableMapOf<WhitelistHookType<*>, MutableSet<WhitelistHook>>()

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
        username: String,
        operator: WhitelistOperator
    ): Boolean {
        if (isWhitelisted(uniqueId)) {
            return false
        }

        val hasVisitorRecord = visitorRecordRepository.hasByUniqueId(uniqueId)
        val model = WhitelistRecordModel(
            uniqueId = uniqueId,
            username = username,
            granter = operator.toModel(),
            joinedAsVisitorBefore = hasVisitorRecord
        )

        whitelistRecordRepository.saveOrUpdate(model)
        invokeHook(WhitelistHookType.GrantWhitelist, WhitelistHookParam.GrantWhitelist(uniqueId, username))
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

        whitelistRecordRepository.saveOrUpdate(updatedModel)
        invokeHook(WhitelistHookType.RevokeWhitelist, WhitelistHookParam.RevokeWhitelist(uniqueId))
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

    override suspend fun lookupVisitorRecordsByCidr(cidr: String): List<VisitorRecord> {
        return visitorRecordRepository.findByCidr(cidr).map { it.toVisitorRecord() }
    }

    override suspend fun lookupVisitorRecordsByIp(ipAddress: InetAddress): List<VisitorRecord> {
        return visitorRecordRepository.findByIpAddress(ipAddress).map { it.toVisitorRecord() }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : WhitelistHookParam> registerHook(type: WhitelistHookType<T>, hook: (T) -> Unit) {
        registeredHooks.computeIfAbsent(type) { mutableSetOf() }.add(hook as ((WhitelistHookParam) -> Unit))
    }

    private fun <T : WhitelistHookParam> invokeHook(type: WhitelistHookType<T>, param: T) {
        registeredHooks[type]?.forEach { it.invoke(param) }
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
