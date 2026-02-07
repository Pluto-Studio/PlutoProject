package plutoproject.feature.whitelist_v2.adapter

import plutoproject.feature.whitelist_v2.api.VisitorRecord
import plutoproject.feature.whitelist_v2.api.VisitorRecordParams
import plutoproject.feature.whitelist_v2.api.Whitelist
import plutoproject.feature.whitelist_v2.api.WhitelistOperator
import plutoproject.feature.whitelist_v2.api.WhitelistRecord
import plutoproject.feature.whitelist_v2.api.WhitelistRevokeReason
import plutoproject.feature.whitelist_v2.api.hook.WhitelistHookParam
import plutoproject.feature.whitelist_v2.api.hook.WhitelistHookType
import plutoproject.feature.whitelist_v2.core.WhitelistCore
import java.net.InetAddress
import java.util.UUID

private typealias WhitelistHook = (WhitelistHookParam) -> Unit

class WhitelistService(
    private val core: WhitelistCore,
    private val knownVisitors: KnownVisitors,
) : Whitelist {
    private val registeredHooks = mutableMapOf<WhitelistHookType<*>, MutableSet<WhitelistHook>>()

    override suspend fun isWhitelisted(uniqueId: UUID): Boolean {
        return core.isWhitelisted(uniqueId)
    }

    override suspend fun lookupWhitelistRecord(uniqueId: UUID): WhitelistRecord? {
        return core.lookupWhitelistRecord(uniqueId)
    }

    override suspend fun grantWhitelist(uniqueId: UUID, username: String, operator: WhitelistOperator): Boolean {
        val ok = core.grantWhitelist(uniqueId, username, operator)
        if (ok) {
            invokeHook(WhitelistHookType.GrantWhitelist, WhitelistHookParam.GrantWhitelist(uniqueId, username))
        }
        return ok
    }

    override suspend fun revokeWhitelist(uniqueId: UUID, operator: WhitelistOperator, reason: WhitelistRevokeReason): Boolean {
        val ok = core.revokeWhitelist(uniqueId, operator, reason)
        if (ok) {
            invokeHook(WhitelistHookType.RevokeWhitelist, WhitelistHookParam.RevokeWhitelist(uniqueId))
        }
        return ok
    }

    override fun isKnownVisitor(uniqueId: UUID): Boolean {
        return knownVisitors.contains(uniqueId)
    }

    override suspend fun lookupVisitorRecord(uniqueId: UUID): List<VisitorRecord> {
        return core.lookupVisitorRecord(uniqueId)
    }

    override suspend fun createVisitorRecord(uniqueId: UUID, params: VisitorRecordParams): VisitorRecord {
        return core.createVisitorRecord(uniqueId, params)
    }

    override suspend fun lookupVisitorRecordsByCidr(cidr: String): List<VisitorRecord> {
        return core.lookupVisitorRecordsByCidr(cidr)
    }

    override suspend fun lookupVisitorRecordsByIp(ipAddress: InetAddress): List<VisitorRecord> {
        return core.lookupVisitorRecordsByIp(ipAddress)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : WhitelistHookParam> registerHook(type: WhitelistHookType<T>, hook: (T) -> Unit) {
        registeredHooks.computeIfAbsent(type) { mutableSetOf() }.add(hook as ((WhitelistHookParam) -> Unit))
    }

    private fun <T : WhitelistHookParam> invokeHook(type: WhitelistHookType<T>, param: T) {
        registeredHooks[type]?.forEach { it.invoke(param) }
    }
}
