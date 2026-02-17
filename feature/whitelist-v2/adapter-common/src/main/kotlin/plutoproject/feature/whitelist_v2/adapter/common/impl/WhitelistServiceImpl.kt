package plutoproject.feature.whitelist_v2.adapter.common.impl

import plutoproject.feature.whitelist_v2.api.*
import plutoproject.feature.whitelist_v2.api.hook.WhitelistHookParam
import plutoproject.feature.whitelist_v2.api.hook.WhitelistHookType
import plutoproject.feature.whitelist_v2.core.usecase.*
import java.net.InetAddress
import java.util.*

private typealias WhitelistHook = (WhitelistHookParam) -> Unit

class WhitelistServiceImpl(
    private val isWhitelistedUseCase: IsWhitelistedUseCase,
    private val lookupWhitelistRecordUseCase: LookupWhitelistRecordUseCase,
    private val grantWhitelistUseCase: GrantWhitelistUseCase,
    private val revokeWhitelistUseCase: RevokeWhitelistUseCase,
    private val lookupVisitorRecordUseCase: LookupVisitorRecordUseCase,
    private val createVisitorRecordUseCase: CreateVisitorRecordUseCase,
    private val lookupVisitorRecordsByCidrUseCase: LookupVisitorRecordsByCidrUseCase,
    private val lookupVisitorRecordsByIpUseCase: LookupVisitorRecordsByIpUseCase,
    private val knownVisitors: KnownVisitors,
) : WhitelistService {
    private val registeredHooks = mutableMapOf<WhitelistHookType<*>, MutableSet<WhitelistHook>>()

    override suspend fun isWhitelisted(uniqueId: UUID): Boolean {
        return isWhitelistedUseCase.execute(uniqueId)
    }

    override suspend fun lookupWhitelistRecord(uniqueId: UUID): WhitelistRecord? {
        val record = lookupWhitelistRecordUseCase.execute(uniqueId) ?: return null
        return WhitelistRecordImpl(record)
    }

    override suspend fun grantWhitelist(uniqueId: UUID, username: String, operator: WhitelistOperator): Boolean {
        val ok = grantWhitelistUseCase.execute(uniqueId, username, operator.toCore())
        if (ok) {
            invokeHook(WhitelistHookType.GrantWhitelist, WhitelistHookParam.GrantWhitelist(uniqueId, username))
        }
        return ok
    }

    override suspend fun revokeWhitelist(uniqueId: UUID, operator: WhitelistOperator, reason: WhitelistRevokeReason): Boolean {
        val ok = revokeWhitelistUseCase.execute(uniqueId, operator.toCore(), reason.toCore())
        if (ok) {
            invokeHook(WhitelistHookType.RevokeWhitelist, WhitelistHookParam.RevokeWhitelist(uniqueId))
        }
        return ok
    }

    override fun isKnownVisitor(uniqueId: UUID): Boolean {
        return knownVisitors.contains(uniqueId)
    }

    override suspend fun lookupVisitorRecord(uniqueId: UUID): List<VisitorRecord> {
        return lookupVisitorRecordUseCase.execute(uniqueId).map(::VisitorRecordImpl)
    }

    override suspend fun createVisitorRecord(uniqueId: UUID, params: VisitorRecordParams): VisitorRecord {
        return VisitorRecordImpl(createVisitorRecordUseCase.execute(uniqueId, params.toCore()))
    }

    override suspend fun lookupVisitorRecordsByCidr(cidr: String): List<VisitorRecord> {
        return lookupVisitorRecordsByCidrUseCase.execute(cidr).map(::VisitorRecordImpl)
    }

    override suspend fun lookupVisitorRecordsByIp(ipAddress: InetAddress): List<VisitorRecord> {
        return lookupVisitorRecordsByIpUseCase.execute(ipAddress).map(::VisitorRecordImpl)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : WhitelistHookParam> registerHook(type: WhitelistHookType<T>, hook: (T) -> Unit) {
        registeredHooks.computeIfAbsent(type) { mutableSetOf() }.add(hook as ((WhitelistHookParam) -> Unit))
    }

    private fun <T : WhitelistHookParam> invokeHook(type: WhitelistHookType<T>, param: T) {
        registeredHooks[type]?.forEach { it.invoke(param) }
    }
}
