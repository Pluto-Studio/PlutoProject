package plutoproject.feature.whitelist_v2.adapter.common

import plutoproject.feature.whitelist_v2.api.VisitorRecord
import plutoproject.feature.whitelist_v2.api.VisitorRecordParams
import plutoproject.feature.whitelist_v2.api.Whitelist
import plutoproject.feature.whitelist_v2.api.WhitelistOperator
import plutoproject.feature.whitelist_v2.api.WhitelistRecord
import plutoproject.feature.whitelist_v2.api.WhitelistRevokeReason
import plutoproject.feature.whitelist_v2.api.hook.WhitelistHookParam
import plutoproject.feature.whitelist_v2.api.hook.WhitelistHookType
import plutoproject.feature.whitelist_v2.core.usecase.CreateVisitorRecordUseCase
import plutoproject.feature.whitelist_v2.core.usecase.GrantWhitelistUseCase
import plutoproject.feature.whitelist_v2.core.usecase.IsWhitelistedUseCase
import plutoproject.feature.whitelist_v2.core.usecase.LookupVisitorRecordUseCase
import plutoproject.feature.whitelist_v2.core.usecase.LookupVisitorRecordsByCidrUseCase
import plutoproject.feature.whitelist_v2.core.usecase.LookupVisitorRecordsByIpUseCase
import plutoproject.feature.whitelist_v2.core.usecase.LookupWhitelistRecordUseCase
import plutoproject.feature.whitelist_v2.core.usecase.RevokeWhitelistUseCase
import java.net.InetAddress
import java.util.UUID

private typealias WhitelistHook = (WhitelistHookParam) -> Unit

class WhitelistService(
    private val isWhitelistedUseCase: IsWhitelistedUseCase,
    private val lookupWhitelistRecordUseCase: LookupWhitelistRecordUseCase,
    private val grantWhitelistUseCase: GrantWhitelistUseCase,
    private val revokeWhitelistUseCase: RevokeWhitelistUseCase,
    private val lookupVisitorRecordUseCase: LookupVisitorRecordUseCase,
    private val createVisitorRecordUseCase: CreateVisitorRecordUseCase,
    private val lookupVisitorRecordsByCidrUseCase: LookupVisitorRecordsByCidrUseCase,
    private val lookupVisitorRecordsByIpUseCase: LookupVisitorRecordsByIpUseCase,
    private val knownVisitors: KnownVisitors,
) : Whitelist {
    private val registeredHooks = mutableMapOf<WhitelistHookType<*>, MutableSet<WhitelistHook>>()

    override suspend fun isWhitelisted(uniqueId: UUID): Boolean {
        return isWhitelistedUseCase.execute(uniqueId)
    }

    override suspend fun lookupWhitelistRecord(uniqueId: UUID): WhitelistRecord? {
        return lookupWhitelistRecordUseCase.execute(uniqueId)
    }

    override suspend fun grantWhitelist(uniqueId: UUID, username: String, operator: WhitelistOperator): Boolean {
        val ok = grantWhitelistUseCase.execute(uniqueId, username, operator)
        if (ok) {
            invokeHook(WhitelistHookType.GrantWhitelist, WhitelistHookParam.GrantWhitelist(uniqueId, username))
        }
        return ok
    }

    override suspend fun revokeWhitelist(uniqueId: UUID, operator: WhitelistOperator, reason: WhitelistRevokeReason): Boolean {
        val ok = revokeWhitelistUseCase.execute(uniqueId, operator, reason)
        if (ok) {
            invokeHook(WhitelistHookType.RevokeWhitelist, WhitelistHookParam.RevokeWhitelist(uniqueId))
        }
        return ok
    }

    override fun isKnownVisitor(uniqueId: UUID): Boolean {
        return knownVisitors.contains(uniqueId)
    }

    override suspend fun lookupVisitorRecord(uniqueId: UUID): List<VisitorRecord> {
        return lookupVisitorRecordUseCase.execute(uniqueId)
    }

    override suspend fun createVisitorRecord(uniqueId: UUID, params: VisitorRecordParams): VisitorRecord {
        return createVisitorRecordUseCase.execute(uniqueId, params)
    }

    override suspend fun lookupVisitorRecordsByCidr(cidr: String): List<VisitorRecord> {
        return lookupVisitorRecordsByCidrUseCase.execute(cidr)
    }

    override suspend fun lookupVisitorRecordsByIp(ipAddress: InetAddress): List<VisitorRecord> {
        return lookupVisitorRecordsByIpUseCase.execute(ipAddress)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : WhitelistHookParam> registerHook(type: WhitelistHookType<T>, hook: (T) -> Unit) {
        registeredHooks.computeIfAbsent(type) { mutableSetOf() }.add(hook as ((WhitelistHookParam) -> Unit))
    }

    private fun <T : WhitelistHookParam> invokeHook(type: WhitelistHookType<T>, param: T) {
        registeredHooks[type]?.forEach { it.invoke(param) }
    }
}
