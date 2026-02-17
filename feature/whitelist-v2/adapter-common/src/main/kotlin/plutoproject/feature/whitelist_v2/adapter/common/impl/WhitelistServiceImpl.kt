package plutoproject.feature.whitelist_v2.adapter.common.impl

import plutoproject.feature.whitelist_v2.api.*
import plutoproject.feature.whitelist_v2.api.hook.WhitelistHookParam
import plutoproject.feature.whitelist_v2.api.hook.WhitelistHookType
import plutoproject.feature.whitelist_v2.api.result.WhitelistGrantResult
import plutoproject.feature.whitelist_v2.api.result.WhitelistRevokeResult
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

    override suspend fun lookupWhitelistRecord(uniqueId: UUID): WhitelistRecordInfo? {
        val record = lookupWhitelistRecordUseCase.execute(uniqueId) ?: return null
        return WhitelistRecordInfoImpl(record)
    }

    override suspend fun grantWhitelist(
        uniqueId: UUID,
        username: String,
        operator: WhitelistOperator
    ): WhitelistGrantResult {
        val ok = grantWhitelistUseCase.execute(uniqueId, username, operator.toCore())
        if (ok == GrantWhitelistUseCase.Result.Ok) {
            invokeHook(WhitelistHookType.GrantWhitelist, WhitelistHookParam.GrantWhitelist(uniqueId, username))
        }
        return ok.toApi()
    }

    override suspend fun revokeWhitelist(
        uniqueId: UUID,
        operator: WhitelistOperator,
        reason: WhitelistRevokeReason
    ): WhitelistRevokeResult {
        val ok = revokeWhitelistUseCase.execute(uniqueId, operator.toCore(), reason.toCore())
        if (ok == RevokeWhitelistUseCase.Result.Ok) {
            invokeHook(WhitelistHookType.RevokeWhitelist, WhitelistHookParam.RevokeWhitelist(uniqueId))
        }
        return ok.toApi()
    }

    override fun isKnownVisitor(uniqueId: UUID): Boolean {
        return knownVisitors.contains(uniqueId)
    }

    override suspend fun lookupVisitorRecord(uniqueId: UUID): List<VisitorRecordInfo> {
        return lookupVisitorRecordUseCase.execute(uniqueId).map(::VisitorRecordInfoImpl)
    }

    override suspend fun createVisitorRecord(uniqueId: UUID, params: VisitorRecordParams): VisitorRecordInfo {
        return VisitorRecordInfoImpl(createVisitorRecordUseCase.execute(uniqueId, params.toCore()))
    }

    override suspend fun lookupVisitorRecordsByCidr(cidr: String): List<VisitorRecordInfo> {
        return lookupVisitorRecordsByCidrUseCase.execute(cidr).map(::VisitorRecordInfoImpl)
    }

    override suspend fun lookupVisitorRecordsByIp(ipAddress: InetAddress): List<VisitorRecordInfo> {
        return lookupVisitorRecordsByIpUseCase.execute(ipAddress).map(::VisitorRecordInfoImpl)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : WhitelistHookParam> registerHook(type: WhitelistHookType<T>, hook: (T) -> Unit) {
        registeredHooks.computeIfAbsent(type) { mutableSetOf() }.add(hook as ((WhitelistHookParam) -> Unit))
    }

    private fun <T : WhitelistHookParam> invokeHook(type: WhitelistHookType<T>, param: T) {
        registeredHooks[type]?.forEach { it.invoke(param) }
    }
}
